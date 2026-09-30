package net.rovalio.scadrialmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.rovalio.scadrialmod.power.allomancy.physical.internal.TinHearing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSoundMixin {

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("TAIL")
    )
    private void scadrial$distantPosition(
            Player excluded,
            double x,
            double y,
            double z,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume,
            float pitch,
            long seed,
            CallbackInfo callback
    ) {
        TinHearing.broadcastDistant(
                (ServerLevel) (Object) this,
                excluded,
                new Vec3(x, y, z),
                sound,
                category,
                volume,
                pitch,
                seed
        );
    }

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("TAIL")
    )
    private void scadrial$distantEntity(
            Player excluded,
            Entity entity,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume,
            float pitch,
            long seed,
            CallbackInfo callback
    ) {
        TinHearing.broadcastDistant(
                (ServerLevel) (Object) this,
                excluded,
                entity.position(),
                sound,
                category,
                volume,
                pitch,
                seed
        );
    }

    @Inject(
            method = "levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V",
            at = @At("TAIL")
    )
    private void scadrial$distantEvent(
            Player excluded,
            int type,
            BlockPos position,
            int data,
            CallbackInfo callback
    ) {
        TinHearing.broadcastDistantEvent(
                (ServerLevel) (Object) this,
                excluded,
                type,
                position,
                data
        );
    }
}