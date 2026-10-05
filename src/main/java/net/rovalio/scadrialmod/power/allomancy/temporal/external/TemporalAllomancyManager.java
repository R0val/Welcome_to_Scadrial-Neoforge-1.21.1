package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.ScadrialNetworking;
import net.rovalio.scadrialmod.network.TemporalAllomancyNetworking;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class TemporalAllomancyManager {

    public static final int MAX_BUBBLES = 32;

    private static final int INPUT_TIMEOUT = 20;

    private static final Map<ServerLevel, WorldState> WORLDS =
            new HashMap<>();

    private static final class WorldState {

        final Map<UUID, Charge> charges = new HashMap<>();
        final Map<UUID, Active> active = new HashMap<>();
        final Map<UUID, double[]> remainders = new HashMap<>();
    }

    private static final class Charge {

        final boolean bendalloy;
        final long started;

        long lastInput;

        Charge(boolean bendalloy, long now) {
            this.bendalloy = bendalloy;
            this.started = now;
            this.lastInput = now;
        }
    }

    private static final class Active {

        final Vec3 center;
        final double charge;
        final boolean bendalloy;

        TemporalBubble bubble;

        Active(
                Vec3 center,
                double charge,
                boolean bendalloy
        ) {
            this.center = center;
            this.charge = charge;
            this.bendalloy = bendalloy;
        }
    }

    private TemporalAllomancyManager() {}

    public static AllomanticFuel fuel(boolean bendalloy) {
        return bendalloy
                ? AllomanticFuel.BENDALLOY
                : AllomanticFuel.CADMIUM;
    }

    private static boolean usable(
            ServerPlayer player,
            AllomanticFuel fuel
    ) {
        var data = ScadrialAttachments.get(player);

        return player.isAlive()
                && !player.isSpectator()
                && !data.isBurning(AllomanticFuel.ALUMINIUM)
                && data.isBurning(fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0
                && ScadrialPowerManager.canUseAllomanticFuel(
                player,
                fuel
        );
    }

    public static boolean charging(ServerPlayer player) {
        WorldState state = WORLDS.get(player.serverLevel());

        return state != null
                && state.charges.containsKey(player.getUUID());
    }

    public static void input(
            ServerPlayer player,
            TemporalAllomancyNetworking.Input input
    ) {
        if (input.action() < TemporalAllomancyNetworking.START
                || input.action() > TemporalAllomancyNetworking.CANCEL) {
            return;
        }

        ServerLevel level = player.serverLevel();

        if (!input.dimension().equals(level.dimension().location())) {
            return;
        }

        WorldState state = WORLDS.computeIfAbsent(
                level,
                ignored -> new WorldState()
        );

        UUID owner = player.getUUID();
        long now = level.getGameTime();

        Charge charge = state.charges.get(owner);

        if (input.action() == TemporalAllomancyNetworking.CANCEL) {
            state.charges.remove(owner);
            return;
        }

        if (!usable(player, fuel(input.bendalloy()))) {
            state.charges.remove(owner);
            return;
        }

        if (input.action() == TemporalAllomancyNetworking.START) {
            if (player.isShiftKeyDown() && charge == null) {
                state.charges.put(
                        owner,
                        new Charge(input.bendalloy(), now)
                );
            }

            return;
        }

        if (charge == null || charge.bendalloy != input.bendalloy()) {
            return;
        }

        if (now - charge.lastInput > INPUT_TIMEOUT) {
            state.charges.remove(owner);
            return;
        }

        if (input.action() == TemporalAllomancyNetworking.KEEP) {
            if (player.isShiftKeyDown()) {
                charge.lastInput = now;
            } else {
                state.charges.remove(owner);
            }

            return;
        }

        if (input.action() != TemporalAllomancyNetworking.RELEASE) {
            return;
        }

        state.charges.remove(owner);

        if (!player.isShiftKeyDown() || now <= charge.started) {
            return;
        }

        if (state.active.size() >= MAX_BUBBLES
                && !state.active.containsKey(owner)) {
            return;
        }

        Active active = new Active(
                player.position().add(0, 1.25, 0),
                TemporalAllomancyMath.charge(now - charge.started),
                charge.bendalloy
        );

        active.bubble = geometry(player, active);

        state.active.put(owner, active);

        publish(level, state);
    }

    private static TemporalBubble geometry(
            ServerPlayer player,
            Active active
    ) {
        double strength =
                ScadrialAttachments.get(player).getAllomancyStrength();

        boolean boosted = usable(
                player,
                AllomanticFuel.DURALUMIN
        );

        return new TemporalBubble(
                player.getUUID(),
                active.center,
                TemporalAllomancyMath.radius(
                        strength,
                        boosted,
                        active.charge
                ),
                TemporalAllomancyMath.outerRadius(
                        strength,
                        boosted
                ),
                active.bendalloy,
                boosted
        );
    }

    @SubscribeEvent
    public static void beforeLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.tickRateManager().runsNormally()) {
            return;
        }

        WorldState state = WORLDS.get(level);

        if (state != null) {
            long now = level.getGameTime();

            state.charges.entrySet().removeIf(entry -> {
                ServerPlayer player = level.getServer()
                        .getPlayerList()
                        .getPlayer(entry.getKey());

                return player == null
                        || player.level() != level
                        || now - entry.getValue().lastInput > INPUT_TIMEOUT
                        || !usable(
                        player,
                        fuel(entry.getValue().bendalloy)
                );
            });

            updateBubbles(level, state);
            publish(level, state);
        }

        for (ServerPlayer player : level.players()) {
            double rate = player.isAlive() && !player.isSpectator()
                    ? TemporalField.of(level).rate(player)
                    : 1.0;

            TemporalPlayerEffects.update(player, rate);
        }
    }

    private static void updateBubbles(
            ServerLevel level,
            WorldState state
    ) {
        var iterator = state.active.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();

            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayer(entry.getKey());

            Active active = entry.getValue();

            if (player == null
                    || player.level() != level
                    || !usable(player, fuel(active.bendalloy))) {
                iterator.remove();
                continue;
            }

            active.bubble = geometry(player, active);

            if (!active.bubble.containsOwner(player.getBoundingBox())) {
                player.addEffect(
                        new MobEffectInstance(
                                active.bendalloy
                                        ? MobEffects.MOVEMENT_SPEED
                                        : MobEffects.MOVEMENT_SLOWDOWN,
                                TemporalAllomancyMath.EXIT_EFFECT_TICKS,
                                0
                        )
                );

                iterator.remove();
                continue;
            }

            if (!consume(player, state, active)) {
                iterator.remove();
            }
        }
    }

    private static boolean consume(
            ServerPlayer player,
            WorldState state,
            Active active
    ) {
        double[] remainders = state.remainders.computeIfAbsent(
                player.getUUID(),
                ignored -> new double[2]
        );

        int index = active.bendalloy ? 1 : 0;

        double amount = remainders[index]
                + TemporalAllomancyMath.subunitsPerTick(
                active.bubble.radius()
        );

        long whole = (long) Math.floor(amount);

        remainders[index] = amount - whole;

        AllomanticFuel fuel = fuel(active.bendalloy);

        var data = ScadrialAttachments.get(player);

        data.consumeAllomanticReserveSubunits(fuel, whole);

        if (data.getAllomanticReserveSubunits(fuel) > 0) {
            return true;
        }

        data.setBurning(fuel, false);
        ScadrialNetworking.syncAllomancy(player);

        return false;
    }

    private static void publish(
            ServerLevel level,
            WorldState state
    ) {
        TemporalField field = TemporalField.of(level);

        if (state.active.isEmpty()
                && field.bubbles().isEmpty()) {
            return;
        }

        List<TemporalBubble> bubbles =
                state.active.values()
                        .stream()
                        .map(active -> active.bubble)
                        .toList();

        if (field.bubbles().equals(bubbles)) {
            return;
        }

        TemporalFieldUpdates.replace(level, bubbles);

        PacketDistributor.sendToPlayersInDimension(
                level,
                new TemporalAllomancyNetworking.State(
                        level.dimension().location(),
                        bubbles
                )
        );
    }

    private static void syncTo(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        PacketDistributor.sendToPlayer(
                player,
                new TemporalAllomancyNetworking.State(
                        level.dimension().location(),
                        TemporalField.of(level).bubbles()
                )
        );
    }

    private static void forget(ServerPlayer player) {
        for (var entry : WORLDS.entrySet()) {
            WorldState state = entry.getValue();

            state.charges.remove(player.getUUID());
            state.remainders.remove(player.getUUID());

            if (state.active.remove(player.getUUID()) != null) {
                publish(entry.getKey(), state);
            }
        }

        TemporalPlayerEffects.update(player, 1.0);
    }

    @SubscribeEvent
    public static void login(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void logout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
        }
    }

    @SubscribeEvent
    public static void dimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void respawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forget(player);
            syncTo(player);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WORLDS.remove(level);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        WORLDS.clear();
    }
}