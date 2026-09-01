package net.rovalio.scadrialmod.power;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.registry.ScadrialOrigins;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ScadrialPowerAssigner {

    /*
     * 4096 = 16³. (Soy un genio de las matemáticas
     *
     * Every probability is represented as an integer weight over this common scale.
     */
    private static final int
            PROBABILITY_SCALE = 4_096;

    ///Noble distribution.
    private static final int
            NOBLE_MISTING_WEIGHT = 3_072;

    private static final int
            NOBLE_MISTBORN_WEIGHT = 192;

    private static final int
            NOBLE_FERRING_WEIGHT = 64;

    ///Skaa distribution.

    /*
     * Esto seguramente haya que balancearlo porque aunque la prob de los skaa de tener poderes es 50/50
     * hay casos muy extraños. Un Fullborn tiene prob de +- 0,02%
     *
     * ¿Debería balancearlo?
     *
     * Como límite, la probabilidad de obtener poderes de un skaa no debería pasar el 64%
     * Si, de nuevo referenciando al número 16. Los cálculos son en base 16.
     *
     * Si Mistborn es 1/16 Misting y Ferring 1/16 Feruchemist
     * ¿Fullborn 1/16 twinborn?
     *
     * Hay que rehacer estos calculos, creo que he calculado mal porcentajes
     */

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

    ///Terris distribution.
    private static final int
            TERRIS_FERRING_WEIGHT = 3_072;

    private static final int
            TERRIS_FERUCHEMIST_WEIGHT = 192;

    private static final int
            TERRIS_MISTING_WEIGHT = 64;

    /*
     * One in 256 ordinary Misting results receives a special metal.
     * 16 / 4096 = 1 / 256.
     */

    /*
     * En verdad tendría que revisar la teoría esa de que los alomantes de atium son en verdad alomantes
     * de electrum. No sé si Sanderson llegó a confirmar algo.
     *
     * ¿Alomantes de oro deberían poder quemar malatium?
     */
    private static final int
            SPECIAL_MISTING_METAL_WEIGHT = 16;

    private static final List<MetalType>
            STANDARD_METALS =
            Arrays.stream(MetalType.values())

                    .filter(
                            MetalType::isStandardMetal
                    )

                    .toList();

    private static final List<MetalType>
            SPECIAL_METALS =
            Arrays.stream(MetalType.values())

                    .filter(metal ->
                            !metal.isStandardMetal()
                    )

                    .toList();

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
                && (originId.equals(
                ScadrialOrigins.NOBLE.getId()
        )
                || originId.equals(
                ScadrialOrigins.SKAA.getId()
        )
                || originId.equals(
                ScadrialOrigins.TERRIS.getId()
        ));
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

        // Prevents rerolling powers through login, death or repeated initialization.
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
                        selectMistingMetal(random);
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

                /*
                 * Natural Twinborn generated here use
                 * only the standard sixteen metals.
                 */
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

        //Onboarding and rerolls assign latent potential. They never perform the Snapping itself.
        ScadrialPowerManager.configurePowers(
                player,
                allomanticProfile,
                allomanticMetal,
                false,
                feruchemicalProfile,
                feruchemicalMetal
        );
    }

    private static MetalType selectMistingMetal(
            RandomSource random
    ) {
        boolean selectSpecialMetal =
                !SPECIAL_METALS.isEmpty()
                        && random.nextInt(
                        PROBABILITY_SCALE
                ) < SPECIAL_MISTING_METAL_WEIGHT;

        if (selectSpecialMetal) {
            return selectRandomMetal(
                    random,
                    SPECIAL_METALS
            );
        }

        return selectStandardMetal(random);
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
