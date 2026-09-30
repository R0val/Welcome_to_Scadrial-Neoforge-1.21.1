package net.rovalio.scadrialmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.rovalio.scadrialmod.power.allomancy.physical.external.ExternalAllomancyMath;
import net.rovalio.scadrialmod.sound.ScadrialSounds;

public final class AllomancyLoopSound extends AbstractTickableSoundInstance {

    public static final int VOLUME_RAMP_TICKS = 2;
    public static final float MIN_VOLUME = 0.16F;
    public static final float MAX_VOLUME = 2.0F;

    private static AllomancyLoopSound pushing;
    private static AllomancyLoopSound pulling;

    private final LocalPlayer player;
    private int heldTicks;

    private AllomancyLoopSound(
            SoundEvent sound,
            LocalPlayer player
    ) {
        super(
                sound,
                SoundSource.PLAYERS,
                SoundInstance.createUnseededRandom()
        );

        this.player = player;

        looping = true;
        delay = 0;
        relative = true;
        attenuation = Attenuation.NONE;

        volume = MAX_VOLUME;
        pitch = 1.0F;
    }

    public static void update(
            boolean push,
            boolean pull
    ) {
        pushing = updateLoop(
                pushing,
                push,
                ScadrialSounds.PUSH.get()
        );

        pulling = updateLoop(
                pulling,
                pull,
                ScadrialSounds.PULL.get()
        );
    }

    private static AllomancyLoopSound updateLoop(
            AllomancyLoopSound current,
            boolean active,
            SoundEvent sound
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!active || minecraft.player == null) {
            stopLoop(current);
            return null;
        }

        if (current == null || current.isStopped()) {
            current = new AllomancyLoopSound(
                    sound,
                    minecraft.player
            );

            minecraft.getSoundManager().play(current);
        }

        return current;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != player
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || minecraft.screen != null
                || !minecraft.isWindowActive()) {
            stop();
            return;
        }

        heldTicks = Math.min(
                heldTicks + 1,
                VOLUME_RAMP_TICKS
        );

        float progress = (float) ExternalAllomancyMath.ramp(
                heldTicks,
                VOLUME_RAMP_TICKS
        );

        volume = MIN_VOLUME
                + (MAX_VOLUME - MIN_VOLUME) * progress;
    }

    private static void stopLoop(AllomancyLoopSound sound) {
        if (sound != null) {
            sound.stop();
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
    }

    public static void clear() {
        stopLoop(pushing);
        stopLoop(pulling);

        pushing = null;
        pulling = null;
    }
}