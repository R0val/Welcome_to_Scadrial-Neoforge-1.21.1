package net.rovalio.scadrialmod.power.allomancy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.ExternalAllomancyNetworking;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class ExternalAllomancyPhysics {

    public static final int RAMP_TICKS = 5;
    public static final int INPUT_TIMEOUT_TICKS = 20;

    public static final float FALL_DAMAGE_REDUCTION = 0.50F;

    private static final double CONTACT_PROBE = 0.04;
    private static final double MIN_TARGET_DISTANCE = 0.15;
    private static final double RESISTANCE_SCALE = 1.0;
    private static final double EPSILON = 1.0E-12;

    private static final int DURALUMIN_RAMP_TICKS = 8;

    private static final Map<UUID, Action> ACTIONS =
            new HashMap<>();

    private static final Map<UUID, Motion> MOTION =
            new HashMap<>();

    private static final Map<UUID, ResourceLocation> FALL_PROTECTED =
            new HashMap<>();

    private ExternalAllomancyPhysics() {
    }

    private static final class Action {

        private ResourceLocation dimension;
        private long lastInput;

        private boolean push;
        private boolean pull;

        private int pushTicks;
        private int pullTicks;

        private MetalTarget target = MetalTarget.NONE;
        private ExternalAllomancyNetworking.Selection lastSent;

        private void advance(
                boolean pushing,
                boolean pulling
        ) {
            pushTicks = nextHoldTicks(
                    pushTicks,
                    pushing && !target.isNone()
            );

            pullTicks = nextHoldTicks(
                    pullTicks,
                    pulling && !target.isNone()
            );
        }
    }

    private record Motion(
            ResourceLocation dimension,
            long tick,
            Vec3 position,
            Vec3 velocity
    ) {}

    private record PowerUse(
            ScadrialPlayerData data,
            double strength,
            double radius,
            boolean push,
            boolean pull,
            boolean duralumin
    ) {}

    private static double physicalStrength(PowerUse power) {
        return ExternalAllomancyMath.externalStrength(
                power.strength(), power.duralumin()
        );
    }

    private record Interaction(
            Entity caster,
            Entity source,
            Vec3 casterDirection,
            Vec3 sourceDirection,
            double distance
    ) {}

    public static void handleInput(
            ExternalAllomancyNetworking.Input input,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (!input.dimension().equals(dimensionOf(player))) {
            return;
        }

        if (!player.isAlive() || player.isSpectator()) {
            clear(player);
            return;
        }

        Action action = ACTIONS.computeIfAbsent(
                player.getUUID(),
                ignored -> new Action()
        );

        action.dimension = input.dimension();
        action.lastInput = player.serverLevel().getGameTime();
        action.push = input.push();
        action.pull = input.pull();

        if (!action.push) {
            action.pushTicks = 0;
        }

        if (!action.pull) {
            action.pullTicks = 0;
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ImpulseBatch batch = new ImpulseBatch();

        for (ServerPlayer player : level.players()) {
            updateMotion(player);
        }

        for (ServerPlayer player : level.players()) {
            collect(player, batch);
        }

        batch.apply();
    }

    private static void collect(
            ServerPlayer player,
            ImpulseBatch batch
    ) {
        Action action = ACTIONS.get(player.getUUID());

        if (action == null) {
            return;
        }

        if (!validInput(player, action)) {
            stop(player, action);
            return;
        }

        PowerUse power = readPower(player, action);

        if (power == null) {
            stop(player, action);
            return;
        }

        action.target = selectTarget(
                player,
                action.target,
                power.radius()
        );

        action.advance(power.push(), power.pull());

        sendSelection(
                player,
                action,
                power.push(),
                power.pull()
        );

        if (!action.target.isNone()) {
            accumulateInteraction(
                    player,
                    action,
                    power,
                    batch
            );
        }
    }

    private static boolean validInput(
            ServerPlayer player,
            Action action
    ) {
        long age = player.serverLevel().getGameTime()
                - action.lastInput;

        return player.isAlive()
                && !player.isSpectator()
                && dimensionOf(player).equals(action.dimension)
                && age >= 0
                && age <= INPUT_TIMEOUT_TICKS
                && (action.push || action.pull);
    }

    private static PowerUse readPower(
            ServerPlayer player,
            Action action
    ) {
        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        double strength =
                AllomancyBurnManager.effectiveStrength(player);

        boolean aluminium = usable(
                player, data, AllomanticFuel.ALUMINIUM
        );
        boolean duralumin = !aluminium && usable(
                player, data, AllomanticFuel.DURALUMIN
        );

        double radius = ExternalAllomancyMath.radius(
                strength, duralumin
        );

        boolean push = action.push
                && usable(player, data, AllomanticFuel.STEEL);

        boolean pull = action.pull
                && usable(player, data, AllomanticFuel.IRON);

        if ((!push && !pull)
                || radius <= 0.0
                || aluminium) {
            return null;
        }

        return new PowerUse(
                data,
                strength,
                radius,
                push,
                pull,
                duralumin
        );
    }

    private static MetalTarget selectTarget(
            ServerPlayer player,
            MetalTarget previous,
            double radius
    ) {
        SyncMetalSourcesS2CPayload cached =
                ExternalAllomancyPerception.current(player);

        if (cached == null
                || !cached.dimension().equals(dimensionOf(player))) {
            return MetalTarget.NONE;
        }

        SyncMetalSourcesS2CPayload sources =
                new SyncMetalSourcesS2CPayload(
                        cached.dimension(),
                        radius,
                        cached.targets(),
                        cached.blocks()
                );

        MetalTarget selected = MetalTargetSelector.select(
                player,
                sources,
                previous
        );

        return selected.validFor(player, radius)
                ? selected
                : MetalTarget.NONE;
    }

    private static int nextHoldTicks(
            int previous,
            boolean active
    ) {
        return active
                ? Math.min(
                Math.max(RAMP_TICKS, DURALUMIN_RAMP_TICKS),
                previous + 1
        )
                : 0;
    }

    private static void accumulateInteraction(
            ServerPlayer player,
            Action action,
            PowerUse power,
            ImpulseBatch batch
    ) {
        double signedAmount = burnAmount(
                power,
                AllomanticFuel.STEEL,
                action.pushTicks
        ) - burnAmount(
                power,
                AllomanticFuel.IRON,
                action.pullTicks
        );

        if (Math.abs(signedAmount) < EPSILON) {
            return;
        }

        Interaction interaction = resolveInteraction(
                player,
                action.target,
                signedAmount
        );

        if (interaction == null) {
            return;
        }

        double force = interactionForce(
                player,
                action.target,
                power,
                interaction
        ) * Math.abs(signedAmount);

        double strength = physicalStrength(power);

        batch.add(
                interaction.caster(),
                interaction.casterDirection().scale(force),
                strength
        );

        if (interaction.source() != null
                && interaction.sourceDirection().lengthSqr() > EPSILON) {
            batch.add(
                    interaction.source(),
                    interaction.sourceDirection().scale(force),
                    strength
            );

            batch.link(
                    interaction.caster(),
                    interaction.source()
            );
        }
    }

    private static Interaction resolveInteraction(
            ServerPlayer player,
            MetalTarget target,
            double signedAmount
    ) {
        Vec3 point = target.position(player.level(), 1.0F);

        if (point == null) {
            return null;
        }

        Vec3 offset = point.subtract(
                MetalSourceDetector.chestPosition(player)
        );

        double distance = offset.length();

        if (distance < MIN_TARGET_DISTANCE) {
            return null;
        }

        Entity caster = player.getRootVehicle();
        Entity entity = target.entity(player.level());

        Entity source = entity == null
                ? null
                : entity.getRootVehicle();

        if (source == caster) {
            return null;
        }

        Vec3 direction = offset.scale(
                Math.copySign(1.0 / distance, signedAmount)
        );

        Vec3 casterDirection = freeDirection(
                caster,
                direction.scale(-1.0)
        );

        Vec3 sourceDirection = Vec3.ZERO;

        if (source != null && !MetalSourceProperties.fixed(source)) {
            sourceDirection = freeDirection(source, direction);
        }

        return new Interaction(
                caster,
                source,
                casterDirection,
                sourceDirection,
                distance
        );
    }

    private static double interactionForce(
            ServerPlayer player,
            MetalTarget target,
            PowerUse power,
            Interaction interaction
    ) {
        double strength = physicalStrength(power);
        double force = ExternalAllomancyMath.force(strength);

        double acceleration =
                ExternalAllomancyMath.accelerationLimit(
                        strength
                );

        force = limitForce(
                force,
                interaction.caster(),
                interaction.casterDirection(),
                acceleration
        );

        if (interaction.source() != null) {
            force = limitForce(
                    force,
                    interaction.source(),
                    interaction.sourceDirection(),
                    acceleration
            );
        }

        double distanceFactor =
                ExternalAllomancyMath.rangeFactor(
                        interaction.distance(),
                        power.radius()
                );

        double resistance =
                ExternalAllomancyMath.resistanceFactor(
                        strength,
                        MetalSourceProperties.charge(
                                player.serverLevel(),
                                target
                        ),
                        RESISTANCE_SCALE
                );

        // La atenuación se aplica después del límite mecánico.
        return force * distanceFactor * resistance;
    }

    private static double burnAmount(
            PowerUse power,
            AllomanticFuel fuel,
            int heldTicks
    ) {
        long available = power.data()
                .getAllomanticReserveSubunits(fuel);

        long cost = AllomancyBurnManager.burnSubunitsPerTick(
                fuel,
                power.duralumin()
        );

        int rampTicks = power.duralumin()
                ? DURALUMIN_RAMP_TICKS
                : RAMP_TICKS;

        return ExternalAllomancyMath.ramp(
                heldTicks,
                rampTicks
        ) * ExternalAllomancyMath.paidFraction(
                available,
                cost
        );
    }

    private static boolean usable(
            ServerPlayer player,
            ScadrialPlayerData data,
            AllomanticFuel fuel
    ) {
        return data.isBurning(fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0L
                && ScadrialPowerManager.canUseAllomanticFuel(
                player, fuel
        );
    }

    private static Vec3 freeDirection(
            Entity entity,
            Vec3 direction
    ) {
        Vec3 probe = direction.scale(CONTACT_PROBE);
        var box = entity.getBoundingBox();

        var collisions = entity.level().getEntityCollisions(
                entity,
                box.expandTowards(probe)
        );

        return Entity.collideBoundingBox(
                entity,
                probe,
                box,
                entity.level(),
                collisions
        ).scale(1.0 / CONTACT_PROBE);
    }

    private static double limitForce(
            double force,
            Entity entity,
            Vec3 direction,
            double acceleration
    ) {
        return ExternalAllomancyMath.limitForce(
                force,
                MetalSourceProperties.mass(entity),
                direction.length(),
                acceleration
        );
    }

    private static final class ImpulseBatch {

        private final Map<Entity, Vec3> impulses =
                new HashMap<>();

        private final Map<Entity, Entity> parents =
                new HashMap<>();

        private final Map<Entity, Double> strengths =
                new HashMap<>();

        private void add(
                Entity entity,
                Vec3 impulse,
                double strength
        ) {
            if (impulse.lengthSqr() <= EPSILON) {
                return;
            }

            impulses.merge(
                    entity,
                    impulse,
                    Vec3::add
            );

            strengths.merge(
                    entity,
                    strength,
                    Math::max
            );
        }

        private Entity root(Entity entity) {
            Entity parent = parents.get(entity);

            if (parent == null || parent == entity) {
                return entity;
            }

            Entity root = root(parent);
            parents.put(entity, root);

            return root;
        }

        private void link(
                Entity first,
                Entity second
        ) {
            Entity firstRoot = root(first);
            Entity secondRoot = root(second);

            if (firstRoot != secondRoot) {
                parents.put(firstRoot, secondRoot);
            }
        }

        private double scale(
                Entity entity,
                Vec3 delta
        ) {
            double strength = strengths.get(entity);

            double acceleration =
                    ExternalAllomancyMath.accelerationLimit(strength);

            double length = delta.length();

            double accelerationScale = length > acceleration
                    ? acceleration / length
                    : 1.0;

            Vec3 velocity = velocity(entity);

            double speedScale = ExternalAllomancyMath.speedScale(
                    velocity.lengthSqr(),
                    velocity.dot(delta),
                    delta.lengthSqr(),
                    ExternalAllomancyMath.poweredSpeedLimit(strength)
            );

            return Math.min(
                    accelerationScale,
                    speedScale
            );
        }

        private void apply() {
            Map<Entity, Vec3> deltas = new HashMap<>();
            Map<Entity, Double> groupScales = new HashMap<>();

            for (var entry : impulses.entrySet()) {
                Entity entity = entry.getKey();

                Vec3 delta = entry.getValue().scale(
                        1.0 / MetalSourceProperties.mass(entity)
                );

                deltas.put(entity, delta);

                groupScales.merge(
                        root(entity),
                        scale(entity, delta),
                        Math::min
                );
            }

            for (var entry : deltas.entrySet()) {
                Entity entity = entry.getKey();

                if (entity.isAlive() && !entity.isRemoved()) {
                    double scale = groupScales.get(root(entity));

                    applyImpulse(
                            entity,
                            entry.getValue().scale(scale)
                    );
                }
            }
        }
    }

    private static void applyImpulse(
            Entity entity,
            Vec3 delta
    ) {
        if (delta.lengthSqr() < EPSILON) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            applyPlayerImpulse(player, delta);
            return;
        }

        entity.setDeltaMovement(
                entity.getDeltaMovement().add(delta)
        );

        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private static void applyPlayerImpulse(
            ServerPlayer player,
            Vec3 delta
    ) {
        Vec3 before = velocity(player);

        applyFallBraking(player, before, delta);

        boolean vanillaVelocityPending = player.hurtMarked;

        Vec3 serverVelocity = vanillaVelocityPending
                ? player.getDeltaMovement()
                : before;

        player.setDeltaMovement(serverVelocity.add(delta));

        if (!vanillaVelocityPending) {
            PacketDistributor.sendToPlayer(
                    player,
                    new ExternalAllomancyNetworking.Impulse(
                            dimensionOf(player),
                            player.getUUID(),
                            delta.x,
                            delta.y,
                            delta.z
                    )
            );
        }
    }

    private static void applyFallBraking(
            ServerPlayer player,
            Vec3 before,
            Vec3 delta
    ) {
        if (delta.y <= 0.0
                || before.y >= 0.0
                || player.onGround()) {
            return;
        }

        player.fallDistance *=
                (float) ExternalAllomancyMath.brakingRatio(
                        -before.y,
                        delta.y
                );

        FALL_PROTECTED.put(
                player.getUUID(),
                dimensionOf(player)
        );
    }

    private static void updateMotion(ServerPlayer player) {
        UUID id = player.getUUID();
        long tick = player.serverLevel().getGameTime();

        Motion previous = MOTION.get(id);

        Vec3 measured = measureVelocity(
                player,
                previous,
                tick
        );

        MOTION.put(
                id,
                new Motion(
                        dimensionOf(player),
                        tick,
                        player.position(),
                        measured
                )
        );

        if (player.onGround()
                || player.isInWater()
                || !player.isAlive()) {
            FALL_PROTECTED.remove(id);
        }
    }

    private static Vec3 measureVelocity(
            ServerPlayer player,
            Motion previous,
            long tick
    ) {
        if (previous == null
                || !previous.dimension().equals(dimensionOf(player))
                || tick <= previous.tick()) {
            return player.getDeltaMovement();
        }

        Vec3 displacement = player.position()
                .subtract(previous.position());

        if (displacement.lengthSqr() >= 256.0) {
            return player.getDeltaMovement();
        }

        return displacement.scale(
                1.0 / (tick - previous.tick())
        );
    }

    private static Vec3 velocity(Entity entity) {
        Motion motion = MOTION.get(entity.getUUID());

        if (entity instanceof ServerPlayer && motion != null) {
            return motion.velocity();
        }

        return entity.getDeltaMovement();
    }

    private static ResourceLocation dimensionOf(
            ServerPlayer player
    ) {
        return player.level().dimension().location();
    }

    public static MetalTarget selected(ServerPlayer player) {
        Action action = ACTIONS.get(player.getUUID());

        return action == null
                ? MetalTarget.NONE
                : action.target;
    }

    private static void sendSelection(
            ServerPlayer player,
            Action action,
            boolean push,
            boolean pull
    ) {
        var update = new ExternalAllomancyNetworking.Selection(
                dimensionOf(player),
                action.target,
                push,
                pull
        );

        if (!update.equals(action.lastSent)) {
            action.lastSent = update;

            PacketDistributor.sendToPlayer(
                    player,
                    update
            );
        }
    }

    private static void stop(
            ServerPlayer player,
            Action action
    ) {
        action.pushTicks = 0;
        action.pullTicks = 0;
        action.target = MetalTarget.NONE;

        sendSelection(
                player,
                action,
                false,
                false
        );

        ACTIONS.remove(player.getUUID());
    }

    private static void clear(ServerPlayer player) {
        Action action = ACTIONS.get(player.getUUID());

        if (action != null) {
            stop(player, action);
        }

        MOTION.remove(player.getUUID());
        FALL_PROTECTED.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation dimension =
                FALL_PROTECTED.remove(player.getUUID());

        if (dimensionOf(player).equals(dimension)) {
            event.setDamageMultiplier(
                    event.getDamageMultiplier()
                            * (1.0F - FALL_DAMAGE_REDUCTION)
            );
        }
    }

    @SubscribeEvent
    public static void logout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void respawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void dimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ACTIONS.clear();
        MOTION.clear();
        FALL_PROTECTED.clear();
    }
}