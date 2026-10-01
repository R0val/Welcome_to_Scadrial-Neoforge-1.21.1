package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.power.allomancy.physical.external.AllomanticProjectiles;

public final class TemporalProjectiles {

    private TemporalProjectiles() {
    }

    public static void beforeTick(Entity entity) {
        if (entity.level().isClientSide()
                || !(entity instanceof Projectile)
                || AllomanticProjectiles.embedded(entity)) {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();

        if (motion.lengthSqr() < 1.0E-10) {
            return;
        }

        Vec3 start = entity.position();
        double crossing = Double.POSITIVE_INFINITY;

        for (TemporalBubble bubble
                : TemporalField.of(entity.level()).bubbles()) {
            Vec3 relative = start.subtract(bubble.center());

            crossing = Math.min(
                    crossing,
                    TemporalAllomancyMath.firstCrossing(
                            relative.x,
                            relative.y,
                            relative.z,
                            motion.x,
                            motion.y,
                            motion.z,
                            bubble.radius()
                    )
            );
        }

        if (!Double.isNaN(crossing)) {
            return;
        }

        var hit = entity.level().clip(
                new ClipContext(
                        start,
                        start.add(motion),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        entity
                )
        );

        double boundaryDistanceSquared =
                motion.lengthSqr() * crossing * crossing;

        if (hit.getType() != HitResult.Type.MISS
                && start.distanceToSqr(hit.getLocation())
                < boundaryDistanceSquared) {
            return;
        }

        Vec3 forward = motion.normalize();

        Vec3 reference = Math.abs(forward.y) < 0.9
                ? new Vec3(0, 1, 0)
                : new Vec3(1, 0, 0);

        Vec3 side = forward.cross(reference).normalize();
        Vec3 up = side.cross(forward).normalize();

        var random = entity.level().random;

        double angle = random.nextDouble()
                * Math.toRadians(
                TemporalAllomancyMath.MAX_DEFLECTION_DEGREES
        );

        double rotation = random.nextDouble() * Math.PI * 2.0;

        Vec3 direction = forward.scale(Math.cos(angle))
                .add(side.scale(Math.sin(angle) + Math.cos(rotation)))
                .add(side.scale(Math.sin(angle) + Math.sin(rotation)));

        entity.setDeltaMovement(direction.scale(motion.length()));
        entity.hasImpulse = true;
        entity.hurtMarked = true;

        AllomanticProjectiles.syncLater(entity);
    }
}

