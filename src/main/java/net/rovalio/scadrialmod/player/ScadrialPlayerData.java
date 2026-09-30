package net.rovalio.scadrialmod.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.physical.internal.PhysicalInternalAllomancyMath;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.*;

public final class ScadrialPlayerData
        implements INBTSerializable<CompoundTag> {

    public enum PowerProfile
            implements StringRepresentable {

        NONE("none"),
        SINGLE("single"),
        FULL("full");

        private final String serializedName;

        PowerProfile(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public static Optional<PowerProfile>
        fromSerializedName(
                String serializedName
        ) {
            if (serializedName == null) {
                return Optional.empty();
            }

            return switch (
                    serializedName.toLowerCase(
                            Locale.ROOT
                    )
                    ) {
                case "none" ->
                        Optional.of(NONE);

                case "single" ->
                        Optional.of(SINGLE);

                case "full" ->
                        Optional.of(FULL);

                default ->
                        Optional.empty();
            };
        }
    }

    /*
     * Represents either an Allomantic or
     * Feruchemical power profile.
     *
     * NONE and FULL cannot store a single metal.
     * SINGLE must store exactly one metal.
     */
    private record MetallicProfile(
            PowerProfile profile,
            MetalType metal
    ) {

        private MetallicProfile {
            Objects.requireNonNull(
                    profile,
                    "Power profile cannot be null"
            );

            if (profile == PowerProfile.SINGLE
                    && metal == null) {

                throw new IllegalArgumentException(
                        "A SINGLE profile requires a metal"
                );
            }

            if (profile != PowerProfile.SINGLE
                    && metal != null) {

                throw new IllegalArgumentException(
                        profile
                                + " profile cannot store "
                                + "a single metal"
                );
            }
        }

        private static MetallicProfile none() {
            return new MetallicProfile(
                    PowerProfile.NONE,
                    null
            );
        }

        private static MetallicProfile of(
                PowerProfile profile,
                MetalType metal
        ) {
            return new MetallicProfile(
                    profile,
                    metal
            );
        }

        private boolean hasPower() {
            return profile != PowerProfile.NONE;
        }

        private Set<MetalType> getMetals() {
            return switch (profile) {

                case NONE ->
                        Set.of();

                case SINGLE ->
                        Set.of(metal);

                case FULL ->
                        MetalType.getStandardMetals();
            };
        }
    }

    private static final String
            TAG_POWER_ASSIGNMENT_COMPLETE =
            "PowerAssignmentComplete";

    private static final String
            TAG_ALLOMANCY_STRENGTH =
            "AllomancyStrength";

    private static final String
            TAG_ALLOMANCY =
            "Allomancy";

    private static final String
            TAG_FERUCHEMY =
            "Feruchemy";

    private static final String
            TAG_PROFILE =
            "Profile";

    private static final String
            TAG_METAL =
            "Metal";

    private static final String
            TAG_SNAPPED =
            "Snapped";

    private static final String
            TAG_SNAPPING_DAMAGE =
            "SnappingDamage";

    private static final double
            MAX_RECORDED_SNAPPING_DAMAGE =
            1.0;

    private static final String TAG_ALLOMANTIC_RESERVES =
            "AllomanticReserves";

    public static final long RESERVE_SUBUNITS_PER_UNIT =
            1_000L;

    private static final AllomanticFuel[] FUELS =
            AllomanticFuel.values();

    private final long[] allomanticReserves =
            new long[FUELS.length];

    private static final String TAG_BURNING_FUELS = "BurningFuels";

    private final EnumSet<AllomanticFuel> burningFuels =
            EnumSet.noneOf(AllomanticFuel.class);

    private boolean powerAssignmentComplete;

    private boolean allomancyStrengthInitialized;
    private double allomancyStrength;

    private MetallicProfile allomancy;
    private MetallicProfile feruchemy;

    private boolean allomancySnapped;
    private double allomancySnappingDamage;

    private double pewterDebt;
    private int pewterRecoveryDelay;

    private int usableFuelMask;
    private boolean usableFuelMaskDirty = true;

    private double appliedPewterStrength;

    public ScadrialPlayerData() {
        reset();
    }

    public boolean isPowerAssignmentComplete() {
        return powerAssignmentComplete;
    }

    public void completePowerAssignment() {
        if (!allomancyStrengthInitialized) {
            throw new IllegalStateException(
                    "Power assignment cannot be completed "
                            + "before Allomancy strength is resolved"
            );
        }

        if (hasAllomancy()
                && allomancyStrength <= 0.0) {

            throw new IllegalStateException(
                    "An Allomantic profile requires "
                            + "positive Allomancy strength"
            );
        }

        if (!hasAllomancy()
                && allomancyStrength != 0.0) {

            throw new IllegalStateException(
                    "A profile without Allomancy "
                            + "must have strength 0"
            );
        }

        powerAssignmentComplete = true;
    }

    public PowerProfile getAllomanticProfile() {
        return allomancy.profile();
    }

    public Optional<MetalType> getAllomanticMetal() {
        return Optional.ofNullable(
                allomancy.metal()
        );
    }

    public void configureAllomancy(
            PowerProfile profile,
            MetalType metal,
            boolean snapped
    ) {
        MetallicProfile newProfile =
                MetallicProfile.of(
                        profile,
                        metal
                );

        if (snapped
                && !newProfile.hasPower()) {

            throw new IllegalArgumentException(
                    "A player without Allomancy "
                            + "cannot be Snapped"
            );
        }

        /*
         * Configuration may preserve an existing
         * Snapping, but it cannot create a new one
         * without recording its exact damage.
         */
        if (snapped
                && !allomancySnapped) {

            throw new IllegalArgumentException(
                    "A new Allomantic Snapping must "
                            + "be recorded with its damage"
            );
        }

        double preservedSnappingDamage =
                snapped
                        ? allomancySnappingDamage
                        : 0.0;

        allomancy = newProfile;
        allomancySnapped = snapped;
        allomancySnappingDamage =
                preservedSnappingDamage;

        invalidateUsableFuels();
    }

    public boolean hasAllomancy() {
        return allomancy.hasPower();
    }

    public boolean canUseAllomancy() {
        return hasAllomancy()
                && allomancySnapped
                && hasActiveAllomancyStrength();
    }

    public boolean isMisting() {
        return allomancy.profile()
                == PowerProfile.SINGLE;
    }

    public boolean isMistborn() {
        return allomancy.profile()
                == PowerProfile.FULL;
    }

    public Set<MetalType> getAllomanticMetals() {
        return allomancy.getMetals();
    }

    public boolean hasAllomanticAccess(MetalType metal) {
        return switch (allomancy.profile()) {
            case NONE -> false;
            case SINGLE -> allomancy.metal() == metal;
            case FULL -> metal != null;
        };
    }

    public boolean isAllomancyStrengthInitialized() {
        return allomancyStrengthInitialized;
    }

    public boolean hasActiveAllomancyStrength() {
        return hasAllomancy()
                && allomancyStrengthInitialized
                && allomancyStrength > 0.0;
    }

    public double getAllomancyStrength() {
        if (!allomancyStrengthInitialized) {
            throw new IllegalStateException(
                    "Allomancy strength has not been initialized"
            );
        }

        return allomancyStrength;
    }

    public void setAllomancyStrength(
            double strength
    ) {
        if (!Double.isFinite(strength)
                || strength < 0.0) {

            throw new IllegalArgumentException(
                    "Allomancy strength must be finite "
                            + "and greater than or equal to 0"
            );
        }

        if (hasAllomancy()
                && strength <= 0.0) {

            throw new IllegalArgumentException(
                    "An Allomantic profile requires "
                            + "positive Allomancy strength"
            );
        }

        if (!hasAllomancy()
                && strength != 0.0) {

            throw new IllegalArgumentException(
                    "A profile without Allomancy "
                            + "must have strength 0"
            );
        }

        allomancyStrengthInitialized = true;
        allomancyStrength = strength;

        invalidateUsableFuels();
    }

    public void clearAllomancyStrengthInitialization() {
        allomancyStrengthInitialized = false;
        allomancyStrength = 0.0;

        invalidateUsableFuels();
    }

    public boolean isAllomancySnapped() {
        return allomancySnapped;
    }

    public double getAllomancySnappingDamage() {
        return allomancySnappingDamage;
    }

    public void recordAllomancySnapping(
            double damage
    ) {
        if (!hasAllomancy()) {
            throw new IllegalStateException(
                    "A player without Allomancy "
                            + "cannot be Snapped"
            );
        }

        if (allomancySnapped) {
            throw new IllegalStateException(
                    "Allomancy has already Snapped"
            );
        }

        if (!isValidSnappingDamage(damage)) {
            throw new IllegalArgumentException(
                    "Allomantic Snapping damage must "
                            + "be finite and greater than 0 "
                            + "without exceeding 1"
            );
        }

        allomancySnapped = true;
        allomancySnappingDamage = damage;

        invalidateUsableFuels();
    }

    private static boolean isValidSnappingDamage(
            double damage
    ) {
        return Double.isFinite(damage)
                && damage > 0.0
                && damage
                <= MAX_RECORDED_SNAPPING_DAMAGE;
    }

    public PowerProfile getFeruchemicalProfile() {
        return feruchemy.profile();
    }

    public Optional<MetalType> getFeruchemicalMetal() {
        return Optional.ofNullable(
                feruchemy.metal()
        );
    }

    public void configureFeruchemy(
            PowerProfile profile,
            MetalType metal
    ) {
        feruchemy =
                MetallicProfile.of(
                        profile,
                        metal
                );
    }

    public boolean hasFeruchemy() {
        return feruchemy.hasPower();
    }

    public boolean isFerring() {
        return feruchemy.profile()
                == PowerProfile.SINGLE;
    }

    public boolean isFeruchemist() {
        return feruchemy.profile()
                == PowerProfile.FULL;
    }

    public Set<MetalType> getFeruchemicalMetals() {
        return feruchemy.getMetals();
    }

    public boolean isTwinborn() {
        return hasAllomancy()
                && hasFeruchemy();
    }

    public boolean isFullborn() {
        return isMistborn()
                && isFeruchemist();
    }

    public Set<MetalType> getCompoundableMetals() {
        EnumSet<MetalType> compoundableMetals =
                EnumSet.noneOf(MetalType.class);

        compoundableMetals.addAll(
                getAllomanticMetals()
        );

        compoundableMetals.retainAll(
                getFeruchemicalMetals()
        );

        return Collections.unmodifiableSet(
                compoundableMetals
        );
    }

    public boolean isCompounder() {
        return !getCompoundableMetals().isEmpty();
    }

    public long getAllomanticReserveSubunits(
            AllomanticFuel fuel
    ) {
        Objects.requireNonNull(
                fuel,
                "Allomantic fuel cannot be null"
        );

        return allomanticReserves[fuel.ordinal()];
    }

    public long getAllomanticReserveSpaceSubunits(
            AllomanticFuel fuel,
            long capacitySubunits
    ) {
        requireNonNegativeReserveAmount(
                capacitySubunits,
                "Reserve capacity"
        );

        long current =
                getAllomanticReserveSubunits(fuel);

        return current >= capacitySubunits
                ? 0L
                : capacitySubunits - current;
    }

    /*
     * Adds as much as fits and returns the amount accepted.
     * The caller decides whether partial acceptance is valid.
     */
    public long addAllomanticReserveSubunits(
            AllomanticFuel fuel,
            long amountSubunits,
            long capacitySubunits
    ) {
        Objects.requireNonNull(
                fuel,
                "Allomantic fuel cannot be null"
        );

        requireNonNegativeReserveAmount(
                amountSubunits,
                "Reserve amount"
        );

        requireNonNegativeReserveAmount(
                capacitySubunits,
                "Reserve capacity"
        );

        long current =
                getAllomanticReserveSubunits(fuel);

        long space =
                getAllomanticReserveSpaceSubunits(
                        fuel,
                        capacitySubunits
                );

        long accepted =
                Math.min(amountSubunits, space);

        if (accepted > 0L) {
            allomanticReserves[fuel.ordinal()] =
                    current + accepted;

            if (current == 0L) {
                invalidateUsableFuels();
            }
        }

        return accepted;
    }

    //Removes up to the requested amount and returns how much was actually consumed.
    public long consumeAllomanticReserveSubunits(
            AllomanticFuel fuel,
            long requestedSubunits
    ) {
        Objects.requireNonNull(
                fuel,
                "Allomantic fuel cannot be null"
        );

        requireNonNegativeReserveAmount(
                requestedSubunits,
                "Requested reserve amount"
        );

        long current =
                getAllomanticReserveSubunits(fuel);

        long consumed =
                Math.min(requestedSubunits, current);

        long remaining =
                current - consumed;

        allomanticReserves[fuel.ordinal()] = remaining;

        if (remaining == 0L && current > 0L) {
            invalidateUsableFuels();
        }

        return consumed;
    }

    public void clearAllomanticReserves() {
        Arrays.fill(allomanticReserves, 0L);
        invalidateUsableFuels();
    }

    public Map<AllomanticFuel, Long> getAllomanticReservesSubunits() {
        EnumMap<AllomanticFuel, Long> reserves =
                new EnumMap<>(AllomanticFuel.class);

        for (AllomanticFuel fuel : FUELS) {
            long amount = allomanticReserves[fuel.ordinal()];

            if (amount > 0L) {
                reserves.put(fuel, amount);
            }
        }

        return Collections.unmodifiableMap(reserves);
    }

    private static void requireNonNegativeReserveAmount(
            long amount,
            String description
    ) {
        if (amount < 0L) {
            throw new IllegalArgumentException(
                    description + " cannot be negative"
            );
        }
    }

    public boolean isBurning(AllomanticFuel fuel) {
        return burningFuels.contains(fuel);
    }

    public void setBurning(AllomanticFuel fuel, boolean burning) {
        boolean changed = burning
                ? burningFuels.add(fuel)
                : burningFuels.remove(fuel);

        if (changed) {
            invalidateUsableFuels();
        }
    }

    public Set<AllomanticFuel> getBurningFuels() {
        return Collections.unmodifiableSet(EnumSet.copyOf(burningFuels));
    }

    public boolean hasBurningFuels() {
        return !burningFuels.isEmpty();
    }

    public boolean isUsableAndBurning(AllomanticFuel fuel) {
        return (usableFuelMask() & (1 << fuel.ordinal())) != 0;
    }

    private int usableFuelMask() {
        if (usableFuelMaskDirty) {
            int mask = 0;

            if (canUseAllomancy()) {
                for (AllomanticFuel fuel : burningFuels) {
                    if (allomanticReserves[fuel.ordinal()] > 0L
                            && hasAllomanticAccess(fuel.getRequiredPower())) {
                        mask |= 1 << fuel.ordinal();
                    }
                }
            }

            usableFuelMask = mask;
            usableFuelMaskDirty = false;
        }

        return usableFuelMask;
    }

    private void invalidateUsableFuels() {
        usableFuelMaskDirty = true;
    }

    public double getAppliedPewterStrength() {
        return appliedPewterStrength;
    }

    public void setAppliedPewterStrength(double strength) {
        appliedPewterStrength = strength;
    }

    public double getPewterDebt() {
        return pewterDebt;
    }

    public void setPewterDebt(double value) {
        pewterDebt = Double.isFinite(value)
                ? Math.max(
                0.0,
                Math.min(
                        PhysicalInternalAllomancyMath.MAX_PEWTER_DEBT,
                        value
                )
        )
                : 0.0;
    }

    public int getPewterRecoveryDelay() {
        return pewterRecoveryDelay;
    }

    public void setPewterRecoveryDelay(int ticks) {
        pewterRecoveryDelay = Math.max(0, ticks);
    }

    public void reset() {
        pewterDebt = 0.0;
        pewterRecoveryDelay = 0;

        powerAssignmentComplete = false;

        allomancy =
                MetallicProfile.none();

        feruchemy =
                MetallicProfile.none();

        allomancySnapped = false;
        allomancySnappingDamage = 0.0;

        allomancyStrengthInitialized = false;
        allomancyStrength = 0.0;

        burningFuels.clear();
        Arrays.fill(allomanticReserves, 0L);

        invalidateUsableFuels();
    }

    @Override
    public CompoundTag serializeNBT(
            HolderLookup.Provider provider
    ) {
        CompoundTag rootTag =
                new CompoundTag();

        rootTag.putBoolean(
                TAG_POWER_ASSIGNMENT_COMPLETE,
                powerAssignmentComplete
        );

        CompoundTag reservesTag =
                new CompoundTag();

        for (AllomanticFuel fuel : FUELS) {
            long amount = allomanticReserves[fuel.ordinal()];

            if (amount > 0L) {
                reservesTag.putLong(
                        fuel.getSerializedName(),
                        amount
                );
            }
        }

        rootTag.put(
                TAG_ALLOMANTIC_RESERVES,
                reservesTag
        );

        CompoundTag allomancyTag =
                serializeProfile(allomancy);

        allomancyTag.putBoolean(
                TAG_SNAPPED,
                allomancySnapped
        );

        if (allomancySnapped
                && isValidSnappingDamage(
                allomancySnappingDamage
        )) {

            allomancyTag.putDouble(
                    TAG_SNAPPING_DAMAGE,
                    allomancySnappingDamage
            );
        }

        if (allomancyStrengthInitialized) {
            rootTag.putDouble(
                    TAG_ALLOMANCY_STRENGTH,
                    allomancyStrength
            );
        }

        rootTag.put(
                TAG_ALLOMANCY,
                allomancyTag
        );

        rootTag.put(
                TAG_FERUCHEMY,
                serializeProfile(feruchemy)
        );

        ListTag burningTag = new ListTag();

        for (AllomanticFuel fuel : burningFuels) {
            burningTag.add(StringTag.valueOf(fuel.getSerializedName()));
        }

        rootTag.put(TAG_BURNING_FUELS, burningTag);

        rootTag.putDouble("PewterDebt", pewterDebt);
        rootTag.putInt("PewterRecoveryDelay", pewterRecoveryDelay);

        return rootTag;
    }

    @Override
    public void deserializeNBT(
            HolderLookup.Provider provider,
            CompoundTag rootTag
    ) {
        reset();

        if (rootTag == null) {
            return;
        }

        setPewterDebt(rootTag.getDouble("PewterDebt"));
        setPewterRecoveryDelay(rootTag.getInt("PewterRecoveryDelay"));

        loadAllomanticReserves(rootTag);

        boolean assignmentComplete =
                rootTag.contains(
                        TAG_POWER_ASSIGNMENT_COMPLETE,
                        Tag.TAG_BYTE
                )
                        && rootTag.getBoolean(
                        TAG_POWER_ASSIGNMENT_COMPLETE
                );

        if (!assignmentComplete) {
            return;
        }

        MetallicProfile loadedAllomancy =
                loadProfile(
                        rootTag,
                        TAG_ALLOMANCY
                );

        MetallicProfile loadedFeruchemy =
                loadProfile(
                        rootTag,
                        TAG_FERUCHEMY
                );

        boolean loadedSnapped = false;
        double loadedSnappingDamage = 0.0;

        if (rootTag.contains(
                TAG_ALLOMANCY,
                Tag.TAG_COMPOUND
        )) {
            CompoundTag allomancyTag =
                    rootTag.getCompound(
                            TAG_ALLOMANCY
                    );

            loadedSnapped =
                    allomancyTag.contains(
                            TAG_SNAPPED,
                            Tag.TAG_BYTE
                    )
                            && allomancyTag.getBoolean(
                            TAG_SNAPPED
                    )
                            && loadedAllomancy.hasPower();

            if (loadedSnapped
                    && allomancyTag.contains(
                    TAG_SNAPPING_DAMAGE,
                    Tag.TAG_DOUBLE
            )) {
                double candidateDamage =
                        allomancyTag.getDouble(
                                TAG_SNAPPING_DAMAGE
                        );

                if (isValidSnappingDamage(
                        candidateDamage
                )) {
                    loadedSnappingDamage =
                            candidateDamage;
                }
            }
        }

        allomancy = loadedAllomancy;
        feruchemy = loadedFeruchemy;
        allomancySnapped = loadedSnapped;
        allomancySnappingDamage =
                loadedSnappingDamage;

        if (!loadedAllomancy.hasPower()) {
            allomancyStrengthInitialized = true;
            allomancyStrength = 0.0;

        } else if (rootTag.contains(
                TAG_ALLOMANCY_STRENGTH,
                Tag.TAG_DOUBLE
        )) {
            double loadedStrength =
                    rootTag.getDouble(
                            TAG_ALLOMANCY_STRENGTH
                    );

            if (Double.isFinite(loadedStrength)
                    && loadedStrength > 0.0) {

                allomancyStrengthInitialized = true;
                allomancyStrength = loadedStrength;
            } else {
                clearAllomancyStrengthInitialization();
            }

        } else {
            // Perfil antiguo: la fuerza se inicializará en el servidor.
            clearAllomancyStrengthInitialization();
        }

        powerAssignmentComplete = true;
        invalidateUsableFuels();

        if (rootTag.contains(TAG_BURNING_FUELS, Tag.TAG_LIST)) {
            ListTag burningTag = rootTag.getList(
                    TAG_BURNING_FUELS,
                    Tag.TAG_STRING
            );

            for (int i = 0; i < burningTag.size(); i++) {
                AllomanticFuel.fromSerializedName(
                        burningTag.getString(i)
                ).ifPresent(burningFuels::add);
            }
        }
    }

    private static CompoundTag serializeProfile(
            MetallicProfile metallicProfile
    ) {
        CompoundTag profileTag =
                new CompoundTag();

        profileTag.putString(
                TAG_PROFILE,
                metallicProfile
                        .profile()
                        .getSerializedName()
        );

        if (metallicProfile.profile()
                == PowerProfile.SINGLE) {

            profileTag.putString(
                    TAG_METAL,
                    metallicProfile
                            .metal()
                            .getSerializedName()
            );
        }

        return profileTag;
    }

    private static MetallicProfile loadProfile(
            CompoundTag rootTag,
            String key
    ) {
        if (!rootTag.contains(
                key,
                Tag.TAG_COMPOUND
        )) {
            return MetallicProfile.none();
        }

        CompoundTag profileTag =
                rootTag.getCompound(key);

        PowerProfile profile =
                loadPowerProfile(profileTag);

        MetalType metal =
                loadMetal(profileTag);

        /*
         * Migrate the former Era 1 "atium Misting"
         * profile to the ELECTRUM power family.
         *
         * The same migration is deliberately not
         * applied to Feruchemy.
         */
        if (TAG_ALLOMANCY.equals(key)
                && profile == PowerProfile.SINGLE
                && profileTag.contains(
                TAG_METAL,
                Tag.TAG_STRING
        )
                && "atium".equalsIgnoreCase(
                profileTag.getString(
                        TAG_METAL
                )
        )) {

            metal = MetalType.ELECTRUM;
        }

        /*
         * Invalid or missing SINGLE data is reduced
         * to NONE. This also removes unsupported
         * legacy Feruchemical atium profiles.
         */
        if (profile == PowerProfile.SINGLE
                && metal == null) {

            return MetallicProfile.none();
        }

        if (profile != PowerProfile.SINGLE) {
            metal = null;
        }

        return MetallicProfile.of(
                profile,
                metal
        );
    }

    private static PowerProfile loadPowerProfile(
            CompoundTag profileTag
    ) {
        if (!profileTag.contains(
                TAG_PROFILE,
                Tag.TAG_STRING
        )) {
            return PowerProfile.NONE;
        }

        return PowerProfile
                .fromSerializedName(
                        profileTag.getString(
                                TAG_PROFILE
                        )
                )
                .orElse(PowerProfile.NONE);
    }

    private static MetalType loadMetal(
            CompoundTag profileTag
    ) {
        if (!profileTag.contains(
                TAG_METAL,
                Tag.TAG_STRING
        )) {
            return null;
        }

        return MetalType
                .fromSerializedName(
                        profileTag.getString(
                                TAG_METAL
                        )
                )
                .orElse(null);
    }

    private void loadAllomanticReserves(
            CompoundTag rootTag
    ) {
        if (!rootTag.contains(
                TAG_ALLOMANTIC_RESERVES,
                Tag.TAG_COMPOUND
        )) {
            return;
        }

        CompoundTag reservesTag =
                rootTag.getCompound(
                        TAG_ALLOMANTIC_RESERVES
                );

        for (String serializedName
                : reservesTag.getAllKeys()) {

            Optional<AllomanticFuel> fuel =
                    AllomanticFuel.fromSerializedName(
                            serializedName
                    );

            if (fuel.isEmpty()
                    || !reservesTag.contains(
                    serializedName,
                    Tag.TAG_LONG
            )) {
                continue;
            }

            long amount =
                    reservesTag.getLong(serializedName);

            if (amount > 0L) {
                allomanticReserves[fuel.get().ordinal()] = amount;
            }
        }
    }
}