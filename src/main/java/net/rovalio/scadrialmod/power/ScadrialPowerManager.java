package net.rovalio.scadrialmod.power;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.CosmereAPI.player.CosmereAttachments;
import net.rovalio.CosmereAPI.player.SpiritwebData;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.network.ScadrialNetworking;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.registry.ScadrialInvestedArts;

import java.util.Objects;

public final class ScadrialPowerManager {

    /*
     * Temporary value used by administrative grants
     * and migrations during Allomancy 0.
     */
    private static final double
            DEFAULT_GRANTED_ALLOMANCY_STRENGTH =
            16.0;

    /*
     * Permanent bonus per SINGLE profile,
     * not per Scadrian origin.
     */
    private static final double
            SCADRIAL_INVESTITURE_BONUS_BEU =
            1.0 / 16.0;

    private ScadrialPowerManager() {
    }

    static void configurePowers(
            ServerPlayer player,
            PowerProfile allomanticProfile,
            MetalType allomanticMetal,
            boolean allomancySnapped,
            double allomancyStrength,
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

        validateAllomancyStrength(
                allomanticProfile,
                allomancyStrength
        );

        if (allomancySnapped
                && allomanticProfile
                == PowerProfile.NONE) {

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

        /*
         * The profile must be configured before
         * ScadrialPlayerData validates its strength.
         */
        data.setAllomancyStrength(
                allomancyStrength
        );

        data.configureFeruchemy(
                feruchemicalProfile,
                feruchemicalMetal
        );

        /*
         * Replace the previous profile contribution
         * with the new one. Repeated grants therefore
         * do not accumulate permanent Investiture.
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
        ScadrialNetworking.sync(player);
    }

    /**
     * Checks whether the character possesses the
     * power required by a specific fuel.
     *
     * Latent Allomancy satisfies this check.
     */
    public static boolean hasAllomanticAccess(
            ScadrialPlayerData data,
            AllomanticFuel fuel
    ) {
        Objects.requireNonNull(
                data,
                "Scadrial data cannot be null"
        );

        Objects.requireNonNull(
                fuel,
                "Allomantic fuel cannot be null"
        );

        return data.getAllomanticMetals()
                .contains(
                        fuel.getRequiredPower()
                );
    }

    /**
     * Checks power compatibility and the current
     * Snapping requirement.
     *
     * This does not yet check reserves, effect
     * implementation or other burning conditions.
     */
    public static boolean canUseAllomanticFuel(
            ServerPlayer player,
            AllomanticFuel fuel
    ) {
        Objects.requireNonNull(
                fuel,
                "Allomantic fuel cannot be null"
        );

        ScadrialPlayerData data =
                getData(player);

        return data.canUseAllomancy()
                && hasAllomanticAccess(
                data,
                fuel
        );
    }

    public static void setAllomancyStrength(
            ServerPlayer player,
            double strength
    ) {
        ScadrialPlayerData data = getData(player);
        data.setAllomancyStrength(strength);
        ScadrialNetworking.sync(player);
    }

    /**
     * Resolves legacy data that already contains a
     * completed power assignment but no strength.
     *
     * The method is idempotent and must run on the
     * logical server.
     */
    public static boolean
    initializeMissingAllomancyStrength(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        if (!data.isPowerAssignmentComplete()
                || data.isAllomancyStrengthInitialized()) {

            return false;
        }

        double migratedStrength =
                data.hasAllomancy()
                        ? DEFAULT_GRANTED_ALLOMANCY_STRENGTH
                        : 0.0;

        data.setAllomancyStrength(
                migratedStrength
        );

        return true;
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
            ScadrialNetworking.sync(player);
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

        double strength =
                resolveAllomancyStrength(
                        data,
                        PowerProfile.SINGLE
                );

        configurePowers(
                player,
                PowerProfile.SINGLE,
                metal,
                preserveSnapping,
                strength,
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

        double strength =
                resolveAllomancyStrength(
                        data,
                        PowerProfile.FULL
                );

        configurePowers(
                player,
                PowerProfile.FULL,
                null,
                preserveSnapping,
                strength,
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
                0.0,
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

        double strength =
                resolveAllomancyStrength(
                        data,
                        data.getAllomanticProfile()
                );

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                strength,
                PowerProfile.SINGLE,
                metal
        );
    }

    public static void grantFeruchemist(
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                getData(player);

        double strength =
                resolveAllomancyStrength(
                        data,
                        data.getAllomanticProfile()
                );

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                strength,
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

        double strength =
                resolveAllomancyStrength(
                        data,
                        data.getAllomanticProfile()
                );

        configurePowers(
                player,
                data.getAllomanticProfile(),
                data.getAllomanticMetal()
                        .orElse(null),
                data.isAllomancySnapped(),
                strength,
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
        ScadrialNetworking.sync(player);
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

        /*
         * Handles old completed profiles once.
         * New unassigned players remain uninitialized.
         */
        initializeMissingAllomancyStrength(
                player
        );

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
         * ScadrialPlayerData does not yet store
         * Hemalurgic spikes or stolen powers.
         */
        InvestedArtConnectionManager.reconcile(
                scadrialData,
                spiritweb
        );
    }

    private static double resolveAllomancyStrength(
            ScadrialPlayerData data,
            PowerProfile targetProfile
    ) {
        Objects.requireNonNull(
                data,
                "Scadrial data cannot be null"
        );

        Objects.requireNonNull(
                targetProfile,
                "Target Allomantic profile cannot be null"
        );

        if (targetProfile == PowerProfile.NONE) {
            return 0.0;
        }

        if (data.hasActiveAllomancyStrength()) {
            return data.getAllomancyStrength();
        }

        return DEFAULT_GRANTED_ALLOMANCY_STRENGTH;
    }

    private static void validateAllomancyStrength(
            PowerProfile profile,
            double strength
    ) {
        Objects.requireNonNull(
                profile,
                "Allomantic profile cannot be null"
        );

        if (!Double.isFinite(strength)
                || strength < 0.0) {

            throw new IllegalArgumentException(
                    "Allomancy strength must be finite "
                            + "and greater than or equal to 0"
            );
        }

        if (profile == PowerProfile.NONE
                && strength != 0.0) {

            throw new IllegalArgumentException(
                    "A profile without Allomancy "
                            + "must have strength 0"
            );
        }

        if (profile != PowerProfile.NONE
                && strength <= 0.0) {

            throw new IllegalArgumentException(
                    "An Allomantic profile requires "
                            + "positive Allomancy strength"
            );
        }
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

    public static double getInvestitureBonusBEU(
            ScadrialPlayerData data
    ) {
        Objects.requireNonNull(
                data,
                "Scadrial data cannot be null"
        );

        return SCADRIAL_INVESTITURE_BONUS_BEU * (
                getProfileWeight(
                        data.getAllomanticProfile()
                )
                        + getProfileWeight(
                        data.getFeruchemicalProfile()
                )
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
