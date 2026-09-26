package net.rovalio.scadrialmod.power;

import net.minecraft.server.level.ServerPlayer;
import net.rovalio.CosmereAPI.player.CosmereAttachments;
import net.rovalio.CosmereAPI.player.SpiritwebData;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;

import java.util.Objects;

final class AllomancySnappingManager {

    private static final double
            MISTBORN_SNAPPING_MIN_DAMAGE = 0.16;

    private static final double
            MISTBORN_SNAPPING_MAX_DAMAGE = 0.20;

    private static final double
            STANDARD_MISTING_SNAPPING_MIN_DAMAGE = 0.08;

    private static final double
            STANDARD_MISTING_SNAPPING_MAX_DAMAGE = 0.10;

    private AllomancySnappingManager() {
    }

    static boolean snap(
            ServerPlayer player
    ) {
        Objects.requireNonNull(
                player,
                "Player cannot be null"
        );

        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        if (!data.hasAllomancy()
                || data.isAllomancySnapped()) {

            return false;
        }

        SpiritwebData spiritweb =
                CosmereAttachments.get(player)
                        .getSpiritweb();

        double initialIntegrity =
                spiritweb.getIntegrity();

        // A completely broken Spiritweb cannot absorb another
        // valid, reversible Snapping modifier.
        if (initialIntegrity <= 0.0) {
            return false;
        }

        double rolledDamage =
                rollSnappingDamage(
                        player,
                        data
                );

        spiritweb.setIntegrity(
                initialIntegrity
                        - rolledDamage
        );

        // SpiritwebData clamps Integrity at zero. Store the
        // amount that was actually removed so a reset
        //can never restore more than this Snapping caused.
        double appliedDamage =
                initialIntegrity
                        - spiritweb.getIntegrity();

        data.recordAllomancySnapping(
                appliedDamage
        );

        return true;
    }

    private static double rollSnappingDamage(
            ServerPlayer player,
            ScadrialPlayerData data
    ) {
        double minimumDamage = data.isMistborn()
                ? MISTBORN_SNAPPING_MIN_DAMAGE
                : STANDARD_MISTING_SNAPPING_MIN_DAMAGE;

        double maximumDamage = data.isMistborn()
                ? MISTBORN_SNAPPING_MAX_DAMAGE
                : STANDARD_MISTING_SNAPPING_MAX_DAMAGE;

        return minimumDamage
                + player.getRandom().nextDouble()
                * (maximumDamage - minimumDamage);
    }
}
