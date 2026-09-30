package net.rovalio.scadrialmod.power.allomancy.physical.external;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload.Target;

public record MetalTarget(BlockPos block, Target entityTarget) {

    public static final MetalTarget NONE = new MetalTarget(null, null);

    public static final StreamCodec<ByteBuf, MetalTarget> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MetalTarget decode(ByteBuf buffer) {
                    return switch (buffer.readUnsignedByte()) {
                        case 0 -> NONE;
                        case 1 -> of(BlockPos.STREAM_CODEC.decode(buffer));
                        case 2 -> of(Target.STREAM_CODEC.decode(buffer));
                        default -> throw new IllegalArgumentException(
                                "Invalid allomantic target kind"
                        );
                    };
                }

                @Override
                public void encode(ByteBuf buffer, MetalTarget target) {
                    if (target.block() != null) {
                        buffer.writeByte(1);
                        BlockPos.STREAM_CODEC.encode(buffer, target.block());
                    } else if (target.entityTarget() != null) {
                        buffer.writeByte(2);
                        Target.STREAM_CODEC.encode(buffer, target.entityTarget());
                    } else {
                        buffer.writeByte(0);
                    }
                }
            };

    public MetalTarget {
        if (block != null && entityTarget != null) {
            throw new IllegalArgumentException(
                    "A target cannot be both block and entity"
            );
        }

        if (block != null) {
            block = block.immutable();
        }
    }

    public static MetalTarget of(BlockPos block) {
        return new MetalTarget(block, null);
    }

    public static MetalTarget of(Target entity) {
        return new MetalTarget(null, entity);
    }

    public boolean isNone() {
        return block == null && entityTarget == null;
    }

    public Entity entity(Level level) {
        if (entityTarget == null) {
            return null;
        }

        Entity entity = level.getEntity(entityTarget.entityId());

        if (entity == null || !entity.getUUID().equals(entityTarget.uuid())) {
            return null;
        }

        return entity;
    }

    public Vec3 position(Level level, float partialTick) {
        if (block != null) {
            return blockPosition(level);
        }

        Entity entity = entity(level);

        if (entity == null
                || !EntityMetalSources.isSource(entity, entityTarget.part())) {
            return null;
        }

        return EntityMetalSources.position(
                entity, entityTarget.part(), partialTick
        );
    }

    private Vec3 blockPosition(Level level) {
        boolean loaded = level.hasChunk(
                SectionPos.blockToSectionCoord(block.getX()),
                SectionPos.blockToSectionCoord(block.getZ())
        );

        if (!loaded
                || !MetalSourceDetector.isDetectableBlock(
                level.getBlockState(block)
        )) {
            return null;
        }

        return Vec3.atCenterOf(block);
    }

    public boolean validFor(ServerPlayer player, double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0) {
            return false;
        }

        if (block != null) {
            return MetalSourceDetector.isValidBlockTarget(
                    player, block, radius
            );
        }

        Entity entity = entity(player.level());

        return entity != null
                && entity.getRootVehicle() != player.getRootVehicle()
                && new EntityMetalSources.Source(
                entity, entityTarget.part()
        ).isValidFor(player, radius);
    }
}