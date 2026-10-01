package net.rovalio.scadrialmod.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.network.SyncAllomancyStateS2CPayload;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ClientAllomancyState {

    public record Snapshot(
            boolean assignmentComplete,
            PowerProfile profile,
            MetalType metal,
            boolean snapped,
            double strength,
            Map<AllomanticFuel, Long> reservesSubunits
    ) {
        public Snapshot {
            Objects.requireNonNull(
                    profile,
                    "Profile cannot be null"
            );
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

        public boolean hasPower(MetalType candidate) {
            return assignmentComplete && switch (profile) {
                case NONE -> false;
                case SINGLE -> metal == candidate;
                case FULL -> true;
            };
        }

        public boolean canUse(MetalType candidate) {
            return hasPower(candidate)
                    && snapped
                    && strength > 0.0;
        }

        public boolean isDuraluminBoostActive() {
            return isUsableAndBurning(AllomanticFuel.DURALUMIN)
                    && !isUsableAndBurning(AllomanticFuel.ALUMINIUM);
        }

        public double effectiveStrength() {
            return AllomancyBurnManager.effectiveStrength(
                    strength,
                    isDuraluminBoostActive()
            );
        }

        public long burnSubunitsPerTick(AllomanticFuel fuel) {
            return Math.round(
                    preciseBurnSubunitsPerTick(fuel)
            );
        }

        public double preciseBurnSubunitsPerTick(AllomanticFuel fuel) {
            if (!isUsableAndBurning(fuel)) {
                return 0.0;
            }

            if (fuel == AllomanticFuel.CADMIUM
                    || fuel == AllomanticFuel.BENDALLOY) {
                return ClientTemporalAllomancy.subunitsPerTick(fuel);
            }

            return AllomancyBurnManager.burnSubunitsPerTick(
                    fuel,
                    isDuraluminBoostActive()
            );
        }

        private boolean isUsableAndBurning(AllomanticFuel fuel) {
            return canUse(fuel.getRequiredPower())
                    && ClientAllomancyBurnState.isBurning(fuel)
                    && getReserveSubunits(fuel) > 0L;
        }

        public long getReserveSubunits(AllomanticFuel fuel) {
            Objects.requireNonNull(
                    fuel,
                    "Allomantic fuel cannot be null"
            );

            return reservesSubunits.getOrDefault(fuel, 0L);
        }
    }

    private static final Snapshot EMPTY =
            new Snapshot(
                    false,
                    PowerProfile.NONE,
                    null,
                    false,
                    0.0,
                    Map.of()
            );

    private static Snapshot current = EMPTY;
    private static boolean received;

    private ClientAllomancyState() {
    }

    public static Snapshot current() {
        return current;
    }

    public static boolean hasReceivedState() {
        return received;
    }

    public static void clear() {
        current = EMPTY;
        received = false;
    }

    public static void handleSync(
            SyncAllomancyStateS2CPayload payload,
            IPayloadContext context
    ) {
        PowerProfile profile =
                PowerProfile.fromSerializedName(
                        payload.profile()
                ).orElse(PowerProfile.NONE);

        MetalType metal =
                MetalType.fromSerializedName(
                        payload.metal()
                ).orElse(null);

        if (!payload.assignmentComplete()) {
            current = new Snapshot(
                    false,
                    PowerProfile.NONE,
                    null,
                    false,
                    0.0,
                    payload.reservesSubunits()
            );

        } else if ((profile == PowerProfile.SINGLE
                && metal == null)
                || !Double.isFinite(payload.strength())
                || payload.strength() < 0.0) {

            current = EMPTY;

        } else {
            current = new Snapshot(
                    true,
                    profile,
                    profile == PowerProfile.SINGLE ? metal : null,
                    payload.snapped(),
                    payload.strength(),
                    payload.reservesSubunits()
            );
        }

        received = true;
    }
}