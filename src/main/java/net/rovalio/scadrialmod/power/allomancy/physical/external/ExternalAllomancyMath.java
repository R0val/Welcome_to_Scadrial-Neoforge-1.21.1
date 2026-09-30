package net.rovalio.scadrialmod.power.allomancy.physical.external;

public final class ExternalAllomancyMath {

    public static final double REFERENCE_STRENGTH = 16.0;
    public static final double REFERENCE_RADIUS = 64.0;
    public static final double MAX_RADIUS = 128.0;
    public static final double MAX_DURALUMIN_RADIUS = 256.0;
    public static final double DURALUMIN_EXTERNAL_MULTIPLIER = 4.0;

    public static final double REFERENCE_FORCE = 48.0;
    public static final double REFERENCE_ACCELERATION_LIMIT = 0.8;
    public static final double REFERENCE_SPEED_LIMIT = 3.0;

    private static final double MAX_ACCELERATION = 8.0;
    private static final double MAX_POWERED_SPEED = 8.0;
    private static final double MAX_STRENGTH_FACTOR = 1.0E6;

    public static final double PROJECTILE_SPEED_MULTIPLIER = 4.0;
    public static final double PROJECTILE_ACCELERATION_MULTIPLIER = 4.0;

    private ExternalAllomancyMath() {
    }

    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double strengthFactor(double strength) {
        if (!Double.isFinite(strength) || strength <= 0.0) {
            return 0.0;
        }

        return Math.min(
                MAX_STRENGTH_FACTOR,
                strength / REFERENCE_STRENGTH
        );
    }

    public static double force(double strength) {
        return REFERENCE_FORCE * strengthFactor(strength);
    }

    /** La strength recibida ya incluye el aumento general del duraluminio. */
    public static double externalStrength(double strength, boolean duralumin) {
        if (!Double.isFinite(strength) || strength <= 0.0) {
            return 0.0;
        }

        double multiplier = duralumin ? DURALUMIN_EXTERNAL_MULTIPLIER : 1.0;
        return Math.min(
                REFERENCE_STRENGTH * MAX_STRENGTH_FACTOR,
                strength * multiplier
        );
    }

    public static double accelerationLimit(double strength) {
        return Math.min(
                MAX_ACCELERATION,
                REFERENCE_ACCELERATION_LIMIT * strengthFactor(strength)
        );
    }

    public static double poweredSpeedLimit(double strength) {
        return Math.min(
                MAX_POWERED_SPEED,
                REFERENCE_SPEED_LIMIT * Math.sqrt(strengthFactor(strength))
        );
    }

    public static double radius(double strength) {
        return radius(strength, false);
    }

    public static double radius(double strength, boolean duralumin) {
        double boostedStrength = externalStrength(strength, duralumin);
        double maximum = duralumin ? MAX_DURALUMIN_RADIUS : MAX_RADIUS;

        return Math.min(
                maximum,
                REFERENCE_RADIUS * Math.sqrt(strengthFactor(boostedStrength))
        );
    }

    public static double ramp(double heldTicks, double rampTicks) {
        if (!Double.isFinite(heldTicks)
                || !Double.isFinite(rampTicks)
                || rampTicks <= 0.0) {
            return 0.0;
        }

        double progress = clamp01(heldTicks / rampTicks);
        return progress * progress * (3.0 - 2.0 * progress);
    }

    public static double rangeFactor(double distance, double radius) {
        if (!Double.isFinite(distance)
                || !Double.isFinite(radius)
                || distance < 0.0
                || radius <= 0.0
                || distance >= radius) {
            return 0.0;
        }

        double remaining = clamp01((radius - distance) / (0.25 * radius));
        double smooth = remaining * remaining * (3.0 - 2.0 * remaining);

        return Math.log1p(4.0 * smooth) / Math.log(5.0);
    }

    public static double resistanceFactor(
            double strength,
            double charge,
            double resistanceScale
    ) {
        double power = strengthFactor(strength);

        if (power <= 0.0
                || !Double.isFinite(charge)
                || charge < 0.0
                || !Double.isFinite(resistanceScale)
                || resistanceScale < 0.0) {
            return 0.0;
        }

        return 1.0 / (1.0 + resistanceScale * charge / power);
    }

    public static double paidFraction(long available, long requested) {
        if (available <= 0L || requested <= 0L) {
            return 0.0;
        }

        return Math.min(1.0, (double) available / requested);
    }

    public static double limitForce(
            double force,
            double mass,
            double freedom,
            double accelerationLimit
    ) {
        if (freedom < 1.0E-6) {
            return force;
        }

        return Math.min(force, accelerationLimit * mass / freedom);
    }

    public static double projectileSpeedLimit(double strength) {
        return poweredSpeedLimit(strength) * PROJECTILE_SPEED_MULTIPLIER;
    }

    public static double projectileAccelerationLimit(double strength) {
        return accelerationLimit(strength) * PROJECTILE_ACCELERATION_MULTIPLIER;
    }

    public static double speedScale(
            double velocitySquared,
            double velocityDotDelta,
            double deltaSquared,
            double speedLimit
    ) {
        if (deltaSquared < 1.0E-12) {
            return 1.0;
        }

        double b = 2.0 * velocityDotDelta;
        double ceiling = Math.max(speedLimit * speedLimit, velocitySquared);
        double c = velocitySquared - ceiling;
        double discriminant = Math.max(0.0, b * b - 4.0 * deltaSquared * c);

        return clamp01(
                (-b + Math.sqrt(discriminant)) / (2.0 * deltaSquared)
        );
    }

    public static double brakingRatio(
            double downwardSpeed,
            double upwardImpulse
    ) {
        if (!Double.isFinite(downwardSpeed)
                || !Double.isFinite(upwardImpulse)
                || downwardSpeed <= 0.0
                || upwardImpulse <= 0.0) {
            return 1.0;
        }

        double remaining = Math.max(0.0, downwardSpeed - upwardImpulse);
        double ratio = remaining / downwardSpeed;

        return ratio * ratio;
    }
}