package net.rovalio.scadrialmod.player;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.CosmereAPI.player.PlayerStateLifecycleRegistry;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;

import java.util.Locale;

public final class ScadrialPlayerLifecycleHandler
        implements PlayerStateLifecycleRegistry.Handler {

    private static final ResourceLocation
            HANDLER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "player_state"
            );

    private static final ResourceLocation
            ALLOMANCY_SNAPPING_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "allomancy_snapping"
            );

    private static final ResourceLocation
            METALLIC_INVESTITURE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "metallic_arts_investiture"
            );

    private ScadrialPlayerLifecycleHandler() {
    }

    public static void register() {
        PlayerStateLifecycleRegistry.register(
                HANDLER_ID,
                new ScadrialPlayerLifecycleHandler()
        );
    }

    @Override
    public void recalculate(
            ServerPlayer player,
            PlayerStateLifecycleRegistry.Reason reason,
            PlayerStateLifecycleRegistry
                    .RecalculationContext context
    ) {

        ScadrialPowerManager.reconcile(player);

        if (reason
                != PlayerStateLifecycleRegistry
                .Reason
                .RESET_STATS) {

            return;
        }

        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        double investitureBonus =
                ScadrialPowerManager.getInvestitureBonusBEU(data);

        if (investitureBonus > 0.0) {
            context.addModifier(
                    METALLIC_INVESTITURE_MODIFIER_ID,
                    0.0,
                    investitureBonus,
                    0.0
            );
        }

        if (!data.isAllomancySnapped()) {
            return;
        }

        double snappingDamage =
                data.getAllomancySnappingDamage();

        if (!Double.isFinite(snappingDamage)
                || snappingDamage <= 0.0) {

            return;
        }

        context.addModifier(
                ALLOMANCY_SNAPPING_MODIFIER_ID,
                -snappingDamage,
                0.0,
                0.0
        );

        context.addMessage(
                ALLOMANCY_SNAPPING_MODIFIER_ID,
                Component.literal(
                        String.format(
                                Locale.ROOT,
                                "Allomantic Snapping damage "
                                        + "restored: %.2f%% Integrity",
                                snappingDamage * 100.0
                        )
                )
        );
    }

    @Override
    public void reset(
            ServerPlayer player
    ) {

        ScadrialPowerManager.reset(player);
    }
}
