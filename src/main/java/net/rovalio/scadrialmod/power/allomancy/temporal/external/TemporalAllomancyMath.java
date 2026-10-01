package net.rovalio.scadrialmod.power.allomancy.temporal.external;

public final class TemporalAllomancyMath {

    public static final double MIN_RADIUS = 2.0;

    public static final int CHARGE_TICKS = 40;
    public static final int EXIT_EFFECT_TICKS = 100;

    public static final double MAX_DEFLECTION_DEGREES = 60.0;

    private TemporalAllomancyMath() {}

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double maxRadius(double strength, boolean boosted) {
        double logarithm = logStrength(strength);

        return boosted
                ? 2.5 * logarithm + 4.0
                : 1.5 * logarithm + 2.0;
    }

    public static double outerRadius(double strength, boolean boosted) {
        double logarithm = logStrength(strength);

        return boosted
                ? 4.0 * logarithm + 8.0
                : 2.5 * logarithm + 6.0;
    }

    private static double logStrength(double strength) {
        double safe = Double.isFinite(strength) ? strength : 1.0;

        return Math.log(clamp(safe, 1.0, 16.0)) / Math.log(2.0);
    }

    public static double charge(long ticks) {
        return clamp((double) ticks / CHARGE_TICKS, 0.0, 1.0);
    }

    public static double radius(
            double strength,
            boolean boosted,
            double charge
    ) {
        return MIN_RADIUS
                + (maxRadius(strength, boosted) - MIN_RADIUS)
                * clamp(charge, 0.0, 1.0);
    }

    public static double subunitsPerTick(double radius){
        return Math.pow(
                2.0,
                (clamp(radius, 2.0, 16.0) - 2.0) / 7
        );
    }

    public static double volume(double radius){
        return 4.0 * Math.PI * radius * radius * radius / 3;
    }

    public static boolean intersectsBox(
            double cx,
            double cy,
            double cz,
            double radius,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ){
        double dx = cx - clamp(cx, minX, maxX);
        double dy = cy - clamp(cy, minY, maxY);
        double dz = cz - clamp(cz, minZ, maxZ);

        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public static double rate(int exponent){
        return Math.scalb(
                1.0,
                Math.max(-2, Math.min(2, exponent))
        );
    }

    public static int exponent(
            double distanceSquared,
            double radius,
            double outerRadius,
            boolean bendalloy,
            boolean boosted
    ){
        if (distanceSquared > outerRadius * outerRadius){
            return 0;
        }

        int interior = bendalloy ? 1 : -1;

        return distanceSquared <= radius * radius
                ? interior
                : -interior * (boosted ? 2 : 1);
    }

    public static int scheduledSteps(
            double rate,
            long tick,
            int phase
    ){
        if (rate >= 1.0){
            return (int) rate;
        }

        int period = (int) Math.round(1.0 / rate);

        return Math.floorMod(tick + phase, period) == 0 ? 1 : 0;
    }

    public static double firstCrossing(
            double px,
            double py,
            double pz,
            double dx,
            double dy,
            double dz,
            double radius
    ){
        double a = dx * dx + dy * dy + dz * dz;

        if (a < 1.0E-12){
            return Double.POSITIVE_INFINITY;
        }

        double b = 2.0 * (px * dx + py * dy + pz * dz);
        double c = px * px + py * py + pz * pz - radius * radius;

        double discriminant = b * b - 4.0 * a * c;

        if (discriminant <= 1.0E-12){
            return Double.POSITIVE_INFINITY;
        }

        double root = Math.sqrt(discriminant);

        double first = (-b - root) / (2 * a);
        double second = (-b + root) / (2 * a);

        if (first > 1.0E-6 && first <= 1.0){
            return first;
        }

        if (second > 1.0E-6 && second <= 1.0) {
            return second;
        }

        return Double.POSITIVE_INFINITY;
    }
}
