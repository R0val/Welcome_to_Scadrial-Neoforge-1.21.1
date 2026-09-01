package net.rovalio.scadrialmod.origin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.rovalio.CosmereAPI.item.custom.TornPagesItem;
import net.rovalio.CosmereAPI.onboarding.OnboardingResult;
import net.rovalio.CosmereAPI.onboarding.OriginInitializationRegistry;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.knowledge.ScadrialStartingKnowledge;
import net.rovalio.scadrialmod.power.ScadrialPowerAssigner;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;
import net.rovalio.scadrialmod.registry.ScadrialPlanets;

import java.util.List;

public final class ScadrialOriginInitializer {

    private ScadrialOriginInitializer() {
    }

    public static void register() {
        OriginInitializationRegistry.register(
                ScadrialOrigins.NOBLE.getId(),
                ScadrialOriginInitializer::initialize
        );

        OriginInitializationRegistry.register(
                ScadrialOrigins.TERRIS.getId(),
                ScadrialOriginInitializer::initialize
        );

        OriginInitializationRegistry.register(
                ScadrialOrigins.SKAA.getId(),
                ScadrialOriginInitializer::initialize
        );
    }

    private static OnboardingResult initialize(
            ServerPlayer player,
            ResourceLocation planetId,
            ResourceLocation originId
    ) {
        if (player == null
                || planetId == null
                || originId == null) {

            return OnboardingResult.INVALID_REQUEST;
        }

        if (!ScadrialPlanets.SCADRIAL
                .getId()
                .equals(planetId)) {

            return OnboardingResult.INITIALIZATION_FAILED;
        }

        if (!isSupportedOrigin(originId)) {
            return OnboardingResult.UNKNOWN_ORIGIN;
        }

        List<ResourceLocation> startingKnowledge =
                ScadrialStartingKnowledge
                        .getForOrigin(originId);

        if (startingKnowledge == null
                || startingKnowledge.isEmpty()) {

            return OnboardingResult.INITIALIZATION_FAILED;
        }

        /*
         * Validate the onboarding reward before
         * modifying the player's power data.
         */
        ItemStack tornPages =
                TornPagesItem.create(
                        planetId,
                        startingKnowledge
                );

        if (tornPages.isEmpty()) {
            return OnboardingResult.REWARD_DELIVERY_FAILED;
        }

        try {
            /*
             * A completed assignment returns false
             * without rolling powers again.
             */
            ScadrialPowerAssigner
                    .assignInitialPowers(
                            player,
                            originId
                    );

        } catch (RuntimeException exception) {

            ScadrialMod.LOGGER.error(
                    "Failed to assign initial "
                            + "Scadrian powers to player {} "
                            + "for origin {}",
                    player.getUUID(),
                    originId,
                    exception
            );

            return OnboardingResult.INITIALIZATION_FAILED;
        }

        return deliverTornPages(
                player,
                tornPages
        );
    }

    private static boolean isSupportedOrigin(
            ResourceLocation originId
    ) {
        return ScadrialOrigins.NOBLE
                .getId()
                .equals(originId)
                || ScadrialOrigins.TERRIS
                .getId()
                .equals(originId)
                || ScadrialOrigins.SKAA
                .getId()
                .equals(originId);
    }

    private static OnboardingResult deliverTornPages(
            ServerPlayer player,
            ItemStack tornPages
    ) {
        player.getInventory().add(
                tornPages
        );

        if (tornPages.isEmpty()) {
            return OnboardingResult.SUCCESS;
        }

        ItemEntity droppedPages =
                player.drop(
                        tornPages,
                        false
                );

        return droppedPages != null
                ? OnboardingResult.SUCCESS
                : OnboardingResult.REWARD_DELIVERY_FAILED;
    }
}