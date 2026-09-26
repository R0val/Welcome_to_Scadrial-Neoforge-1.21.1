package net.rovalio.scadrialmod.power.allomancy;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rovalio.scadrialmod.network.ScadrialNetworking;
import net.rovalio.scadrialmod.network.ToggleAllomanticBurnC2SPayload;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;

public final class AllomancyBurnManager {

    public static final long DURALUMIN_CONSUMPTION_MULTIPLIER = 64L;
    public static final double DURALUMIN_STRENGTH_MULTIPLIER = 4.0;

    private AllomancyBurnManager() {
    }

    public static long passiveSubunitsPerTick(AllomanticFuel fuel) {
        return switch (fuel) {
            case IRON, STEEL, TIN, PEWTER,
                 ZINC, BRASS, COPPER, BRONZE,
                 CHROMIUM, NICROSIL, ALUMINIUM, DURALUMIN,
                 CADMIUM, BENDALLOY, GOLD, ELECTRUM,
                 ATIUM_ELECTRUM, MALATIUM -> 1L;
        };
    }

    public static long burnSubunitsPerTick(
            AllomanticFuel fuel,
            boolean duraluminActive
    ) {
        long base = passiveSubunitsPerTick(fuel);

        if (!duraluminActive
                || fuel == AllomanticFuel.DURALUMIN
                || fuel == AllomanticFuel.ALUMINIUM) {
            return base;
        }

        return Math.multiplyExact(
                base,
                DURALUMIN_CONSUMPTION_MULTIPLIER
        );
    }

    public static double effectiveStrength(
            double baseStrength,
            boolean duraluminActive
    ) {
        if (!Double.isFinite(baseStrength) || baseStrength <= 0.0) {
            return 0.0;
        }

        double multiplier = duraluminActive
                ? DURALUMIN_STRENGTH_MULTIPLIER
                : 1.0;

        return Math.min(
                Double.MAX_VALUE,
                baseStrength * multiplier
        );
    }

    public static double effectiveStrength(ServerPlayer player) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);

        if (!player.isAlive()
                || player.isSpectator()
                || !data.canUseAllomancy()) {
            return 0.0;
        }

        boolean duraluminActive = isUsableAndBurning(
                player, data, AllomanticFuel.DURALUMIN
        ) && !isUsableAndBurning(
                player, data, AllomanticFuel.ALUMINIUM
        );

        return effectiveStrength(
                data.getAllomancyStrength(),
                duraluminActive
        );
    }

    public static void handleToggle(
            ToggleAllomanticBurnC2SPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        AllomanticFuel.fromSerializedName(payload.fuel())
                .ifPresent(fuel -> toggle(player, fuel));
    }

    private static void toggle(
            ServerPlayer player,
            AllomanticFuel fuel
    ) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }

        ScadrialPlayerData data = ScadrialAttachments.get(player);

        if (data.isBurning(fuel)) {
            data.setBurning(fuel, false);
        } else if (ScadrialPowerManager.canUseAllomanticFuel(player, fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0L) {
            data.setBurning(fuel, true);
        }

        // El aluminio actúa inmediatamente al encenderlo.
        if (isUsableAndBurning(player, data, AllomanticFuel.ALUMINIUM)) {
            applyAluminium(data);
        }

        ScadrialNetworking.sync(player);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ScadrialPlayerData data = ScadrialAttachments.get(player);

        if (data.getBurningFuels().isEmpty()) {
            return;
        }

        boolean changed = false;

        // Validate every metal before starting its effects
        for (AllomanticFuel fuel : data.getBurningFuels()) {
            if (!player.isAlive()
                    || player.isSpectator()
                    || !isUsableAndBurning(player, data, fuel)) {
                data.setBurning(fuel, false);
                changed = true;
            }
        }

        // Aluminium has priority over every metal
        if (data.isBurning(AllomanticFuel.ALUMINIUM)) {
            changed |= applyAluminium(data);
        }

        boolean duraluminActive =
                data.isBurning(AllomanticFuel.DURALUMIN);

        for (AllomanticFuel fuel : data.getBurningFuels()) {
            long cost = burnSubunitsPerTick(
                    fuel,
                    duraluminActive
            );

            data.consumeAllomanticReserveSubunits(fuel, cost);

            if (data.getAllomanticReserveSubunits(fuel) == 0L) {
                data.setBurning(fuel, false);
                changed = true;
            }
        }

        if (changed || player.tickCount % 20 == 0) {
            ScadrialNetworking.sync(player);
        }
    }

    private static boolean isUsableAndBurning(
            ServerPlayer player,
            ScadrialPlayerData data,
            AllomanticFuel fuel
    ) {
        return data.isBurning(fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0L
                && ScadrialPowerManager.canUseAllomanticFuel(player, fuel);
    }

    private static boolean applyAluminium(ScadrialPlayerData data) {
        boolean changed = false;

        for (AllomanticFuel fuel : AllomanticFuel.values()) {
            if (fuel == AllomanticFuel.ALUMINIUM) {
                continue;
            }

            long reserve = data.getAllomanticReserveSubunits(fuel);

            if (reserve > 0L) {
                data.consumeAllomanticReserveSubunits(fuel, reserve);
                changed = true;
            }

            if (data.isBurning(fuel)) {
                data.setBurning(fuel, false);
                changed = true;
            }
        }

        return changed;
    }
}