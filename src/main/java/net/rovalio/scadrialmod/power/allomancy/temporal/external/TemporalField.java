package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TemporalField {

    public interface Access {
        TemporalField scadrial$temporalField();
    }

    private List<TemporalBubble> bubbles = List.of();

    private final Map<Long, List<TemporalBubble>> chunks =
            new HashMap<>();

    public static TemporalField of(Level level) {
        return ((Access) level).scadrial$temporalField();
    }

    public List<TemporalBubble> bubbles() {
        return bubbles;
    }

    public void replace(List<TemporalBubble> next) {
        if (bubbles.equals(next)) {
            return;
        }

        bubbles = List.copyOf(next);
        chunks.clear();

        for (TemporalBubble bubble : bubbles) {
            var box = bubble.bounds();

            int minX = Mth.floor(box.minX) >> 4;
            int maxX = Mth.floor(box.maxX) >> 4;

            int minZ = Mth.floor(box.minZ) >> 4;
            int maxZ = Mth.floor(box.maxZ) >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    chunks.computeIfAbsent(
                            ChunkPos.asLong(x, z),
                            ignored -> new ArrayList<>()
                    ).add(bubble);
                }
            }
        }
    }

    public double rate(Vec3 point) {
        return rate(point, null);
    }

    public double rate(Entity entity) {
        return rate(
                entity.getBoundingBox().getCenter(),
                entity
        );
    }

    private double rate(Vec3 point, Entity entity) {
        long chunk = ChunkPos.asLong(
                Mth.floor(point.x) >> 4,
                Mth.floor(point.z) >> 4
        );

        List<TemporalBubble> nearby = chunks.get(chunk);

        if (nearby == null) {
            return 1.0;
        }

        int exponent = 0;

        for (TemporalBubble bubble : nearby) {
            boolean ownerInside = entity != null
                    && bubble.owner().equals(entity.getUUID())
                    && bubble.containsOwner(entity.getBoundingBox());

            exponent += ownerInside
                    ? (bubble.bendalloy() ? 1 : -1)
                    : bubble.exponent(point);
        }

        return TemporalAllomancyMath.rate(exponent);
    }
}