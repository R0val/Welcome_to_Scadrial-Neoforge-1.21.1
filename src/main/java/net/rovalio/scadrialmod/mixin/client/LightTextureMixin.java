package net.rovalio.scadrialmod.mixin.client;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;

import net.rovalio.scadrialmod.client.PhysicalInternalAllomancyClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Inject(
            method = "getBrightness",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void scadrial$tinBrightness(
            DimensionType dimension,
            int light,
            CallbackInfoReturnable<Float> callback
    ) {
        callback.setReturnValue(
                PhysicalInternalAllomancyClient.brightness(
                        callback.getReturnValue()
                )
        );
    }
}