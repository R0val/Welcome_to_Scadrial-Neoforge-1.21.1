package net.rovalio.scadrialmod.knowledge;

import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;

import java.util.List;

public final class ScadrialStartingKnowledge {

    //General Scadrian knowledge

    public static final ResourceLocation FINAL_EMPIRE =
            id("final_empire");

    public static final ResourceLocation ASHFALLS =
            id("ashfalls");

    public static final ResourceLocation LORD_RULER_1 =
            id("lord_ruler_1");


    //Noble origin knowledge

    public static final ResourceLocation LUTHADEL =
            id("luthadel");

    public static final ResourceLocation NOBLE_SOCIETY =
            id("noble_society");

    public static final ResourceLocation BASIC_ALLOMANCY =
            id("basic_allomancy");


    //Terris origin knowledge

    public static final ResourceLocation TERRIS =
            id("terris");

    public static final ResourceLocation TERRIS_CULTURE =
            id("terris_culture");

    public static final ResourceLocation FERUCHEMY =
            id("feruchemy");

    public static final ResourceLocation HERO_OF_AGES_PROPHECY =
            id("hero_of_ages_prophecy");

    //Skaa origin knowledge

    public static final ResourceLocation SKAA_LIFE =
            id("skaa_life");

    public static final ResourceLocation PLANTATIONS =
            id("plantations");

    public static final ResourceLocation HEMALURGIC_CREATURE_RUMORS =
            id("hemalurgic_creature_rumors");


    private ScadrialStartingKnowledge() {
    }


    //Starting knowledge

    public static List<ResourceLocation> getForOrigin(
            ResourceLocation originId
    ) {

        if (originId.equals(
                ScadrialOrigins.NOBLE.getId()
        )) {

            return List.of(
                    FINAL_EMPIRE,
                    LUTHADEL,
                    NOBLE_SOCIETY,
                    BASIC_ALLOMANCY
            );
        }

        if (originId.equals(
                ScadrialOrigins.TERRIS.getId()
        )) {

            return List.of(
                    FINAL_EMPIRE,
                    TERRIS,
                    TERRIS_CULTURE,
                    FERUCHEMY,
                    HERO_OF_AGES_PROPHECY
            );
        }

        if (originId.equals(
                ScadrialOrigins.SKAA.getId()
        )) {

            return List.of(
                    FINAL_EMPIRE,
                    ASHFALLS,
                    LORD_RULER_1,
                    SKAA_LIFE,
                    PLANTATIONS,
                    HEMALURGIC_CREATURE_RUMORS
            );
        }

        return List.of();
    }

    private static ResourceLocation id(
            String path
    ) {

        return ResourceLocation.fromNamespaceAndPath(
                ScadrialMod.MOD_ID,
                path
        );
    }
}