package net.rovalio.scadrialmod.power.allomancy.physical.external;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.LongConsumer;

public final class MetalSourceDetector {

    public static final TagKey<Item> TANGIBLE_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "allomancy_tangible"
                    )
            );

    public static final TagKey<Item> ALUMINIUM_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "allomancy_aluminium"
                    )
            );

    public static final TagKey<Block> TANGIBLE_BLOCKS =
            TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID, "allomancy_tangible"
                    )
            );

    public static final TagKey<Block> ALUMINIUM_BLOCKS =
            TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID, "allomancy_aluminium"
                    )
            );

    private record BlockCandidate(
            long position,
            double distanceSquared,
            int priority,
            double alignment
    ) {
    }

    private static final Comparator<BlockCandidate> BLOCK_ORDER =
            (first, second) -> compareCandidates(
                    first.priority(),
                    first.alignment(),
                    first.distanceSquared(),
                    first.position(),
                    second
            );

    private static volatile Set<Block> detectableBlocks;

    private MetalSourceDetector() {
    }

    public static Vec3 chestPosition(ServerPlayer player) {
        double height = Math.min(
                1.25,
                player.getBbHeight() * 0.75
        );

        return player.position().add(0.0, height, 0.0);
    }

    public static List<ItemEntity> findLooseMetal(
            ServerPlayer player,
            double radius,
            int maxResults
    ) {
        validateRadius(radius);

        if (maxResults < 0) {
            throw new IllegalArgumentException(
                    "maxResults cannot be negative"
            );
        }

        if (maxResults == 0) {
            return List.of();
        }

        Vec3 chest = chestPosition(player);
        double radiusSquared = radius * radius;
        AABB searchArea =
                new AABB(chest, chest).inflate(radius);

        List<ItemEntity> found =
                player.serverLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        searchArea,
                        entity -> isDetectable(entity)
                                && entity.getBoundingBox()
                                .getCenter()
                                .distanceToSqr(chest)
                                <= radiusSquared
                );

        found.sort(
                Comparator.comparingDouble(entity ->
                        entity.getBoundingBox()
                                .getCenter()
                                .distanceToSqr(chest)
                )
        );

        return List.copyOf(
                found.subList(
                        0,
                        Math.min(found.size(), maxResults)
                )
        );
    }

    public static boolean isValidTarget(
            ServerPlayer player,
            ItemEntity entity,
            double radius
    ) {
        validateRadius(radius);

        return entity.level() == player.serverLevel()
                && isDetectable(entity)
                && entity.getBoundingBox()
                .getCenter()
                .distanceToSqr(chestPosition(player))
                <= radius * radius;
    }

    public static List<BlockPos> findMetalBlocks(
            ServerPlayer player,
            double radius,
            int maxResults
    ) {
        validateRadius(radius);

        if (maxResults < 0) {
            throw new IllegalArgumentException(
                    "maxResults cannot be negative"
            );
        }

        if (maxResults == 0) {
            return List.of();
        }

        BlockSearch search = new BlockSearch(
                player, radius, maxResults
        );

        MetalBlockIndex.forEach(
                player.serverLevel(),
                search.chest,
                radius,
                search
        );

        return search.results();
    }

    private static final class BlockSearch implements LongConsumer {

        private final Vec3 chest;
        private final Vec3 eyes;
        private final Vec3 look;

        private final boolean hasActive;
        private final long active;

        private final double radiusSquared;
        private final int capacity;

        private BlockCandidate nearest;

        private final PriorityQueue<BlockCandidate> candidates =
                new PriorityQueue<>(BLOCK_ORDER.reversed());

        private BlockSearch(
                ServerPlayer player,
                double radius,
                int capacity
        ) {
            BlockPos activeBlock =
                    ExternalAllomancyPhysics.selected(player).block();

            this.chest = chestPosition(player);
            this.eyes = player.getEyePosition();
            this.look = player.getLookAngle();
            this.hasActive = activeBlock != null;
            this.active = hasActive ? activeBlock.asLong() : 0L;
            this.radiusSquared = radius * radius;
            this.capacity = capacity;
        }

        @Override
        public void accept(long position) {
            double x = BlockPos.getX(position) + 0.5;
            double y = BlockPos.getY(position) + 0.5;
            double z = BlockPos.getZ(position) + 0.5;

            double dx = x - chest.x;
            double dy = y - chest.y;
            double dz = z - chest.z;
            double distance = dx * dx + dy * dy + dz * dz;

            if (distance > radiusSquared) {
                return;
            }

            double alignment = alignment(x, y, z);

            int priority = hasActive && position == active
                    ? 0
                    : alignment >= MetalTargetSelector.ACQUIRE_COS
                    ? 1
                    : 2;

            boolean closest = nearest == null
                    || distance < nearest.distanceSquared();

            if (!closest
                    && candidates.size() == capacity
                    && compareCandidates(
                    priority,
                    alignment,
                    distance,
                    position,
                    candidates.peek()
            ) >= 0) {
                return;
            }

            BlockCandidate candidate = new BlockCandidate(
                    position,
                    distance,
                    priority,
                    alignment
            );

            if (closest) {
                nearest = candidate;
            }

            keep(candidate);
        }

        private double alignment(double x, double y, double z) {
            double ox = x - eyes.x;
            double oy = y - eyes.y;
            double oz = z - eyes.z;
            double lengthSquared = ox * ox + oy * oy + oz * oz;

            if (lengthSquared < 1.0E-10) {
                return -1.0;
            }

            return (look.x * ox + look.y * oy + look.z * oz)
                    / Math.sqrt(lengthSquared);
        }

        private void keep(BlockCandidate candidate) {
            if (candidates.size() == capacity) {
                if (BLOCK_ORDER.compare(candidate, candidates.peek()) >= 0) {
                    return;
                }

                candidates.poll();
            }

            candidates.add(candidate);
        }

        private List<BlockPos> results() {
            if (nearest != null && !candidates.contains(nearest)) {
                if (candidates.size() == capacity) {
                    candidates.poll();
                }

                candidates.add(nearest);
            }

            return candidates.stream()
                    .mapToLong(BlockCandidate::position)
                    .sorted()
                    .mapToObj(BlockPos::of)
                    .toList();
        }
    }

    private static int compareCandidates(
            int priority,
            double alignment,
            double distanceSquared,
            long position,
            BlockCandidate other
    ) {
        int byPriority = Integer.compare(priority, other.priority());

        if (byPriority != 0) {
            return byPriority;
        }

        double key = priority == 1 ? -alignment : distanceSquared;
        double otherKey = other.priority() == 1
                ? -other.alignment()
                : other.distanceSquared();

        int byKey = Double.compare(key, otherKey);

        return byKey != 0
                ? byKey
                : Long.compare(position, other.position());
    }

    public static boolean isValidBlockTarget(
            ServerPlayer player, BlockPos position, double radius
    ) {
        validateRadius(radius);
        ServerLevel level = player.serverLevel();
        return !level.isOutsideBuildHeight(position)
                && level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ())) != null
                && Vec3.atCenterOf(position).distanceToSqr(chestPosition(player))
                <= radius * radius
                && isDetectableBlock(level.getBlockState(position));
    }

    public static boolean isDetectableBlock(BlockState state) {
        return detectableBlocks().contains(state.getBlock());
    }

    public static Set<Block> detectableBlocks() {
        Set<Block> blocks = detectableBlocks;

        if (blocks == null) {
            refreshDetectableBlocks();
            blocks = detectableBlocks;
        }

        return blocks;
    }

    public static void refreshDetectableBlocks() {
        Set<Block> blocks = new ReferenceOpenHashSet<>();

        for (Holder<Block> holder
                : BuiltInRegistries.BLOCK.getTagOrEmpty(TANGIBLE_BLOCKS)) {
            if (!holder.is(ALUMINIUM_BLOCKS)) {
                blocks.add(holder.value());
            }
        }

        detectableBlocks = blocks;
    }

    private static boolean isDetectable(ItemEntity entity) {
        ItemStack stack = entity.getItem();

        return entity.isAlive() && EntityMetalSources.isMetalItem(stack);
    }

    private static void validateRadius(double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            throw new IllegalArgumentException(
                    "Detection radius must be finite and positive"
            );
        }
    }
}
