package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload.Target;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExternalAllomancyPerception {

    private static final int SCAN_INTERVAL = 5;

    private static final Comparator<Target> TARGET_ORDER =
            Comparator.comparingInt(Target::entityId)
                    .thenComparingInt(target -> target.part().ordinal());

    private static final Map<
            UUID,
            SyncMetalSourcesS2CPayload
            > LAST_SENT = new HashMap<>();

    private ExternalAllomancyPerception() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ScadrialPlayerData data = ScadrialAttachments.get(player);

        if (!canPerceive(player, data)) {
            SyncMetalSourcesS2CPayload previous =
                    LAST_SENT.get(player.getUUID());

            if (previous != null && previous.radius() > 0.0) {
                sync(player);
            }
            return;
        }

        if (!LAST_SENT.containsKey(player.getUUID())
                || Math.floorMod(
                player.tickCount + player.getId(),
                SCAN_INTERVAL
        ) == 0) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);

        double strength =
                AllomancyBurnManager.effectiveStrength(player, data);

        boolean duralumin =
                data.isUsableAndBurning(AllomanticFuel.DURALUMIN)
                        && !data.isUsableAndBurning(AllomanticFuel.ALUMINIUM);

        double radius = canPerceive(player, data)
                ? ExternalAllomancyMath.radius(strength, duralumin)
                : 0.0;

        List<Target> targets = radius > 0.0
                ? nearbyTargets(player, radius)
                : List.of();

        List<BlockPos> blocks = radius > 0.0
                ? nearbyBlocks(player, radius)
                : List.of();

        SyncMetalSourcesS2CPayload update =
                new SyncMetalSourcesS2CPayload(
                        player.level().dimension().location(),
                        radius,
                        targets,
                        blocks
                );

        if (!update.equals(
                LAST_SENT.put(player.getUUID(), update)
        )) {
            PacketDistributor.sendToPlayer(player, update);
        }
    }

    private static List<Target> nearbyTargets(
            ServerPlayer player,
            double radius
    ) {
        List<EntityMetalSources.Source> sources = EntityMetalSources.find(
                player,
                radius,
                SyncMetalSourcesS2CPayload.MAX_TARGETS
        );

        List<Target> targets = new ArrayList<>(sources.size());

        for (EntityMetalSources.Source source : sources) {
            targets.add(new Target(
                    source.entity().getId(),
                    source.entity().getUUID(),
                    source.part()
            ));
        }

        targets.sort(TARGET_ORDER);
        return targets;
    }

    private static List<BlockPos> nearbyBlocks(
            ServerPlayer player,
            double radius
    ) {
        List<BlockPos> found = MetalSourceDetector.findMetalBlocks(
                player,
                radius,
                SyncMetalSourcesS2CPayload.MAX_BLOCK_TARGETS
        );

        List<BlockPos> valid = new ArrayList<>(found.size());

        for (BlockPos position : found) {
            if (MetalSourceDetector.isValidBlockTarget(
                    player,
                    position,
                    radius
            )) {
                valid.add(position);
            }
        }

        return valid;
    }

    private static boolean canPerceive(
            ServerPlayer player,
            ScadrialPlayerData data
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && (data.isUsableAndBurning(AllomanticFuel.IRON)
                || data.isUsableAndBurning(AllomanticFuel.STEEL));
    }

    public static SyncMetalSourcesS2CPayload current(ServerPlayer player) {
        return LAST_SENT.get(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
    }

    public static void clear() {
        LAST_SENT.clear();
    }
}
