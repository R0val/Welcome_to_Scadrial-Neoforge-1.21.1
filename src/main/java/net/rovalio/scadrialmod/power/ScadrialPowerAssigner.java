package net.rovalio.scadrialmod.power;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;

import java.util.List;
import java.util.Objects;

public final class ScadrialPowerAssigner {

    /*
     * 4096 = 16³.
     *
     * Every probability is represented as an
     * integer weight over this common scale.
     */
    private static final int
            PROBABILITY_SCALE = 4_096;

    /*
     * Temporary strength used during Allomancy 0.
     *
     * It represents the Elend-level reference used
     * to calibrate the first physical mechanics.
     */
    private static final double
            TEST_ALLOMANCY_STRENGTH = 16.0;

    // Noble distribution.

    private static final int
            NOBLE_MISTING_WEIGHT = 3_072;

    private static final int
            NOBLE_MISTBORN_WEIGHT = 192;

    private static final int
            NOBLE_FERRING_WEIGHT = 64;

    // Skaa distribution.

    private static final int
            SKAA_MISTING_WEIGHT = 1_024;

    private static final int
            SKAA_MISTBORN_WEIGHT = 64;

    private static final int
            SKAA_FERRING_WEIGHT = 768;

    private static final int
            SKAA_FERUCHEMIST_WEIGHT = 48;

    private static final int
            SKAA_TWINBORN_WEIGHT = 128;

    private static final int
            SKAA_FULLBORN_WEIGHT = 8;

    // Terris distribution.

    private static final int
            TERRIS_FERRING_WEIGHT = 3_072;

    private static final int
            TERRIS_FERUCHEMIST_WEIGHT = 192;

    private static final int
            TERRIS_MISTING_WEIGHT = 64;

    private static final List<MetalType>
            STANDARD_METALS =
            List.of(MetalType.values());

    private ScadrialPowerAssigner() {
    }

    private enum AssignmentOutcome {
        NONE,
        MISTING,
        MISTBORN,
        FERRING,
        FERUCHEMIST,
        TWINBORN,
        FULLBORN
    }

    public static boolean supportsOrigin(
            ResourceLocation originId
    ) {
        return originId != null
                && (
                originId.equals(
                        ScadrialOrigins.NOBLE.getId()
                )
                        || originId.equals(
                        ScadrialOrigins.SKAA.getId()
                )
                        || originId.equals(
                        ScadrialOrigins.TERRIS.getId()
                )
        );
    }

    public static boolean assignInitialPowers(
            ServerPlayer player,
            ResourceLocation originId
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        requireSupportedOrigin(originId);

        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        /*
         * Prevents rerolling powers through login,
         * death or repeated initialization.
         */
        if (data.isPowerAssignmentComplete()) {
            return false;
        }

        RandomSource random =
                player.getRandom();

        AssignmentOutcome outcome =
                selectOutcome(
                        originId,
                        random
                );

        applyOutcome(
                player,
                originId,
                random,
                outcome
        );

        return true;
    }

    public static boolean rerollPowers(
            ServerPlayer player,
            ResourceLocation originId
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        requireSupportedOrigin(originId);

        RandomSource random =
                player.getRandom();

        AssignmentOutcome outcome =
                selectOutcome(
                        originId,
                        random
                );

        applyOutcome(
                player,
                originId,
                random,
                outcome
        );

        return true;
    }

    private static ResourceLocation requireSupportedOrigin(
            ResourceLocation originId
    ) {
        Objects.requireNonNull(
                originId,
                "Origin ID cannot be null"
        );

        if (!supportsOrigin(originId)) {
            throw new IllegalArgumentException(
                    "Unsupported Scadrial origin: "
                            + originId
            );
        }

        return originId;
    }

    private static AssignmentOutcome selectOutcome(
            ResourceLocation originId,
            RandomSource random
    ) {
        if (originId.equals(
                ScadrialOrigins.NOBLE.getId()
        )) {
            return rollNobleOutcome(random);
        }

        if (originId.equals(
                ScadrialOrigins.SKAA.getId()
        )) {
            return rollSkaaOutcome(random);
        }

        if (originId.equals(
                ScadrialOrigins.TERRIS.getId()
        )) {
            return rollTerrisOutcome(random);
        }

        throw new IllegalArgumentException(
                "Unsupported Scadrial origin: "
                        + originId
        );
    }

    private static AssignmentOutcome rollNobleOutcome(
            RandomSource random
    ) {
        int roll =
                random.nextInt(
                        PROBABILITY_SCALE
                );

        int threshold =
                NOBLE_MISTING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.MISTING;
        }

