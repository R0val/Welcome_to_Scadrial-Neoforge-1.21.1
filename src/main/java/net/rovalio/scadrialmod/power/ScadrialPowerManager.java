package net.rovalio.scadrialmod.power;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.CosmereAPI.player.CosmereAttachments;
import net.rovalio.CosmereAPI.player.SpiritwebData;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.registry.ScadrialInvestedArts;

import java.util.Objects;

public final class ScadrialPowerManager {

    private ScadrialPowerManager() {
    }

    static void configurePowers(
            ServerPlayer player,
            PowerProfile allomanticProfile,
            MetalType allomanticMetal,
            boolean allomancySnapped,
            PowerProfile feruchemicalProfile,
            MetalType feruchemicalMetal
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        validateProfile(
                "Allomantic",
                allomanticProfile,
                allomanticMetal
        );

        validateProfile(
                "Feruchemical",
                feruchemicalProfile,
                feruchemicalMetal
        );

        if (allomancySnapped
                && allomanticProfile == PowerProfile.NONE) {

            throw new IllegalArgumentException(
                    "A player without an Allomantic "
                            + "profile cannot be Snapped"
            );
        }

        double previousInvestitureBonus =
                getInvestitureBonusBEU(data);

        SpiritwebData spiritweb =
                CosmereAttachments.get(player)
                        .getSpiritweb();

        if (data.isAllomancySnapped()
                && !allomancySnapped) {

            restoreRecordedSnappingIntegrity(
                    player,
                    data
            );
        }

        data.configureAllomancy(
                allomanticProfile,
                allomanticMetal,
                allomancySnapped
        );

        data.configureFeruchemy(
                feruchemicalProfile,
                feruchemicalMetal
        );

        /*
         * Replace the old profile contribution with the new one.
         * Repeated grants therefore do not accumulate Investiture.
         */
        double investitureDelta =
                getInvestitureBonusBEU(data)
                        - previousInvestitureBonus;

        spiritweb.setInvestitureBEU(
                spiritweb.getInvestitureBEU()
                        + investitureDelta
        );

        reconcile(player);
        data.completePowerAssignment();
    }

    public static boolean snapAllomancy(
            ServerPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        boolean snapped =
                AllomancySnappingManager.snap(
                        player
                );

        if (snapped) {
            reconcile(player);
        }

        return snapped;
    }

    public static void grantMisting(
            ServerPlayer player,
            MetalType metal
    ) {
        Objects.requireNonNull(
                metal,
                "Allomantic metal cannot be null"
        );

        ScadrialPlayerData data =
                getData(player);

        boolean preserveSnapping =
                data.isAllomancySnapped()
                        && data.isMisting()
                        && data.getAllomanticMetal()
                        .filter(metal::equals)
                        .isPresent();

        configurePowers(
                player,
                PowerProfile.SINGLE,
                metal,
                preserveSnapping,
                data.getFeruchemicalProfile(),
                data.getFeruchemicalMetal()
                        .orElse(null)
        );
    }

    public static void grantMistborn(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        boolean preserveSnapping =
                data.isAllomancySnapped()
                        && data.isMistborn();

        configurePowers(
                player,
                PowerProfile.FULL,
                null,
                preserveSnapping,
                data.getFeruchemicalProfile(),
                data.getFeruchemicalMetal()
                        .orElse(null)
        );
    }

    public static boolean revokeAllomancy(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        if (!data.hasAllomancy()) {
            return false;
        }

        configurePowers(
                player,
                PowerProfile.NONE,
                null,
                false,
                data.getFeruchemicalProfile(),
                data.getFeruchemicalMetal()
                        .orElse(null)
        );

        return true;
    }

