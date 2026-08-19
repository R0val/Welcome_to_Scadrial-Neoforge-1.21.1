package net.rovalio.scadrialmod.knowledge;

import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;

import java.util.List;
import java.util.Map;

import static net.rovalio.scadrialmod.knowledge.ScadrialKnowledge.*;

public final class ScadrialStartingKnowledge {

    /*
     * Assigns entries from ScadrialKnowledge
     * to each selectable Scadrial origin.
     */
    private static final Map<
            ResourceLocation,
            List<ResourceLocation>
            > KNOWLEDGE_BY_ORIGIN = Map.of(

            ScadrialOrigins.NOBLE.getId(),
            List.of(
                    FINAL_EMPIRE,
                    NOBLE_SOCIETY,
                    ALLOMANCY
            ),

            ScadrialOrigins.TERRIS.getId(),
            List.of(
                    TERRIS,
                    FERUCHEMY,
                    HOA_PROPHECY
            ),

            ScadrialOrigins.SKAA.getId(),
            List.of(
                    ASHFALL,
                    LORD_RULER_RUMORS,
                    ALLOMANCY_RUMORS
            )
    );

    private ScadrialStartingKnowledge() {
    }

    public static List<ResourceLocation> getForOrigin(
            ResourceLocation originId
    ) {
        if (originId == null) {
            return List.of();
        }

        return KNOWLEDGE_BY_ORIGIN.getOrDefault(
                originId,
                List.of()
        );
    }
}