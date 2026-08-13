package net.rovalio.scadrialmod.origin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.rovalio.CosmereAPI.item.custom.TornPagesItem;
import net.rovalio.CosmereAPI.onboarding.OriginInitializationRegistry;
import net.rovalio.scadrialmod.knowledge.ScadrialStartingKnowledge;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;
import net.rovalio.scadrialmod.registry.ScadrialPlanets;

import java.util.List;

public final class ScadrialOriginInitializer {

    private ScadrialOriginInitializer() {
    }

    //register Origin Initialization
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

    //Initializes Scadrial origins
    private static boolean initialize(
            ServerPlayer player,
            ResourceLocation planetId,
            ResourceLocation originId
    ) {

        //Validates Scadrial as a planet
        if (!planetId.equals(
                ScadrialPlanets.SCADRIAL.getId()
        )) {
            return false;
        }

        //Gets starting knowledge
        List<ResourceLocation> startingKnowledge =
                ScadrialStartingKnowledge
                        .getForOrigin(originId);

        if (startingKnowledge.isEmpty()) {
            return false;
        }

        //Creates tornPages with origin entries
        ItemStack tornPages =
                TornPagesItem.create(
                        planetId,
                        startingKnowledge
                );


        //Gives torn pages to player
        boolean added =
                player.getInventory().add(
                        tornPages
                );

        if (!added) {

            player.drop(
                    tornPages,
                    false
            );
        }

        return true;
    }
}