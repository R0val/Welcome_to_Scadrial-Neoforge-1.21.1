package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.entity.CoinProjectile;
import net.rovalio.scadrialmod.equipment.EquipmentNetworking;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class AllomanticProjectiles {

    private static final Set<AbstractArrow> TRACKED =
            new HashSet<>();

    private static final Set<AbstractArrow> PENDING =
            new HashSet<>();

    private AllomanticProjectiles() {}

    public static boolean embedded(Entity entity) {
        return entity instanceof AbstractArrow arrow
                && arrow.inGround
                && !arrow.isNoPhysics();
    }

    public static Vec3 releasePosition(
            Entity entity,
            Vec3 direction
    ) {
        Vec3 normal = direction.normalize();

        for (int step = 1; step <= 12; step++) {
            Vec3 offset = normal.scale(step * 0.05);

            var hit = entity.level().clip(
                    new ClipContext(
                            entity.position(),
                            entity.position().add(offset),
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            entity
                    )
            );

            if (hit.getType() != HitResult.Type.MISS) {
                return null;
            }

            if (entity.level().noCollision(
                    entity,
                    entity.getBoundingBox().move(offset)
            )) {
                return entity.position().add(offset);
            }
        }

        return null;
    }

    public static boolean release(
            Entity entity,
            Vec3 impulse
    ) {
        if (!embedded(entity)) {
            return true;
        }

        Vec3 position = releasePosition(entity, impulse);

        if (position == null) {
            return false;
        }

        AbstractArrow arrow = (AbstractArrow) entity;

        arrow.inGround = false;
        arrow.inGroundTime = 0;
        arrow.shakeTime = 0;
        arrow.life = 0;

        arrow.setPos(position);
        arrow.setDeltaMovement(Vec3.ZERO);

        if (arrow instanceof ThrownTrident trident) {
            trident.dealtDamage = false;
        }

        if (arrow instanceof CoinProjectile coin) {
            coin.rearm();
        }

        return true;
    }

    public static void syncLater(Entity entity) {
        if (!entity.level().isClientSide()
                && entity instanceof AbstractArrow arrow) {
            TRACKED.add(arrow);
            PENDING.add(arrow);
        }
    }

    @SubscribeEvent
    public static void afterTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        var iterator = TRACKED.iterator();

        while (iterator.hasNext()) {
            AbstractArrow arrow = iterator.next();

            if (arrow.isRemoved()) {
                iterator.remove();
                PENDING.remove(arrow);
                continue;
            }

            if (arrow.level() != event.getLevel()) {
                continue;
            }

            boolean changed = PENDING.remove(arrow);
            boolean embedded = embedded(arrow);

            if (changed || embedded || arrow.tickCount % 2 == 0) {
                PacketDistributor.sendToPlayersTrackingEntity(
                        arrow,
                        new EquipmentNetworking.Motion(
                                arrow.getId(),
                                arrow.getUUID(),
                                arrow.level().dimension().location(),
                                arrow.position(),
                                arrow.getDeltaMovement(),
                                embedded
                        )
                );
            }

            if (embedded) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        TRACKED.clear();
        PENDING.clear();
    }
}