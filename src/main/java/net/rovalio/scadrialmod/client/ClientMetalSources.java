package net.rovalio.scadrialmod.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;

public final class ClientMetalSources {

    private static SyncMetalSourcesS2CPayload current;

    private ClientMetalSources() {
    }

    public static SyncMetalSourcesS2CPayload current() {
        return current;
    }

    public static void clear() {
        current = null;
    }

    public static void handleSync(
            SyncMetalSourcesS2CPayload payload,
            IPayloadContext context
    ) {
        current = payload;
    }
}