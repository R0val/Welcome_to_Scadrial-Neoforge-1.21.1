package net.rovalio.scadrialmod.client;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;
import net.rovalio.scadrialmod.power.allomancy.physical.external.MetalTarget;

import java.util.ArrayList;
import java.util.List;

public final class ClientMetalSources {

    private static SyncMetalSourcesS2CPayload current;
    private static List<MetalTarget> targets = List.of();

    private ClientMetalSources() {
    }

    public static SyncMetalSourcesS2CPayload current() {
        return current;
    }

    public static List<MetalTarget> targets() {
        return targets;
    }

    public static void clear() {
        current = null;
        targets = List.of();
    }

    public static void handleSync(
            SyncMetalSourcesS2CPayload payload,
            IPayloadContext context
    ) {
        List<MetalTarget> resolved = new ArrayList<>(
                payload.targets().size() + payload.blocks().size()
        );

        for (SyncMetalSourcesS2CPayload.Target target : payload.targets()) {
            resolved.add(MetalTarget.of(target));
        }

        for (BlockPos block : payload.blocks()) {
            resolved.add(MetalTarget.of(block));
        }

        current = payload;
        targets = List.copyOf(resolved);
    }
}
