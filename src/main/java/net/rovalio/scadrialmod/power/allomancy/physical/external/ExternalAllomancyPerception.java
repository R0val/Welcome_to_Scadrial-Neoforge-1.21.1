package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload.Target;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExternalAllomancyPerception {

    private static final int SCAN_INTERVAL = 5;
    private static final int BLOCK_SCAN_INTERVAL = 5;

    private static final Map<
            UUID,
            SyncMetalSourcesS2CPayload
            > LAST_SENT = new HashMap<>();

    private static final Map<UUID, BlockScan> LAST_BLOCK_SCAN =
            new HashMap<>();

    private record BlockScan(
            ResourceLocation dimension,
            long tick,
            double radius,
            Vec3 origin,
            List<BlockPos> positions
    ) {
    }

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
                AllomancyBurnManager.effectiveStrength(player);

        boolean duralumin = isUsableAndBurning(
                player, data, AllomanticFuel.DURALUMIN
        ) && !isUsableAndBurning(
                player, data, AllomanticFuel.ALUMINIUM
        );

        double radius = canPerceive(player, data)
                ? detectionRadius(strength, duralumin)
                : 0.0;

        List<Target> targets = radius > 0.0
                ? EntityMetalSources.find(
                        player,
                        radius,
                        SyncMetalSourcesS2CPayload.MAX_TARGETS
                ).stream()
                .map(source -> new Target(
                        source.entity().getId(),
                        source.entity().getUUID(),
                        source.part()
                ))
                .sorted(
                        Comparator.comparingInt(Target::entityId)
                                .thenComparing(
                                        target -> target.part()
                                                .serializedName()
                                )
                )
                .toList()
                : List.of();

        List<BlockPos> blocks = radius > 0.0
                ? nearbyBlocks(player, radius)
                : List.of();

        if (radius == 0.0) {
            LAST_BLOCK_SCAN.remove(player.getUUID());
        }

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

    private static List<BlockPos> nearbyBlocks(
            ServerPlayer player,
            double radius
    ) {
        UUID playerId = player.getUUID();
        long tick = player.serverLevel().getGameTime();
        Vec3 origin = MetalSourceDetector.chestPosition(player);
        ResourceLocation dimension =
                player.level().dimension().location();

        BlockScan scan = LAST_BLOCK_SCAN.get(playerId);

        if (scan == null
                || !scan.dimension().equals(dimension)
                || Double.compare(scan.radius(), radius) != 0
                || tick < scan.tick()
                || tick - scan.tick() >= BLOCK_SCAN_INTERVAL
                || scan.origin().distanceToSqr(origin) >= 16.0) {

            scan = new BlockScan(
                    dimension,
                    tick,
                    radius,
                    origin,
                    MetalSourceDetector.findMetalBlocks(
                            player,
                            radius,
                            SyncMetalSourcesS2CPayload.MAX_BLOCK_TARGETS
                    )
            );

            LAST_BLOCK_SCAN.put(playerId, scan);
        }

        return scan.positions().stream()
                .filter(position ->
                        MetalSourceDetector.isValidBlockTarget(
                                player,
                                position,
                                radius
                        )
                )
                .toList();
    }

    private static boolean canPerceive(
            ServerPlayer player,
            ScadrialPlayerData data
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && (isUsableAndBurning(
                player,
                data,
                AllomanticFuel.IRON
        ) || isUsableAndBurning(
                player,
                data,
                AllomanticFuel.STEEL
        ));
    }

    private static boolean isUsableAndBurning(
            ServerPlayer player,
            ScadrialPlayerData data,
            AllomanticFuel fuel
    ) {
        return data.isBurning(fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0L
                && ScadrialPowerManager.canUseAllomanticFuel(
                player,
                fuel
        );
    }

    private static double detectionRadius(double strength, boolean duralumin) {
        return ExternalAllomancyMath.radius(strength, duralumin);
    }

    public static SyncMetalSourcesS2CPayload current(ServerPlayer player) {
        return LAST_SENT.get(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        LAST_BLOCK_SCAN.remove(player.getUUID());
    }

    public static void clear() {
        LAST_SENT.clear();
        LAST_BLOCK_SCAN.clear();
    }
}