package net.rovalio.scadrialmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record SyncAllomancyStateS2CPayload(
        boolean assignmentComplete,
        String profile,
        String metal,
        boolean snapped,
        double strength,
        Map<AllomanticFuel, Long> reservesSubunits
) implements CustomPacketPayload {

    private static final StreamCodec<ByteBuf, AllomanticFuel>
            ALLOMANTIC_FUEL_CODEC =
            ByteBufCodecs.STRING_UTF8.map(serializedName ->
                    AllomanticFuel.fromSerializedName(serializedName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown allomantic fuel: "
                                                + serializedName
                                )
                        ),
                    AllomanticFuel::getSerializedName
            );

    private static final StreamCodec<
            ByteBuf,
            Map<AllomanticFuel, Long>
            > RESERVES_CODEC =
            ByteBufCodecs.map(
                    ignoredSize ->
                            new EnumMap<AllomanticFuel, Long>(
                                    AllomanticFuel.class
                            ),
                    ALLOMANTIC_FUEL_CODEC,
                    ByteBufCodecs.VAR_LONG,
                    AllomanticFuel.values().length
            );

    public static final Type<SyncAllomancyStateS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "allomancy_state"
            ));

    public static final StreamCodec<
            ByteBuf,
            SyncAllomancyStateS2CPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SyncAllomancyStateS2CPayload::assignmentComplete,

                    ByteBufCodecs.STRING_UTF8,
                    SyncAllomancyStateS2CPayload::profile,

                    ByteBufCodecs.STRING_UTF8,
                    SyncAllomancyStateS2CPayload::metal,

                    ByteBufCodecs.BOOL,
                    SyncAllomancyStateS2CPayload::snapped,

                    ByteBufCodecs.DOUBLE,
                    SyncAllomancyStateS2CPayload::strength,

                    RESERVES_CODEC,
                    SyncAllomancyStateS2CPayload::reservesSubunits,

                    SyncAllomancyStateS2CPayload::new
            );

    public SyncAllomancyStateS2CPayload {
        Objects.requireNonNull(profile, "Profile cannot be null");
        Objects.requireNonNull(metal, "Metal cannot be null");
        Objects.requireNonNull(
                reservesSubunits,
                "Reserve map cannot be null"
        );

        EnumMap<AllomanticFuel, Long> copy =
                new EnumMap<>(AllomanticFuel.class);

        reservesSubunits.forEach((fuel, amount) -> {
            Objects.requireNonNull(
                    fuel,
                    "Allomantic fuel cannot be null"
            );
            Objects.requireNonNull(
                    amount,
                    "Reserve amount cannot be null"
            );

            if (amount < 0L) {
                throw new IllegalArgumentException(
                        "Reserve amount cannot be negative"
                );
            }

            if (amount > 0L) {
                copy.put(fuel, amount);
            }
        });

        reservesSubunits =
                Collections.unmodifiableMap(copy);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}