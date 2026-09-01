package net.rovalio.scadrialmod.player;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.Objects;
import java.util.function.Supplier;

public final class ScadrialAttachments {

    private static final DeferredRegister<
            AttachmentType<?>
            > ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    ScadrialMod.MOD_ID
            );

    // Concierto walls 27 de mayo en el movistar arena

    public static final Supplier<
            AttachmentType<ScadrialPlayerData>
            > SCADRIAL_DATA =
            ATTACHMENT_TYPES.register(
                    "scadrial_data",
                    () -> AttachmentType
                            .serializable(
                                    ScadrialPlayerData::new
                            )
                            .copyOnDeath()
                            .build()
            );

    private ScadrialAttachments() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static ScadrialPlayerData get(
            Player player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        return player.getData(SCADRIAL_DATA);
    }
}