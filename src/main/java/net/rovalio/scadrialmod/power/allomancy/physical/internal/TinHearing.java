package net.rovalio.scadrialmod.power.allomancy.physical.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;

import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

public final class TinHearing {

    private TinHearing() {
    }

    public static void broadcastDistant(
            ServerLevel level,
            Player excluded,
            Vec3 position,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume,
            float pitch,
            long seed
    ) {
        if (volume <= 0.0F) {
            return;
        }

        double normalRange = sound.value().getRange(volume);

        if (normalRange <= 0.0) {
            return;
        }

        for (var listener : level.players()) {
            if (listener == excluded) {
                continue;
            }

            double strength = PhysicalInternalAllomancyManager.strength(
                    listener,
                    AllomanticFuel.TIN
            );

            if (strength <= 0.0) {
                continue;
            }

            double distanceSquared =
                    listener.position().distanceToSqr(position);

            double extendedRange =
                    normalRange
                            * PhysicalInternalAllomancyMath.hearingRange(strength);

            // Vanilla already serves listeners inside its normal radius.
            if (distanceSquared >= normalRange * normalRange
                    && distanceSquared < extendedRange * extendedRange) {

                listener.connection.send(
                        new ClientboundSoundPacket(
                                sound,
                                category,
                                position.x,
                                position.y,
                                position.z,
                                volume,
                                pitch,
                                seed
                        )
                );
            }
        }
    }

    public static void broadcastDistantEvent(
            ServerLevel level,
            Player excluded,
            int type,
            BlockPos position,
            int data
    ) {
        // Vanilla sound events, including jukebox start and stop.
        if (type < 1000 || type >= 2000) {
            return;
        }

        double normalRange = 64.0;

        Vec3 origin = new Vec3(
                position.getX(),
                position.getY(),
                position.getZ()
        );

        for (var listener : level.players()) {
            if (listener == excluded) {
                continue;
            }

            double distanceSquared =
                    listener.position().distanceToSqr(origin);

            // A distant record must stop even if tin was switched off.
            if (type == LevelEvent.SOUND_STOP_JUKEBOX_SONG) {
                if (distanceSquared >= normalRange * normalRange) {
                    listener.connection.send(
                            new ClientboundLevelEventPacket(
                                    type,
                                    position,
                                    data,
                                    false
                            )
                    );
                }

                continue;
            }

            double strength = PhysicalInternalAllomancyManager.strength(
                    listener,
                    AllomanticFuel.TIN
            );

            if (strength <= 0.0) {
                continue;
            }

            double extendedRange =
                    normalRange
                            * PhysicalInternalAllomancyMath.hearingRange(strength);

            if (distanceSquared >= normalRange * normalRange
                    && distanceSquared < extendedRange * extendedRange) {

                listener.connection.send(
                        new ClientboundLevelEventPacket(
                                type,
                                position,
                                data,
                                false
                        )
                );
            }
        }
    }
}
