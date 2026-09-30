package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

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
            BlockPos position,
            double distanceSquared,
            int priority,
            double alignment
    ) {
    }

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

        search.scan(player.serverLevel(), radius);
        return search.results();
    }

    private static final class BlockSearch {

        private final Vec3 chest;
        private final Vec3 eyes;
        private final Vec3 look;
        private final BlockPos active;

        private final double radiusSquared;
        private final int capacity;

        private BlockCandidate nearest;

        private final Comparator<BlockCandidate> order =
                Comparator.comparingInt(BlockCandidate::priority)
                        .thenComparingDouble(candidate ->
                                candidate.priority() == 1
                                        ? -candidate.alignment()
                                        : candidate.distanceSquared()
                        )
                        .thenComparingLong(candidate ->
                                candidate.position().asLong()
                        );

        private final PriorityQueue<BlockCandidate> candidates =
                new PriorityQueue<>(order.reversed());

        private BlockSearch(
                ServerPlayer player,
                double radius,
                int capacity
        ) {
            this.chest = chestPosition(player);
            this.eyes = player.getEyePosition();
            this.look = player.getLookAngle();
            this.active = ExternalAllomancyPhysics.selected(player).block();
            this.radiusSquared = radius * radius;
            this.capacity = capacity;
        }

        private void scan(ServerLevel level, double radius) {
            int minX = chunkCoordinate(chest.x - radius);
            int maxX = chunkCoordinate(chest.x + radius);
            int minZ = chunkCoordinate(chest.z - radius);
            int maxZ = chunkCoordinate(chest.z + radius);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    LevelChunk chunk = level.getChunkSource()
                            .getChunkNow(x, z);

                    if (chunk != null) {
                        chunk.findBlocks(
                                MetalSourceDetector::isDetectableBlock,
                                (position, state) -> accept(position)
                        );
                    }
                }
            }
        }

        private int chunkCoordinate(double coordinate) {
            return SectionPos.blockToSectionCoord(
                    (int) Math.floor(coordinate)
            );
        }

        private void accept(BlockPos position) {
            Vec3 center = Vec3.atCenterOf(position);
            double distance = center.distanceToSqr(chest);

            if (distance > radiusSquared) {
                return;
            }

            double alignment = MetalTargetSelector.alignment(
                    eyes, look, center
            );

            int priority = priority(position, alignment);

            BlockCandidate candidate = new BlockCandidate(
                    position.immutable(),
                    distance,
                    priority,
                    alignment
            );

            if (nearest == null
                    || distance < nearest.distanceSquared()) {
                nearest = candidate;
            }

            keep(candidate);
        }

        private int priority(
                BlockPos position,
                double alignment
        ) {
            if (position.equals(active)) {
                return 0;
            }

            return alignment >= MetalTargetSelector.ACQUIRE_COS
                    ? 1
                    : 2;
        }

        private void keep(BlockCandidate candidate) {
            if (candidates.size() == capacity) {
                if (order.compare(candidate, candidates.peek()) >= 0) {
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
                    .sorted(order)
                    .map(BlockCandidate::position)
                    .toList();
        }
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
        return state.is(TANGIBLE_BLOCKS) && !state.is(ALUMINIUM_BLOCKS);
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
