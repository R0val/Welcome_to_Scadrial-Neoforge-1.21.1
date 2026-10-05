package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class TemporalField {

    public interface Access {
        TemporalField scadrial$temporalField();
    }

    public record Snapshot(
            long version,
            List<TemporalBubble> bubbles,
            Map<Long, List<TemporalBubble>> chunks,
            boolean indexed
    ) {
        public Snapshot {
            bubbles = List.copyOf(bubbles);

            Map<Long, List<TemporalBubble>> frozen =
                    new HashMap<>();

            chunks.forEach((key, value) ->
                    frozen.put(key, List.copyOf(value))
            );

            chunks = Map.copyOf(frozen);
        }

        public Snapshot buildIndex() {
            Map<Long, List<TemporalBubble>> index =
                    new HashMap<>();

            for (TemporalBubble bubble : bubbles) {
                AABB bounds = bubble.bounds();

                int minX = Mth.floor(bounds.minX) >> 4;
                int maxX = Mth.floor(bounds.maxX) >> 4;
                int minZ = Mth.floor(bounds.minZ) >> 4;
                int maxZ = Mth.floor(bounds.maxZ) >> 4;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        index.computeIfAbsent(
                                ChunkPos.asLong(x, z),
                                ignored -> new ArrayList<>()
                        ).add(bubble);
                    }
                }
            }

            return new Snapshot(
                    version,
                    bubbles,
                    index,
                    true
            );
        }

        public boolean mightAffect(BlockPos position) {
            if (bubbles.isEmpty()) {
                return false;
            }

            return !indexed || chunks.containsKey(
                    ChunkPos.asLong(
                            position.getX() >> 4,
                            position.getZ() >> 4
                    )
            );
        }

        public double rate(Vec3 point) {
            return rate(point, null, null);
        }

        public double rate(
                UUID entityId,
                AABB bounds
        ) {
            return rate(
                    bounds.getCenter(),
                    entityId,
                    bounds
            );
        }

        private double rate(
                Vec3 point,
                UUID entityId,
                AABB bounds
        ) {
            List<TemporalBubble> candidates = bubbles;

            if (indexed) {
                long chunk = ChunkPos.asLong(
                        Mth.floor(point.x) >> 4,
                        Mth.floor(point.z) >> 4
                );

                candidates = chunks.getOrDefault(
                        chunk,
                        List.of()
                );
            }

            int exponent = 0;

            for (TemporalBubble bubble : candidates) {
                boolean ownerInside = bounds != null
                        && bubble.owner().equals(entityId)
                        && bubble.containsOwner(bounds);

                exponent += ownerInside
                        ? (bubble.bendalloy() ? 1 : -1)
                        : bubble.exponent(point);
            }

            return TemporalAllomancyMath.rate(exponent);
        }
    }

    private final Thread ownerThread =
            Thread.currentThread();

    private volatile Snapshot current = new Snapshot(
            0L,
            List.of(),
            Map.of(),
            true
    );

    private boolean closed;

    public static TemporalField of(Level level) {
        return ((Access) level).scadrial$temporalField();
    }

    public Snapshot snapshot() {
        return current;
    }

    boolean isClosed() {
        requireOwnerThread();
        return closed;
    }

    public List<TemporalBubble> bubbles() {
        return current.bubbles();
    }

    public double rate(Vec3 point) {
        Snapshot view = current;
        return view.rate(point);
    }

    public double rate(Entity entity) {
        requireOwnerThread();

        Snapshot view = current;

        return view.rate(
                entity.getUUID(),
                entity.getBoundingBox()
        );
    }

    public void replace(List<TemporalBubble> next) {
        Snapshot pending = prepare(next);

        if (!closed && !pending.indexed()) {
            publish(
                    pending,
                    pending.buildIndex()
            );
        }
    }

    private Snapshot prepare(List<TemporalBubble> next) {
        requireOwnerThread();

        Snapshot previous = current;

        if (closed || previous.bubbles().equals(next)) {
            return previous;
        }

        List<TemporalBubble> copy = List.copyOf(next);

        Snapshot pending = new Snapshot(
                previous.version() + 1L,
                copy,
                Map.of(),
                copy.isEmpty()
        );

        current = pending;

        return pending;
    }

    private boolean publish(
            Snapshot expected,
            Snapshot indexed
    ) {
        requireOwnerThread();

        if (closed || current != expected) {
            return false;
        }

        current = indexed;
        return true;
    }

    private void close() {
        requireOwnerThread();

        closed = true;

        current = new Snapshot(
                current.version() + 1L,
                List.of(),
                Map.of(),
                true
        );
    }

    private void requireOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "Temporal field mutation/entity access must run on its owner thread"
            );
        }
    }

    public static final class Rebuilder {

        private final TemporalField field;
        private final Executor workerExecutor;
        private final Executor ownerExecutor;
        private final Consumer<Throwable> errorHandler;

        private boolean inFlight;
        private boolean closed;

        Rebuilder(
                TemporalField field,
                Executor workerExecutor,
                Executor ownerExecutor,
                Consumer<Throwable> errorHandler
        ) {
            field.requireOwnerThread();

            this.field = field;
            this.workerExecutor = workerExecutor;
            this.ownerExecutor = ownerExecutor;
            this.errorHandler = errorHandler;
        }

        public void replace(List<TemporalBubble> next) {
            field.requireOwnerThread();

            if (closed) {
                return;
            }

            field.prepare(next);
            dispatch();
        }

        private void dispatch() {
            Snapshot input = field.snapshot();

            if (closed
                    || field.closed
                    || inFlight
                    || input.indexed()) {
                return;
            }

            inFlight = true;

            try {
                CompletableFuture.supplyAsync(
                        input::buildIndex,
                        workerExecutor
                ).handleAsync((result, error) -> {
                    complete(input, result, error);
                    return null;
                }, ownerExecutor).exceptionally(error -> {
                    errorHandler.accept(error);
                    return null;
                });
            } catch (RuntimeException error) {
                inFlight = false;
                errorHandler.accept(error);
            }
        }

        private void complete(
                Snapshot input,
                Snapshot result,
                Throwable error
        ) {
            field.requireOwnerThread();

            inFlight = false;

            if (closed || field.closed) {
                return;
            }

            if (error != null) {
                errorHandler.accept(error);
            } else {
                field.publish(input, result);
            }

            if (field.snapshot() != input) {
                dispatch();
            }
        }

        public void close() {
            field.requireOwnerThread();

            closed = true;
            field.close();
        }
    }
}