package net.rovalio.scadrialmod.power;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Hay que valorar si quitar el atium de aquí y dejar solo el electrum
 * Me refiero, un alomante de electrum podría quemar electrum y atium, pero ahora a ver cómo programo eso
 * Me voy a morir programando las reservas de metal. Puto NBT
 *
 * Hay que diseñar las interfaces
 *
 * Debería escribir un saludo o introducción en la clase principal
 */

public enum MetalType
        implements StringRepresentable {

    IRON("iron", true, "Lurcher", "Skimmer"),
    STEEL("steel", true, "Coinshot", "Steelrunner"),
    TIN("tin", true, "Tineye", "Windwhisperer"),
    PEWTER("pewter", true, "Thug", "Brute"),

    ZINC("zinc", true, "Rioter", "Sparker"),
    BRASS("brass", true, "Soother", "Firesoul"),
    COPPER("copper", true, "Smoker", "Archivist"),
    BRONZE("bronze", true, "Seeker", "Sentry"),

    CHROMIUM("chromium", true, "Leecher", "Spinner"),
    NICROSIL("nicrosil", true, "Nicroburst", "Soulbearer"),
    ALUMINUM("aluminum", true, "Aluminum Gnat", "Trueself"),
    DURALUMIN("duralumin", true, "Duralumin Gnat", "Connector"),

    CADMIUM("cadmium", true, "Pulser", "Gasper"),
    BENDALLOY("bendalloy", true, "Slider", "Subsumer"),
    GOLD("gold", true, "Augur", "Bloodmaker"),
    ELECTRUM("electrum", true, "Oracle", "Pinnacle"),

    //Atium is a special metal fueled by Ruin. It is not included in the standard sixteen.
    // No canonical specialized term is currently documented for an Atium Ferring.
    ATIUM("atium", false, "Seer", "Atium Ferring",
            AllomanticSource.RUIN
    );

    public enum AllomanticSource {
        PRESERVATION,
        RUIN
    }

    private static final Map<String, MetalType>
            BY_SERIALIZED_NAME =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    MetalType::getSerializedName,
                                    metal -> metal
                            )
                    );

    private static final Set<MetalType>
            STANDARD_METALS =
            Arrays.stream(values())
                    .filter(
                            MetalType::isStandardMetal
                    )
                    .collect(
                            Collectors.toUnmodifiableSet()
                    );

    private final String serializedName;
    private final boolean standardMetal;

    private final String allomanticTitle;
    private final String feruchemicalTitle;

    private final AllomanticSource
            allomanticSource;

    /*
     * All standard metals are fueled by
     * Preservation unless specified otherwise.
     */
    MetalType(
            String serializedName,
            boolean standardMetal,
            String allomanticTitle,
            String feruchemicalTitle
    ) {
        this(
                serializedName,
                standardMetal,
                allomanticTitle,
                feruchemicalTitle,
                AllomanticSource.PRESERVATION
        );
    }

    MetalType(
            String serializedName,
            boolean standardMetal,
            String allomanticTitle,
            String feruchemicalTitle,
            AllomanticSource allomanticSource
    ) {
        this.serializedName =
                Objects.requireNonNull(
                        serializedName,
                        "Serialized name cannot be null"
                );

        this.standardMetal =
                standardMetal;

        this.allomanticTitle =
                Objects.requireNonNull(
                        allomanticTitle,
                        "Allomantic title cannot be null"
                );

        this.feruchemicalTitle =
                Objects.requireNonNull(
                        feruchemicalTitle,
                        "Feruchemical title cannot be null"
                );

        this.allomanticSource =
                Objects.requireNonNull(
                        allomanticSource,
                        "Allomantic source cannot be null"
                );
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public boolean isStandardMetal() {
        return standardMetal;
    }

    public String getAllomanticTitle() {
        return allomanticTitle;
    }

    public String getFeruchemicalTitle() {
        return feruchemicalTitle;
    }

    public AllomanticSource
    getAllomanticSource() {
        return allomanticSource;
    }

    public static Set<MetalType>
    getStandardMetals() {
        return STANDARD_METALS;
    }

    public static Optional<MetalType>
    fromSerializedName(
            String serializedName
    ) {
        if (serializedName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                BY_SERIALIZED_NAME.get(
                        serializedName.toLowerCase(
                                Locale.ROOT
                        )
                )
        );
    }
}