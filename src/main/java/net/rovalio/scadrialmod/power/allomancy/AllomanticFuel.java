package net.rovalio.scadrialmod.power.allomancy;

import net.minecraft.util.StringRepresentable;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Allomantic substances and their required basic powers.
 *
 * Each entry has its own identity for reserves and effects.
 * Physical presentations such as dust, shavings and beads
 * do not create additional entries.
 */
public enum AllomanticFuel implements StringRepresentable {

    IRON(MetalType.IRON),
    STEEL(MetalType.STEEL),
    TIN(MetalType.TIN),
    PEWTER(MetalType.PEWTER),

    ZINC(MetalType.ZINC),
    BRASS(MetalType.BRASS),
    COPPER(MetalType.COPPER),
    BRONZE(MetalType.BRONZE),

    CHROMIUM(MetalType.CHROMIUM),
    NICROSIL(MetalType.NICROSIL),
    ALUMINIUM(MetalType.ALUMINIUM),
    DURALUMIN(MetalType.DURALUMIN),

    CADMIUM(MetalType.CADMIUM),
    BENDALLOY(MetalType.BENDALLOY),
    GOLD(MetalType.GOLD),
    ELECTRUM(MetalType.ELECTRUM),

    ATIUM_ELECTRUM("atium_electrum",
            MetalType.ELECTRUM,
            AllomanticSource.RUIN
    ),

    // Mod rule: access through the GOLD power family.
    MALATIUM(
            "malatium",
            MetalType.GOLD,
            AllomanticSource.RUIN
    );

    public enum AllomanticSource {
        PRESERVATION,
        RUIN
    }

    private static final Map<String, AllomanticFuel>
            BY_SERIALIZED_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            AllomanticFuel::getSerializedName,
                            metal -> metal
                    ));

    private final String serializedName;
    private final MetalType requiredPower;
    private final AllomanticSource allomanticSource;

    AllomanticFuel(MetalType standardMetal) {
        this(
                standardMetal.getSerializedName(),
                standardMetal,
                AllomanticSource.PRESERVATION
        );
    }

    AllomanticFuel(
            String serializedName,
            MetalType requiredPower,
            AllomanticSource allomanticSource
    ) {
        this.serializedName =
                Objects.requireNonNull(serializedName);

        this.requiredPower =
                Objects.requireNonNull(requiredPower);

        this.allomanticSource =
                Objects.requireNonNull(allomanticSource);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public MetalType getRequiredPower() {
        return requiredPower;
    }

    public AllomanticSource getAllomanticSource() {
        return allomanticSource;
    }

    public static Optional<AllomanticFuel> fromSerializedName(
            String serializedName
    ) {
        if (serializedName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                BY_SERIALIZED_NAME.get(
                        serializedName.toLowerCase(Locale.ROOT)
                )
        );
    }
}