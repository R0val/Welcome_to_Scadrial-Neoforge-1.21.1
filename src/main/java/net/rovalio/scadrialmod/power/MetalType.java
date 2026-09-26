package net.rovalio.scadrialmod.power;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * The sixteen standard Metallic Arts power families.
 *
 * This enum identifies a character's basic powers.
 */
public enum MetalType implements StringRepresentable {

    IRON("iron", "Lurcher", "Skimmer"),
    STEEL("steel", "Coinshot", "Steelrunner"),
    TIN("tin", "Tineye", "Windwhisperer"),
    PEWTER("pewter", "Thug", "Brute"),

    ZINC("zinc", "Rioter", "Sparker"),
    BRASS("brass", "Soother", "Firesoul"),
    COPPER("copper", "Smoker", "Archivist"),
    BRONZE("bronze", "Seeker", "Sentry"),

    CHROMIUM("chromium", "Leecher", "Spinner"),
    NICROSIL("nicrosil", "Nicroburst", "Soulbearer"),
    ALUMINIUM("aluminium", "Aluminum Gnat", "Trueself"),
    DURALUMIN("duralumin", "Duralumin Gnat", "Connector"),

    CADMIUM("cadmium", "Pulser", "Gasper"),
    BENDALLOY("bendalloy", "Slider", "Subsumer"),
    GOLD("gold", "Augur", "Bloodmaker"),
    ELECTRUM("electrum", "Oracle", "Pinnacle");

    private static final Map<String, MetalType> BY_SERIALIZED_NAME =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            MetalType::getSerializedName,
                            metal -> metal
                    ));

    private static final Set<MetalType> STANDARD_METALS =
            Collections.unmodifiableSet(
                    EnumSet.allOf(MetalType.class)
            );

    private final String serializedName;
    private final String allomanticTitle;
    private final String feruchemicalTitle;

    MetalType(
            String serializedName,
            String allomanticTitle,
            String feruchemicalTitle
    ) {
        this.serializedName =
                Objects.requireNonNull(serializedName);

        this.allomanticTitle =
                Objects.requireNonNull(allomanticTitle);

        this.feruchemicalTitle =
                Objects.requireNonNull(feruchemicalTitle);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String getAllomanticTitle() {
        return allomanticTitle;
    }

    public String getFeruchemicalTitle() {
        return feruchemicalTitle;
    }

    public static Set<MetalType> getStandardMetals() {
        return STANDARD_METALS;
    }

    public static Optional<MetalType> fromSerializedName(
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