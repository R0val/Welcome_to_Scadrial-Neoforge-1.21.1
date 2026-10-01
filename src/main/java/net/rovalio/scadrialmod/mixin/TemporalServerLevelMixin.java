package net.rovalio.scadrialmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalEntityTicks;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class TemporalServerLevelMixin {

    @Unique
    private boolean scadrial$insideTemporalTick;

    @Shadow
    public abstract void tickNonPassenger(Entity entity);

    @Inject(
            method = "tickNonPassenger",
            at = @At("HEAD"),
            cancellable = true
    )
    private void scadrial$tickEntity(
            Entity entity,
            CallbackInfo ci
    ) {
        if (scadrial$insideTemporalTick
                || !TemporalEntityTicks.controlled(entity)) {
            return;
        }

        scadrial$insideTemporalTick = true;

        try {
            TemporalEntityTicks.tick(
                    entity,
                    () -> tickNonPassenger(entity)
            );
        } finally {
            scadrial$insideTemporalTick = false;
        }

        ci.cancel();
    }

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
            )
    )
    private void scadrial$randomBlock(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        int steps = scadrial$randomSteps(level, pos, random);

        for (int i = 0; i < steps; i++) {
            BlockState current = level.getBlockState(pos);

            if (!current.is(state.getBlock())
                    || !current.isRandomlyTicking()) {
                break;
            }

            current.randomTick(level, pos, random);
        }
    }

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
            )
    )
    private void scadrial$randomFluid(
            FluidState state,
            Level world,
            BlockPos pos,
            RandomSource random
    ) {
        ServerLevel level = (ServerLevel) world;

        int steps = scadrial$randomSteps(level, pos, random);

        for (int i = 0; i < steps; i++) {
            FluidState current = level.getFluidState(pos);

            if (!current.is(state.getType())
                    || !current.isRandomlyTicking()) {
                break;
            }

            current.randomTick(level, pos, random);
        }
    }

    @Unique
    private static int scadrial$randomSteps(
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        double rate = TemporalField.of(level).rate(
                Vec3.atCenterOf(pos)
        );

        if (rate >= 1.0) {
            return (int) rate;
        }

        return random.nextDouble() < rate ? 1 : 0;
    }
}