    public static void grantFerring(
            ServerPlayer player,
            MetalType metal
    ) {
        Objects.requireNonNull(
                metal,
                "Feruchemical metal cannot be null"
        );

        ScadrialPlayerData data =
                getData(player);

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                PowerProfile.SINGLE,
                metal
        );
    }

    public static void grantFeruchemist(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                PowerProfile.FULL,
                null
        );
    }

    public static boolean revokeFeruchemy(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        if (!data.hasFeruchemy()) {
            return false;
        }

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                PowerProfile.NONE,
                null
        );

        return true;
    }

    public static void reset(
            ServerPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        double previousInvestitureBonus =
                getInvestitureBonusBEU(data);

        SpiritwebData spiritweb =
                CosmereAttachments.get(player)
                        .getSpiritweb();

        restoreRecordedSnappingIntegrity(
                player,
                data
        );

        data.reset();

        spiritweb.setInvestitureBEU(
                spiritweb.getInvestitureBEU()
                        - previousInvestitureBonus
        );

        reconcile(player);
    }

    public static void reconcile(
            ServerPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        ScadrialPlayerData scadrialData =
                ScadrialAttachments.get(player);

        SpiritwebData spiritweb =
                CosmereAttachments.get(player)
                        .getSpiritweb();

        reconcileInvestedArt(
                spiritweb,
                ScadrialInvestedArts
                        .ALLOMANCY
                        .getId(),
                scadrialData.hasAllomancy()
        );

        reconcileInvestedArt(
                spiritweb,
                ScadrialInvestedArts
                        .FERUCHEMY
                        .getId(),
                scadrialData.hasFeruchemy()
        );

        /*
         * Hemalurgy is deliberately untouched.
         * ScadrialPlayerData does not yet store Hemalurgic spikes or stolen powers.
         */

        InvestedArtConnectionManager.reconcile(
                scadrialData,
                spiritweb
        );
    }

    private static void restoreRecordedSnappingIntegrity(
            ServerPlayer player,
            ScadrialPlayerData data
    ) {
        double snappingDamage =
                data.getAllomancySnappingDamage();

        if (!Double.isFinite(snappingDamage)
                || snappingDamage <= 0.0) {

            return;
        }

        SpiritwebData spiritweb =
                CosmereAttachments.get(player)
                        .getSpiritweb();

        spiritweb.setIntegrity(
                spiritweb.getIntegrity()
                        + snappingDamage
        );
    }

    private static void reconcileInvestedArt(
            SpiritwebData spiritweb,
            ResourceLocation investedArtId,
            boolean shouldPossess
    ) {
        if (shouldPossess) {

            if (!spiritweb.hasInvestedArt(
                    investedArtId
            )) {
                spiritweb.grantInvestedArt(
                        investedArtId
                );
            }

            return;
        }

        if (spiritweb.hasInvestedArt(
                investedArtId
        )) {
            spiritweb.revokeInvestedArt(
                    investedArtId
            );
        }
    }

    private static ScadrialPlayerData getData(
            ServerPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        return ScadrialAttachments.get(player);
    }

    private static void validateProfile(
            String profileName,
            PowerProfile profile,
            MetalType metal
    ) {
        Objects.requireNonNull(
                profile,
                profileName
                        + " profile cannot be null"
        );

        if (profile == PowerProfile.SINGLE
                && metal == null) {

            throw new IllegalArgumentException(
                    profileName
                            + " SINGLE profile "
                            + "requires a metal"
            );
        }

        if (profile != PowerProfile.SINGLE
                && metal != null) {

            throw new IllegalArgumentException(
                    profileName
                            + " "
                            + profile
                            + " profile cannot store "
                            + "a single metal"
            );
        }
    }

    // Permanent bonus per SINGLE profile, not per Scadrian origin.
    private static final double
            SCADRIAL_INVESTITURE_BONUS_BEU = 1.0 / 16.0;

    public static double getInvestitureBonusBEU(
            ScadrialPlayerData data
    ) {
        Objects.requireNonNull(
                data,
                "Scadrial data cannot be null"
        );

        return SCADRIAL_INVESTITURE_BONUS_BEU * (
                getProfileWeight(data.getAllomanticProfile())
                        + getProfileWeight(data.getFeruchemicalProfile())
        );
    }

    private static int getProfileWeight(
            PowerProfile profile
    ) {
        return switch (profile) {
            case NONE -> 0;
            case SINGLE -> 1;
            case FULL -> 16;
        };
    }
}
