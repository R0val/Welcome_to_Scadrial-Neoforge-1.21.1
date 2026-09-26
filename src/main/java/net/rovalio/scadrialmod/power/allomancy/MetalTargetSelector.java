package net.rovalio.scadrialmod.power.allomancy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;

import java.util.ArrayList;
import java.util.List;

public final class MetalTargetSelector {

    public static final double ACQUIRE_COS =
            Math.cos(Math.toRadians(6.0));

    private static final double RETAIN_ANGLE =
            Math.toRadians(8.0);

    private static final double SWITCH_MARGIN =
            Math.toRadians(1.0);

    private record Candidate(
            MetalTarget target,
            double distanceSquared,
            double cosine
    ) {
    }

    private MetalTargetSelector() {
    }

    public static double alignment(
            Vec3 eyes,
            Vec3 look,
            Vec3 position
    ) {
        Vec3 offset = position.subtract(eyes);

        if (offset.lengthSqr() < 1.0E-10) {
            return -1.0;
        }

        return look.dot(offset.normalize());
    }

    public static MetalTarget select(
            Player player,
            SyncMetalSourcesS2CPayload sources,
            MetalTarget previous
    ) {
        if (sources == null
                || !Double.isFinite(sources.radius())
                || sources.radius() <= 0.0
                || !sources.dimension().equals(
                player.level().dimension().location()
        )) {
            return MetalTarget.NONE;
        }

        Vec3 chest = player.position().add(
                0.0,
                Math.min(1.25, player.getBbHeight() * 0.75),
                0.0
        );

        Candidate aimed = null;
        Candidate nearest = null;
        Candidate retained = null;

        for (MetalTarget target : candidates(sources, previous)) {
            Candidate candidate = inspect(
                    player, target, chest, sources.radius()
            );

            if (candidate == null) {
                continue;
            }

            if (target.equals(previous)) {
                retained = candidate;
            }

            if (nearest == null
                    || candidate.distanceSquared() < nearest.distanceSquared()) {
                nearest = candidate;
            }

            if (betterAim(candidate, aimed)) {
                aimed = candidate;
            }
        }

        if (retainPrevious(retained, aimed)) {
            return retained.target();
        }

        if (aimed != null) {
            return aimed.target();
        }

        return nearest == null ? MetalTarget.NONE : nearest.target();
    }

    private static List<MetalTarget> candidates(
            SyncMetalSourcesS2CPayload sources,
            MetalTarget previous
    ) {
        List<MetalTarget> result = new ArrayList<>();

        sources.targets().forEach(
                target -> result.add(MetalTarget.of(target))
        );
        sources.blocks().forEach(
                block -> result.add(MetalTarget.of(block))
        );

        if (previous != null
                && !previous.isNone()
                && !result.contains(previous)) {
            result.add(previous);
        }

        return result;
    }

    private static Candidate inspect(
            Player player,
            MetalTarget target,
            Vec3 chest,
            double radius
    ) {
        if (target.entityTarget() != null) {
            Entity entity = target.entity(player.level());

            if (entity == null
                    || entity.getRootVehicle() == player.getRootVehicle()) {
                return null;
            }
        }

        Vec3 point = target.position(player.level(), 1.0F);

        if (point == null) {
            return null;
        }

        double distance = point.distanceToSqr(chest);

        if (distance < 1.0E-8 || distance > radius * radius) {
            return null;
        }

        return new Candidate(
                target,
                distance,
                alignment(
                        player.getEyePosition(),
                        player.getLookAngle(),
                        point
                )
        );
    }

    private static boolean betterAim(
            Candidate candidate,
            Candidate current
    ) {
        if (candidate.cosine() < ACQUIRE_COS) {
            return false;
        }

        if (current == null || candidate.cosine() > current.cosine()) {
            return true;
        }

        return Double.compare(candidate.cosine(), current.cosine()) == 0
                && candidate.distanceSquared() < current.distanceSquared();
    }

    private static boolean retainPrevious(
            Candidate previous,
            Candidate aimed
    ) {
        if (previous == null) {
            return false;
        }

        double previousAngle = angle(previous.cosine());

        return previousAngle <= RETAIN_ANGLE
                && (aimed == null
                || previousAngle - angle(aimed.cosine()) < SWITCH_MARGIN);
    }

    private static double angle(double cosine) {
        return Math.acos(Math.max(-1.0, Math.min(1.0, cosine)));
    }
}