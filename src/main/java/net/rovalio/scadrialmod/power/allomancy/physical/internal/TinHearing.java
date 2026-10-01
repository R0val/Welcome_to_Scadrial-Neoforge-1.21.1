package net.rovalio.scadrialmod.power.allomancy.physical.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LevelEvent;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class TinHearing {

    private static final double EVENT_RANGE = 64.0;

    private record Listener(
            ServerPlayer player,
            double rangeMultiplier
    ) {
    }

    private static Map<ServerLevel, List<Listener>> listeners = Map.of();

    private TinHearing() {
    }

    @SubscribeEvent
    public static void refreshListeners(ServerTickEvent.Pre event) {
        Map<ServerLevel, List<Listener>> updated = null;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            double strength = PhysicalInternalAllomancyManager.strength(
                    player,
                    AllomanticFuel.TIN
            );

            if (strength <= 0.0) {
                continue;
            }

            if (updated == null) {
                updated = new IdentityHashMap<>();
            }

            updated.computeIfAbsent(
                    player.serverLevel(),
                    ignored -> new ArrayList<>()
            ).add(new Listener(
                    player,
                    PhysicalInternalAllomancyMath.hearingRange(strength)
            ));
        }

        listeners = updated == null ? Map.of() : updated;
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        listeners = Map.of();
    }

    public static void broadcastDistant(
            ServerLevel level,
            Player excluded,
            double x,
            double y,
            double z,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume,
            float pitch,
            long seed
    ) {
        List<Listener> levelListeners = listeners.get(level);

        if (levelListeners == null || volume <= 0.0F) {
            return;
        }

        double normalRange = sound.value().getRange(volume);

        if (normalRange <= 0.0) {
            return;
        }

        double normalRangeSquared = normalRange * normalRange;

        for (Listener listener : levelListeners) {
            ServerPlayer player = listener.player();

            if (player == excluded
                    || player.isRemoved()
                    || player.level() != level) {
                continue;
            }

            double distanceSquared = player.distanceToSqr(x, y, z);
            double extendedRange = normalRange * listener.rangeMultiplier();

            if (distanceSquared >= normalRangeSquared
                    && distanceSquared < extendedRange * extendedRange) {

                player.connection.send(
                        new ClientboundSoundPacket(
                                sound,
                                category,
                                x,
                                y,
                                z,
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
        if (type < 1000 || type >= 2000) {
            return;
        }

        double normalRangeSquared = EVENT_RANGE * EVENT_RANGE;

        double x = position.getX();
        double y = position.getY();
        double z = position.getZ();

        if (type == LevelEvent.SOUND_STOP_JUKEBOX_SONG) {
            for (ServerPlayer player : level.players()) {
                if (player != excluded
                        && player.distanceToSqr(x, y, z) >= normalRangeSquared) {

                    player.connection.send(
                            new ClientboundLevelEventPacket(
                                    type,
                                    position,
                                    data,
                                    false
                            )
                    );
                }
            }

            return;
        }

        List<Listener> levelListeners = listeners.get(level);

        if (levelListeners == null) {
            return;
        }

        for (Listener listener : levelListeners) {
            ServerPlayer player = listener.player();

            if (player == excluded
                    || player.isRemoved()
                    || player.level() != level) {
                continue;
            }

            double distanceSquared = player.distanceToSqr(x, y, z);
            double extendedRange = EVENT_RANGE * listener.rangeMultiplier();

            if (distanceSquared >= normalRangeSquared
                    && distanceSquared < extendedRange * extendedRange) {

                player.connection.send(
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
