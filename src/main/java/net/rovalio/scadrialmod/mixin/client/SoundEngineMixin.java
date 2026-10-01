package net.rovalio.scadrialmod.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;

import net.rovalio.scadrialmod.client.PhysicalInternalAllomancyClient;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle>
            instanceToChannel;

    @Unique
    private double scadrial$previousTin = Double.NaN;

    @Shadow
    private float calculateVolume(SoundInstance sound) {
        throw new AssertionError();
    }

    @Inject(
            method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void scadrial$amplify(
            float volume,
            SoundSource source,
            CallbackInfoReturnable<Float> callback
    ) {
        callback.setReturnValue(
                callback.getReturnValue()
                        * PhysicalInternalAllomancyClient.soundGain()
        );
    }

    @ModifyExpressionValue(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/Sound;getAttenuationDistance()I"
            )
    )
    private int scadrial$hearingDistance(int distance) {
        return Math.round(
                distance * PhysicalInternalAllomancyClient.hearingRange()
        );
    }

    @Inject(
            method = "tickNonPaused",
            at = @At("TAIL")
    )
    private void scadrial$refreshPlayingSounds(CallbackInfo callback) {
        double strength = PhysicalInternalAllomancyClient.tinStrength();

        if (strength == scadrial$previousTin) {
            return;
        }

        scadrial$previousTin = strength;

        float range = PhysicalInternalAllomancyClient.hearingRange();

        instanceToChannel.forEach((sound, channel) -> {
            float gain = calculateVolume(sound);

            float distance =
                    Math.max(sound.getVolume(), 2.0F)
                            * sound.getSound().getAttenuationDistance()
                            * range;

            boolean attenuated =
                    sound.getAttenuation()
                            == SoundInstance.Attenuation.LINEAR;

            channel.execute(source -> {
                source.setVolume(gain);

                if (attenuated) {
                    source.linearAttenuation(distance);
                }
            });
        });
    }
}