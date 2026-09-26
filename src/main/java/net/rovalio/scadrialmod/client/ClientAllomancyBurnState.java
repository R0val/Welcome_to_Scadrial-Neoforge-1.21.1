package net.rovalio.scadrialmod.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.network.SyncAllomanticBurnsS2CPayload;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.EnumSet;
import java.util.Set;

public final class ClientAllomancyBurnState {

    private static Set<AllomanticFuel> burning = Set.of();
    private static boolean received;

    private ClientAllomancyBurnState() {
    }

    public static boolean isBurning(AllomanticFuel fuel) {
        return burning.contains(fuel);
    }

    public static boolean hasReceivedState() {
        return received;
    }

    public static void clear() {
        burning = Set.of();
        received = false;
    }

    public static void handleSync(
            SyncAllomanticBurnsS2CPayload payload,
            IPayloadContext context
    ) {
        EnumSet<AllomanticFuel> updated =
                EnumSet.noneOf(AllomanticFuel.class);

        for (String name : payload.fuels()) {
            AllomanticFuel.fromSerializedName(name)
                    .ifPresent(updated::add);
        }

        burning = Set.copyOf(updated);
        received = true;
    }
}