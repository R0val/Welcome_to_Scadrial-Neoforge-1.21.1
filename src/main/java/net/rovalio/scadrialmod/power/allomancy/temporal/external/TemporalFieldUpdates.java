package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class TemporalFieldUpdates {

    private static final Map<ServerLevel, TemporalField.Rebuilder> BUILDERS =
            new IdentityHashMap<>();

    private TemporalFieldUpdates() {
    }

    public static void replace(
            ServerLevel level,
            List<TemporalBubble> bubbles
    ) {
        requireServerThread(level);

        TemporalField field = TemporalField.of(level);

        if (field.isClosed()) {
            return;
        }

        TemporalField.Rebuilder builder = BUILDERS.computeIfAbsent(
                level,
                key -> {
                    String dimension =
                            key.dimension().location().toString();

                    return new TemporalField.Rebuilder(
                            field,
                            Util.backgroundExecutor(),
                            key.getServer(),
                            error -> ScadrialMod.LOGGER.error(
                                    "Failed to rebuild temporal field in {}",
                                    dimension,
                                    error
                            )
                    );
                }
        );

        builder.replace(bubbles);
    }

    private static void requireServerThread(ServerLevel level){
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Temporal field updates must run on the Minecraft server thread"
            );
        }
    }

    private static void forget(ServerLevel level){
        requireServerThread(level);

        TemporalField.Rebuilder builder = BUILDERS.remove(level);

        if (builder != null) {
            builder.close();
        }
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload event){
        if (event.getLevel() instanceof ServerLevel level){
            if (level.getServer().isSameThread()){
                forget(level);
            } else {
                level.getServer().execute(
                        () -> forget(level)
                );
            }
        }
    }
}
