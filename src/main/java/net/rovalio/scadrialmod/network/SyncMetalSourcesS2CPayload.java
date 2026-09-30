package net.rovalio.scadrialmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.power.allomancy.physical.external.EntityMetalSources.Part;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SyncMetalSourcesS2CPayload(
        ResourceLocation dimension,
        double radius,
        List<Target> targets,
        List<BlockPos> blocks
) implements CustomPacketPayload {

    public static final int MAX_TARGETS = 64;
    public static final int MAX_BLOCK_TARGETS = 64;

    public record Target(
            int entityId,
            UUID uuid,
            Part part
    ) {
        private static final StreamCodec<ByteBuf, Part> PART_CODEC =
                ByteBufCodecs.STRING_UTF8.map(
                        Part::read,
                        Part::serializedName
                );

        public static final StreamCodec<ByteBuf, Target> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        Target::entityId,

                        UUIDUtil.STREAM_CODEC,
                        Target::uuid,

                        PART_CODEC,
                        Target::part,

                        Target::new
                );

        public Target {
            Objects.requireNonNull(uuid);
            Objects.requireNonNull(part);
        }
    }

    public static final Type<SyncMetalSourcesS2CPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "metal_sources"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            SyncMetalSourcesS2CPayload
            > STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SyncMetalSourcesS2CPayload::dimension,

            ByteBufCodecs.DOUBLE,
            SyncMetalSourcesS2CPayload::radius,

            Target.STREAM_CODEC.apply(
                    ByteBufCodecs.list(MAX_TARGETS)
            ),
            SyncMetalSourcesS2CPayload::targets,

            BlockPos.STREAM_CODEC.apply(
                    ByteBufCodecs.list(MAX_BLOCK_TARGETS)
            ),
            SyncMetalSourcesS2CPayload::blocks,

            SyncMetalSourcesS2CPayload::new
    );

    public SyncMetalSourcesS2CPayload {
        Objects.requireNonNull(dimension);
        targets = List.copyOf(targets);
        blocks = List.copyOf(blocks);

        if (!Double.isFinite(radius)
                || radius < 0.0
                || targets.size() > MAX_TARGETS
                || blocks.size() > MAX_BLOCK_TARGETS) {
            throw new IllegalArgumentException(
                    "Invalid metal sources"
            );
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}