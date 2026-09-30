package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class AllomanticProjectileRecovery {

    public static final double COLLECTION_MARGIN = 1.5;

    private static final Map<Projectile, Pull> PULLS = new HashMap<>();

    private AllomanticProjectileRecovery() {}

    private static final class Pull {

        private ServerPlayer player;
        private double force;
        private boolean active;

        private boolean physicsChanged;
        private boolean previousNoGravity;
        private boolean previousNoPhysics;
        private boolean previousTridentDamage;

        private void preparePhysics(Projectile projectile) {
            previousNoGravity = projectile.isNoGravity();
            physicsChanged = true;

            projectile.setNoGravity(true);

            if (projectile instanceof ThrownTrident trident) {
                previousNoPhysics = trident.isNoPhysics();
                previousTridentDamage = trident.dealtDamage;

                trident.setNoPhysics(false);
                trident.dealtDamage = false;
                trident.inGroundTime = 0;
            }
        }

        private void restorePhysics(Projectile projectile) {
            if (!physicsChanged) {
                return;
            }

            projectile.setNoGravity(previousNoGravity);

            if (projectile instanceof ThrownTrident trident) {
                trident.setNoPhysics(
                        previousNoPhysics && !trident.inGround
                );

                trident.dealtDamage |= previousTridentDamage;
            }

            physicsChanged = false;
        }
    }

    public static boolean canRecover(Entity entity) {
        return entity instanceof Projectile projectile
                && !projectile.isRemoved()
                && EntityMetalSources.isSource(
                projectile,
                EntityMetalSources.Part.PROJECTILE_ITEM
        );
    }

    public static void beginTick(ServerLevel level) {
        for (var entry : PULLS.entrySet()) {
            Projectile projectile = entry.getKey();
            Pull pull = entry.getValue();

            if (projectile.level() == level) {
                pull.restorePhysics(projectile);
                pull.active = false;
            }
        }
    }

    public static void markPulled(
            ServerPlayer player,
            Entity entity,
            double force
    ) {
        if (!canRecover(entity)
                || !Double.isFinite(force)
                || force < 0.0) {
            return;
        }

        Projectile projectile = (Projectile) entity;

        Pull pull = PULLS.computeIfAbsent(
                projectile,
                ignored -> new Pull()
        );

        // El tiraje más fuerte decide el destinatario.
        // En caso de empate, el UUID mantiene un resultado estable.
        if (!pull.active
                || force > pull.force
                || (force == pull.force
                && player.getUUID().compareTo(pull.player.getUUID()) < 0)) {
            pull.player = player;
            pull.force = force;
        }

        pull.active = true;
    }

    public static void finishTick(ServerLevel level) {
        var iterator = PULLS.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();

            Projectile projectile = entry.getKey();
            Pull pull = entry.getValue();

            boolean invalid = projectile.isRemoved()
                    || (projectile.level() == level
                    && (!pull.active
                    || !validRecipient(projectile, pull.player)));

            if (invalid) {
                pull.restorePhysics(projectile);
                AllomanticProjectiles.syncLater(projectile);
                iterator.remove();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeProjectileTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Projectile projectile)
                || projectile.level().isClientSide()
                || projectile.isRemoved()) {
            return;
        }

        Pull pull = PULLS.get(projectile);

        if (pull == null
                || !pull.active
                || !validRecipient(projectile, pull.player)) {
            return;
        }

        if (pull.force > 0.0) {
            guide(projectile, pull);
        }

        if (withinCollectionPath(projectile, pull.player)
                && recover(projectile, pull.player)) {
            event.setCanceled(true);
        }
    }

    private static void guide(
            Projectile projectile,
            Pull pull
    ) {
        if (AllomanticProjectiles.embedded(projectile)) {
            return;
        }

        Vec3 direction = MetalSourceDetector.chestPosition(pull.player)
                .subtract(projectile.position())
                .normalize();

        double speed = Math.max(
                0.0,
                projectile.getDeltaMovement().dot(direction)
        );

        pull.preparePhysics(projectile);

        projectile.setDeltaMovement(direction.scale(speed));
        projectile.hasImpulse = true;

        AllomanticProjectiles.syncLater(projectile);
    }

    @SubscribeEvent
    public static void afterProjectileTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Projectile projectile)
                || projectile.level().isClientSide()) {
            return;
        }

        Pull pull = PULLS.get(projectile);

        if (pull != null) {
            pull.restorePhysics(projectile);
        }
    }

    private static boolean withinCollectionPath(
            Projectile projectile,
            ServerPlayer player
    ) {
        Vec3 start = projectile.position();

        Vec3 end = AllomanticProjectiles.embedded(projectile)
                ? start
                : start.add(projectile.getDeltaMovement());

        var box = player.getBoundingBox().inflate(COLLECTION_MARGIN);

        Vec3 contact = box.contains(start)
                ? start
                : box.clip(start, end).orElse(null);

        return contact != null
                && clearPath(projectile, player, contact);
    }

    private static boolean clearPath(
            Projectile projectile,
            ServerPlayer player,
            Vec3 contact
    ) {
        Vec3 start = projectile.position();

        if (!clearBlocks(projectile, start, contact)
                || !clearBlocks(
                projectile,
                contact,
                player.getBoundingBox().getCenter()
        )) {
            return false;
        }

        var searchBox = projectile.getBoundingBox()
                .expandTowards(contact.subtract(start))
                .inflate(1.0);

        var blocker = ProjectileUtil.getEntityHitResult(
                projectile.level(),
                projectile,
                start,
                contact,
                searchBox,
                entity -> entity != player
                        && entity.isPickable()
                        && !entity.isSpectator()
        );

        return blocker == null;
    }

    private static boolean clearBlocks(
            Projectile projectile,
            Vec3 start,
            Vec3 end
    ) {
        return projectile.level().clip(
                new ClipContext(
                        start,
                        end,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        projectile
                )
        ).getType() == HitResult.Type.MISS;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void impact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide()
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer player
                && recover(event.getProjectile(), player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getDirectEntity() instanceof Projectile projectile
                && recover(projectile, player)) {
            event.setCanceled(true);
        }
    }

    private static boolean validRecipient(
            Projectile projectile,
            ServerPlayer player
    ) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && player.level() == projectile.level();
    }

    private static boolean recover(
            Projectile projectile,
            ServerPlayer player
    ) {
        Pull pull = PULLS.get(projectile);

        if (projectile.isRemoved()
                || pull == null
                || !pull.active
                || pull.player != player
                || !validRecipient(projectile, player)) {
            return false;
        }

        ItemStack stack = EntityMetalSources.item(
                projectile,
                EntityMetalSources.Part.PROJECTILE_ITEM
        ).copyWithCount(1);

        if (stack.isEmpty()) {
            return false;
        }

        pull.restorePhysics(projectile);
        PULLS.remove(projectile);

        player.take(projectile, 1);
        projectile.discard();

        // Si el inventario está lleno, vanilla deja el objeto en el mundo.
        player.getInventory().placeItemBackInInventory(stack);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        return true;
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PULLS.forEach(
                (projectile, pull) -> pull.restorePhysics(projectile)
        );

        PULLS.clear();
    }
}