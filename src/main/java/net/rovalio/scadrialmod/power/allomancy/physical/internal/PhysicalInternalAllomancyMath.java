package net.rovalio.scadrialmod.power.allomancy.physical.internal;

public final class PhysicalInternalAllomancyMath {

    public static final int RECOVERY_DELAY_TICKS = 100;

    public static final double MIN_RECOVERY_PER_SECOND = 0.02;
    public static final double RECOVERY_PER_STRENGTH_UNIT = 0.18;

    public static final double MAX_PEWTER_DEBT = 64.0;
    public static final double DECAY_START_DEBT = 20.0;
    public static final double MIN_ABSORPTION = 1.0 / 16.0;

    private static final int[] ABSORPTION = {
            0,
            1, 1,
            2, 2,
            3, 3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            14
    };

    private PhysicalInternalAllomancyMath() {
    }

    public static double unit(double strength) {
        return Double.isFinite(strength)
                ? Math.max(0.0, Math.min(4.0, strength / 16.0))
                : 0.0;
    }

    public static double absorption(double strength) {
        if (unit(strength) == 0.0) {
            return 0.0;
        }

        if (strength <= 16.0) {
            int rank = Math.max(1, (int) Math.floor(strength));
            return ABSORPTION[rank] / 16.0;
        }

        // Duralumin approaches 15/16 without absorbing all damage.
        return (15.0 - Math.exp(-(strength - 16.0) / 16.0)) / 16.0;
    }

    public static double deferredDamage(
            double currentDebt,
            double incomingDamage,
            double strength
    ) {
        if (!Double.isFinite(currentDebt)
                || !Double.isFinite(incomingDamage)
                || incomingDamage <= 0.0) {
            return 0.0;
        }

        double fullEfficiency = absorption(strength);

        if (fullEfficiency <= 0.0) {
            return 0.0;
        }

        double startingDebt = Math.max(
                0.0,
                Math.min(MAX_PEWTER_DEBT, currentDebt)
        );

        if (startingDebt >= MAX_PEWTER_DEBT) {
            return 0.0;
        }

        double debt = startingDebt;
        double remainingDamage = incomingDamage;

        // Absorción normal hasta alcanzar 20 puntos de deuda.
        if (debt < DECAY_START_DEBT) {
            double gained = Math.min(
                    DECAY_START_DEBT - debt,
                    remainingDamage * fullEfficiency
            );

            debt += gained;
            remainingDamage = Math.max(
                    0.0,
                    remainingDamage - gained / fullEfficiency
            );
        }

        // La eficiencia baja conforme crece la deuda.
        double decayEndDebt = decayEndDebt(strength);

        if (remainingDamage > 0.0
                && fullEfficiency > MIN_ABSORPTION
                && debt < decayEndDebt) {

            double slope =
                    (fullEfficiency - MIN_ABSORPTION)
                            / (decayEndDebt - DECAY_START_DEBT);

            double currentEfficiency =
                    fullEfficiency
                            - slope * (debt - DECAY_START_DEBT);

            double damageToMinimum =
                    Math.log(currentEfficiency / MIN_ABSORPTION)
                            / slope;

            if (remainingDamage < damageToMinimum) {
                debt += currentEfficiency
                        * -Math.expm1(-slope * remainingDamage)
                        / slope;

                remainingDamage = 0.0;
            } else {
                debt = decayEndDebt;
                remainingDamage -= damageToMinimum;
            }
        }

        // Al final de la curva solo se absorbe 1/16, hasta llenar la deuda.
        if (remainingDamage > 0.0) {
            debt += Math.min(
                    MAX_PEWTER_DEBT - debt,
                    remainingDamage * MIN_ABSORPTION
            );
        }

        return Math.max(
                0.0,
                Math.min(
                        MAX_PEWTER_DEBT - startingDebt,
                        debt - startingDebt
                )
        );
    }

    private static double decayEndDebt(double strength) {
        double normal = Math.min(
                1.0,
                Math.max(0.0, strength / 16.0)
        );

        double duralumin = Math.min(
                1.0,
                Math.max(0.0, (strength - 16.0) / 48.0)
        );

        return DECAY_START_DEBT
                + 32.0 * normal
                + 4.0 * duralumin;
    }

    public static double recoveryPerSecond(double strength) {
        if (unit(strength) == 0.0) {
            return 0.0;
        }

        return MIN_RECOVERY_PER_SECOND
                + RECOVERY_PER_STRENGTH_UNIT * unit(strength);
    }

    public static double immediateReduction(double strength) {
        return Math.min(0.5, 0.2 * unit(strength));
    }

    public static double explosionReduction(double strength) {
        return Math.min(0.8, 0.5 * unit(strength));
    }

    public static double pushResistance(double strength) {
        return Math.min(0.8, 0.5 * unit(strength));
    }

    public static double soundGain(double strength) {
        return 1.0 + 0.75 * unit(strength);
    }

    public static double hearingRange(double strength) {
        return 1.0 + 3.0 * unit(strength);
    }

    public static double zoom(double strength) {
        return 1.0 + 3.0 * unit(strength);
    }

    public static double nightVision(double strength) {
        return Math.min(0.45, 0.18 * unit(strength));
    }

    public static double mistVisibility(double strength) {
        return 1.0 - 0.95 * Math.min(1.0, unit(strength));
    }
}