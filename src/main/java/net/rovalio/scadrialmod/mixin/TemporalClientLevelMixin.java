package net.rovalio.scadrialmod.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalEntityTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class TemporalClientLevelMixin {

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
}