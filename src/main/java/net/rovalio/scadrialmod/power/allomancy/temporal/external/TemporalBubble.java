package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record TemporalBubble(
        UUID owner,
        Vec3 center,
        double radius,
        double outerRadius,
        boolean bendalloy,
        boolean boosted
) {

    public TemporalBubble {
        if (owner == null
                || center == null
                || !Double.isFinite(center.x)
                || !Double.isFinite(center.y)
                || !Double.isFinite(center.z)
                || !Double.isFinite(radius)
                || !Double.isFinite(outerRadius)
                || radius < 2
                || radius > 16
                || outerRadius <= radius
                || outerRadius > 24) {
            throw new IllegalArgumentException(
                    "Invalid temporal bubble geometry"
            );
        }
    }

    public AABB bounds() {
        return new AABB(center, center).inflate(outerRadius);
    }

    public boolean containsOwner(AABB box){
        return TemporalAllomancyMath.intersectsBox(
                center.x,
                center.y,
                center.z,
                radius,
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
        );
    }

    public int exponent(Vec3 point){
        return TemporalAllomancyMath.exponent(
                center.distanceToSqr(point),
                radius,
                outerRadius,
                bendalloy,
                boosted
        );
    }


}