        threshold +=
                NOBLE_MISTBORN_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.MISTBORN;
        }

        threshold +=
                NOBLE_FERRING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FERRING;
        }

        return AssignmentOutcome.NONE;
    }

    private static AssignmentOutcome rollSkaaOutcome(
            RandomSource random
    ) {
        int roll =
                random.nextInt(
                        PROBABILITY_SCALE
                );

        int threshold =
                SKAA_MISTING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.MISTING;
        }

        threshold +=
                SKAA_MISTBORN_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.MISTBORN;
        }

        threshold +=
                SKAA_FERRING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FERRING;
        }

        threshold +=
                SKAA_FERUCHEMIST_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FERUCHEMIST;
        }

        threshold +=
                SKAA_TWINBORN_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.TWINBORN;
        }

        threshold +=
                SKAA_FULLBORN_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FULLBORN;
        }

        return AssignmentOutcome.NONE;
    }

    private static AssignmentOutcome rollTerrisOutcome(
            RandomSource random
    ) {
        int roll =
                random.nextInt(
                        PROBABILITY_SCALE
                );

        int threshold =
                TERRIS_FERRING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FERRING;
        }

        threshold +=
                TERRIS_FERUCHEMIST_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.FERUCHEMIST;
        }

        threshold +=
                TERRIS_MISTING_WEIGHT;

        if (roll < threshold) {
            return AssignmentOutcome.MISTING;
        }

        return AssignmentOutcome.NONE;
    }

    private static void applyOutcome(
            ServerPlayer player,
            ResourceLocation originId,
            RandomSource random,
            AssignmentOutcome outcome
    ) {
        PowerProfile allomanticProfile =
                PowerProfile.NONE;

        MetalType allomanticMetal = null;

        PowerProfile feruchemicalProfile =
                PowerProfile.NONE;

        MetalType feruchemicalMetal = null;

        switch (outcome) {

            case NONE -> {
            }

            case MISTING -> {
                allomanticProfile =
                        PowerProfile.SINGLE;

                allomanticMetal =
                        selectStandardMetal(random);
            }

            case MISTBORN ->
                    allomanticProfile =
                            PowerProfile.FULL;

            case FERRING -> {
                feruchemicalProfile =
                        PowerProfile.SINGLE;

                feruchemicalMetal =
                        selectStandardMetal(random);
            }

            case FERUCHEMIST ->
                    feruchemicalProfile =
                            PowerProfile.FULL;

            case TWINBORN -> {
                allomanticProfile =
                        PowerProfile.SINGLE;

                feruchemicalProfile =
                        PowerProfile.SINGLE;

                allomanticMetal =
                        selectStandardMetal(random);

                feruchemicalMetal =
                        selectStandardMetal(random);
            }

            case FULLBORN -> {
                allomanticProfile =
                        PowerProfile.FULL;

                feruchemicalProfile =
                        PowerProfile.FULL;
            }
        }

        double allomancyStrength =
                rollAllomancyStrength(
                        originId,
                        random,
                        allomanticProfile
                );

        /*
         * Onboarding and rerolls assign latent
         * potential. They never perform the
         * Snapping itself.
         */
        ScadrialPowerManager.configurePowers(
                player,
                allomanticProfile,
                allomanticMetal,
                false,
                allomancyStrength,
                feruchemicalProfile,
                feruchemicalMetal
        );
    }

    private static double rollAllomancyStrength(
            ResourceLocation originId,
            RandomSource random,
            PowerProfile allomanticProfile
    ) {
        Objects.requireNonNull(
                originId,
                "Origin ID cannot be null"
        );

        Objects.requireNonNull(
                random,
                "Random source cannot be null"
        );

        Objects.requireNonNull(
                allomanticProfile,
                "Allomantic profile cannot be null"
        );

        if (allomanticProfile
                == PowerProfile.NONE) {

            return 0.0;
        }

        /*
         * Provisional configuration for Allomancy 0.
         *
         * The origin and random source are accepted
         * now so this method can later introduce
         * origin-dependent distributions without
         * changing the assignment flow.
         */
        return TEST_ALLOMANCY_STRENGTH;
    }

    private static MetalType selectStandardMetal(
            RandomSource random
    ) {
        return selectRandomMetal(
                random,
                STANDARD_METALS
        );
    }

    private static MetalType selectRandomMetal(
            RandomSource random,
            List<MetalType> metals
    ) {
        if (metals.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot select from an empty "
                            + "metal collection"
            );
        }

        return metals.get(
                random.nextInt(
                        metals.size()
                )
        );
    }
}