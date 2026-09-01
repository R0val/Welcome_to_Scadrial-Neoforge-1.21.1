package net.rovalio.scadrialmod.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        fromSerializedName(String serializedName) {

            if (serializedName == null) {
                return Optional.empty();
            }

            return switch (
                    serializedName.toLowerCase(Locale.ROOT)
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

    private boolean powerAssignmentComplete;

    private MetallicProfile allomancy;
    private MetallicProfile feruchemy;

    private boolean allomancySnapped;
    private double allomancySnappingDamage;

    public ScadrialPlayerData() {
        reset();
    }

    public boolean isPowerAssignmentComplete() {
        return powerAssignmentComplete;
    }

    public void completePowerAssignment() {
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

    public PowerProfile getFeruchemicalProfile() {
        return feruchemy.profile();
    }

    public Optional<MetalType> getFeruchemicalMetal() {
        return Optional.ofNullable(
                feruchemy.metal()
        );
    }

    public boolean isAllomancySnapped() {
        return allomancySnapped;
    }

    public double getAllomancySnappingDamage() {
        return allomancySnappingDamage;
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
    }

    public boolean hasAllomancy() {
        return allomancy.hasPower();
    }

    public boolean canUseAllomancy() {
        return hasAllomancy()
                && allomancySnapped;
    }

    public boolean hasFeruchemy() {
        return feruchemy.hasPower();
    }

    public boolean isMisting() {
        return allomancy.profile()
                == PowerProfile.SINGLE;
    }

    public boolean isMistborn() {
        return allomancy.profile()
                == PowerProfile.FULL;
    }

    public boolean isFerring() {
        return feruchemy.profile()
                == PowerProfile.SINGLE;
    }

    public boolean isFeruchemist() {
        return feruchemy.profile()
                == PowerProfile.FULL;
    }

    public boolean isTwinborn() {
        return hasAllomancy()
                && hasFeruchemy();
    }

    public boolean isFullborn() {
        return isMistborn()
                && isFeruchemist();
    }

    public Set<MetalType> getAllomanticMetals() {
        return allomancy.getMetals();
    }

    public Set<MetalType> getFeruchemicalMetals() {
        return feruchemy.getMetals();
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

    public void reset() {
        powerAssignmentComplete = false;

        allomancy =
                MetallicProfile.none();

        feruchemy =
                MetallicProfile.none();

        allomancySnapped = false;
        allomancySnappingDamage = 0.0;
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

        rootTag.put(
                TAG_ALLOMANCY,
                allomancyTag
        );

        rootTag.put(
                TAG_FERUCHEMY,
                serializeProfile(feruchemy)
        );

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

        boolean assignmentComplete =
                rootTag.contains(
                        TAG_POWER_ASSIGNMENT_COMPLETE,
                        Tag.TAG_BYTE
                )
                        && rootTag.getBoolean(
                        TAG_POWER_ASSIGNMENT_COMPLETE
                );

        /*
         * An incomplete assignment must not restore
         * partially written power data.
         */
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

        powerAssignmentComplete = true;
    }

    private static boolean isValidSnappingDamage(
            double damage
    ) {
        return Double.isFinite(damage)
                && damage > 0.0
                && damage
                <= MAX_RECORDED_SNAPPING_DAMAGE;
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
         * Invalid or missing data is reduced to NONE.
         * This prevents corrupted SINGLE profiles.
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
}