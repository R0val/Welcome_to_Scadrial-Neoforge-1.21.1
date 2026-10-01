package net.rovalio.scadrialmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import net.rovalio.scadrialmod.power.allomancy.physical.external.MetalBlockIndex;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN")
    )
    private void scadrial$trackMetalBlocks(
            BlockPos position,
            BlockState state,
            boolean moving,
            CallbackInfoReturnable<BlockState> callback
    ) {
        BlockState previous = callback.getReturnValue();

        if (previous != null
                && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
            MetalBlockIndex.onBlockChanged(level, position, previous, state);
        }
    }
}
