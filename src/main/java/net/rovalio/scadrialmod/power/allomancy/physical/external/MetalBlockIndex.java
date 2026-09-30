package net.rovalio.scadrialmod.power.allomancy.physical.external;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class MetalBlockIndex {

    private static final int MAX_IN_FLIGHT = 64;
    private static final int MAX_SNAPSHOTS_PER_TICK = 256;
    private static final int MAX_DISPATCH_CHECKS_PER_TICK = 4096;
    private static final long REINDEX_AFTER_TICKS = 1200L;

    private static final Map<ServerLevel, LevelIndex> LEVELS =
            new IdentityHashMap<>();

    private static volatile int tagGeneration;

    private static int inFlight;

    private MetalBlockIndex() {
    }

    private static final class LevelIndex {

        private final int generation;

        private final Long2ObjectOpenHashMap<Section> sections =
                new Long2ObjectOpenHashMap<>();

        private final Long2IntOpenHashMap versions =
                new Long2IntOpenHashMap();

        private final LongOpenHashSet pending =
                new LongOpenHashSet();

        private final LongLinkedOpenHashSet queued =
                new LongLinkedOpenHashSet();

        private LevelIndex(int generation) {
            this.generation = generation;
        }
    }

    private static final class Section {

        private final long indexedAt;
        private LongOpenHashSet positions;

        private Section(long indexedAt, long[] positions) {
            this.indexedAt = indexedAt;

            if (positions != null && positions.length > 0) {
                this.positions = new LongOpenHashSet(positions);
            }
        }

        private void add(long position) {
            if (positions == null) {
                positions = new LongOpenHashSet();
            }

            positions.add(position);
        }

        private void remove(long position) {
            if (positions != null) {
                positions.remove(position);
            }
        }

        private void forEach(LongConsumer consumer) {
            if (positions != null && !positions.isEmpty()) {
                positions.forEach(consumer);
            }
        }
    }

    private record MissingSection(
            long key,
            double distanceSquared
    ) {
    }

    public static void forEach(
            ServerLevel level,
            Vec3 center,
            double radius,
            LongConsumer consumer
    ) {
        LevelIndex index = indexFor(level);
        long now = level.getGameTime();
        double radiusSquared = radius * radius;

        int minX = SectionPos.blockToSectionCoord(Mth.floor(center.x - radius));
        int maxX = SectionPos.blockToSectionCoord(Mth.floor(center.x + radius));
        int minZ = SectionPos.blockToSectionCoord(Mth.floor(center.z - radius));
        int maxZ = SectionPos.blockToSectionCoord(Mth.floor(center.z + radius));

        int minY = Math.max(
                level.getMinSection(),
                SectionPos.blockToSectionCoord(Mth.floor(center.y - radius))
        );

        int maxY = Math.min(
                level.getMaxSection() - 1,
                SectionPos.blockToSectionCoord(Mth.floor(center.y + radius))
        );

        if (minY > maxY) {
            return;
        }

        List<MissingSection> missing = null;

        for (int x = minX; x <= maxX; x++) {
            double dx = axisDistance(center.x, x);

            if (dx * dx > radiusSquared) {
                continue;
            }

            for (int z = minZ; z <= maxZ; z++) {
                double dz = axisDistance(center.z, z);
                double horizontal = dx * dx + dz * dz;

                if (horizontal > radiusSquared) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    double dy = axisDistance(center.y, y);
                    double distanceSquared = horizontal + dy * dy;

                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    long key = SectionPos.asLong(x, y, z);
                    Section section = index.sections.get(key);

                    if (section != null) {
                        section.forEach(consumer);

                        if (now - section.indexedAt < REINDEX_AFTER_TICKS) {
                            continue;
                        }
                    }

                    if (index.pending.contains(key)
                            || index.queued.contains(key)) {
                        continue;
                    }

                    if (missing == null) {
                        missing = new ArrayList<>();
                    }

                    missing.add(new MissingSection(key, distanceSquared));
                }
            }
        }

        if (missing != null) {
            missing.sort(
                    Comparator.comparingDouble(MissingSection::distanceSquared)
            );

            for (MissingSection section : missing) {
                index.queued.add(section.key());
            }
        }
    }

    public static void onBlockChanged(
            ServerLevel level,
            BlockPos position,
            BlockState previous,
            BlockState current
    ) {
        Set<Block> blocks = MetalSourceDetector.detectableBlocks();

        boolean wasMetal = blocks.contains(previous.getBlock());
        boolean isMetal = blocks.contains(current.getBlock());

        if (wasMetal == isMetal) {
            return;
        }

        long packed = position.asLong();

        if (!level.getServer().isSameThread()) {
            level.getServer().execute(
                    () -> applyChange(level, packed, isMetal)
            );
            return;
        }

        applyChange(level, packed, isMetal);
    }

    private static void applyChange(
            ServerLevel level,
            long position,
            boolean metal
    ) {
        LevelIndex index = LEVELS.get(level);

        if (index == null) {
            return;
        }

        long key = SectionPos.blockToSection(position);

        if (index.pending.contains(key)) {
            index.versions.addTo(key, 1);
        }

        Section section = index.sections.get(key);

        if (section == null) {
            return;
        }

        if (metal) {
            section.add(position);
        } else {
            section.remove(position);
        }
    }

    @SubscribeEvent
    public static void dispatch(ServerTickEvent.Post event) {
        int snapshots = 0;
        int checks = 0;

        for (Map.Entry<ServerLevel, LevelIndex> entry : LEVELS.entrySet()) {
            ServerLevel level = entry.getKey();
            LevelIndex index = entry.getValue();

            while (!index.queued.isEmpty()
                    && inFlight < MAX_IN_FLIGHT
                    && snapshots < MAX_SNAPSHOTS_PER_TICK
                    && checks < MAX_DISPATCH_CHECKS_PER_TICK) {

                long key = index.queued.removeFirstLong();
                checks++;

                if (index.pending.contains(key)) {
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunkNow(
                        SectionPos.x(key),
                        SectionPos.z(key)
                );

                if (chunk != null && dispatch(level, index, chunk, key)) {
                    snapshots++;
                }
            }
        }
    }

    private static boolean dispatch(
            ServerLevel level,
            LevelIndex index,
            LevelChunk chunk,
            long key
    ) {
        int sectionY = SectionPos.y(key);

        if (sectionY < level.getMinSection()
                || sectionY >= level.getMaxSection()) {
            return false;
        }

        LevelChunkSection section = chunk.getSection(
                chunk.getSectionIndexFromSectionY(sectionY)
        );

        Set<Block> blocks = MetalSourceDetector.detectableBlocks();
        long now = level.getGameTime();

        if (section.hasOnlyAir()
                || !section.getStates().maybeHas(
                state -> blocks.contains(state.getBlock())
        )) {
            index.sections.put(key, new Section(now, null));
            return false;
        }

        PalettedContainer<BlockState> snapshot = section.getStates().copy();
        int version = index.versions.get(key);

        int originX = SectionPos.sectionToBlockCoord(SectionPos.x(key));
        int originY = SectionPos.sectionToBlockCoord(sectionY);
        int originZ = SectionPos.sectionToBlockCoord(SectionPos.z(key));

        index.pending.add(key);
        inFlight++;

        CompletableFuture
                .supplyAsync(
                        () -> scan(snapshot, originX, originY, originZ, blocks),
                        Util.backgroundExecutor()
                )
                .whenCompleteAsync(
                        (positions, error) -> complete(
                                level,
                                index,
                                key,
                                version,
                                positions,
                                error
                        ),
                        level.getServer()
                );

        return true;
    }

    private static long[] scan(
            PalettedContainer<BlockState> states,
            int originX,
            int originY,
            int originZ,
            Set<Block> blocks
    ) {
        LongArrayList found = new LongArrayList();

        for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    if (blocks.contains(states.get(x, y, z).getBlock())) {
                        found.add(BlockPos.asLong(
                                originX + x,
                                originY + y,
                                originZ + z
                        ));
                    }
                }
            }
        }

        return found.toLongArray();
    }

    private static void complete(
            ServerLevel level,
            LevelIndex index,
            long key,
            int version,
            long[] positions,
            Throwable error
    ) {
        if (!level.getServer().isSameThread()) {
            return;
        }

        inFlight = Math.max(0, inFlight - 1);

        if (LEVELS.get(level) != index || !index.pending.remove(key)) {
            return;
        }

        if (error != null) {
            ScadrialMod.LOGGER.error(
                    "Failed to index metal blocks in section {}",
                    SectionPos.of(key),
                    error
            );

            index.sections.put(key, new Section(level.getGameTime(), null));
            return;
        }

        if (index.versions.get(key) != version
                || level.getChunkSource().getChunkNow(
                SectionPos.x(key),
                SectionPos.z(key)
        ) == null) {
            return;
        }

        index.versions.remove(key);
        index.sections.put(key, new Section(level.getGameTime(), positions));
    }

    private static LevelIndex indexFor(ServerLevel level) {
        int generation = tagGeneration;
        LevelIndex index = LEVELS.get(level);

        if (index == null || index.generation != generation) {
            index = new LevelIndex(generation);
            LEVELS.put(level, index);
        }

        return index;
    }

    private static double axisDistance(double coordinate, int section) {
        double min = SectionPos.sectionToBlockCoord(section);
        double max = min + SectionPos.SECTION_SIZE;

        if (coordinate < min) {
            return min - coordinate;
        }

        return coordinate > max
                ? coordinate - max
                : 0.0;
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        MetalSourceDetector.refreshDetectableBlocks();
        MetalSourceProperties.clearResolvedMasses();

        if (event.getUpdateCause()
                == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            tagGeneration++;
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ChunkPos position = event.getChunk().getPos();

        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> forgetChunk(level, position));
            return;
        }

        forgetChunk(level, position);
    }

    private static void forgetChunk(ServerLevel level, ChunkPos position) {
        LevelIndex index = LEVELS.get(level);

        if (index == null) {
            return;
        }

        for (int y = level.getMinSection(); y < level.getMaxSection(); y++) {
            long key = SectionPos.asLong(position.x, y, position.z);

            index.sections.remove(key);
            index.queued.remove(key);

            if (index.pending.contains(key)) {
                index.versions.addTo(key, 1);
            } else {
                index.versions.remove(key);
            }
        }
    }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LEVELS.clear();
        inFlight = 0;
    }
}
