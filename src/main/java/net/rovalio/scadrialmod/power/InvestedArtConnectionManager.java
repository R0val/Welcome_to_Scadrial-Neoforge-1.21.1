package net.rovalio.scadrialmod.power;

import net.minecraft.resources.ResourceLocation;
import net.rovalio.CosmereAPI.player.ConnectionData;
import net.rovalio.CosmereAPI.player.ConnectionType;
import net.rovalio.CosmereAPI.player.SpiritwebData;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.registry.ScadrialShards;

import java.util.Objects;

final class InvestedArtConnectionManager {

    /* Aún faltan por añadir Armonía y a lo mejor Discordia en era 3
     * Por favor Sando, haz que Kelsier sea Discordia.
     * En verda, lo guapo que está poder escribir un rato en español
     */

    private static final int
            MISTING_SHARD_CONNECTION = 32;

    private static final int
            MISTBORN_PRESERVATION_CONNECTION = 64;

    private static final int
            FERRING_PRESERVATION_CONNECTION = 24;

    private static final int
            FERRING_RUIN_CONNECTION = 23;

    private static final int
            FERUCHEMIST_PRESERVATION_CONNECTION = 40;

    private static final int
            FERUCHEMIST_RUIN_CONNECTION = 39;

    private InvestedArtConnectionManager() {
    }

    static void reconcile(
            ScadrialPlayerData data,
            SpiritwebData spiritweb
    ) {
        Objects.requireNonNull(
                data,
                "Scadrial player data cannot be null"
        );

        Objects.requireNonNull(
                spiritweb,
                "Spiritweb cannot be null"
        );

        reconcileAllomancy(
                data,
                spiritweb
        );

        reconcileFeruchemy(
                data,
                spiritweb
        );
    }

    private static void reconcileAllomancy(
            ScadrialPlayerData data,
            SpiritwebData spiritweb
    ) {
        switch (data.getAllomanticProfile()) {
            case NONE -> {
            }

            case SINGLE -> ensureMinimumShardConnection(
                    spiritweb,
                    ScadrialShards.PRESERVATION.getId(),
                    MISTING_SHARD_CONNECTION
            );

            case FULL -> ensureMinimumShardConnection(
                    spiritweb,
                    ScadrialShards.PRESERVATION.getId(),
                    MISTBORN_PRESERVATION_CONNECTION
            );
        }
    }

    private static void reconcileFeruchemy(
            ScadrialPlayerData data,
            SpiritwebData spiritweb
    ) {
        switch (data.getFeruchemicalProfile()) {

            case NONE -> {}

            case SINGLE -> {
                ensureMinimumShardConnection(
                        spiritweb,
                        ScadrialShards.PRESERVATION
                                .getId(),
                        FERRING_PRESERVATION_CONNECTION
                );

                ensureMinimumShardConnection(
                        spiritweb,
                        ScadrialShards.RUIN
                                .getId(),
                        FERRING_RUIN_CONNECTION
                );
            }

            case FULL -> {
                ensureMinimumShardConnection(
                        spiritweb,
                        ScadrialShards.PRESERVATION
                                .getId(),
                        FERUCHEMIST_PRESERVATION_CONNECTION
                );

                ensureMinimumShardConnection(
                        spiritweb,
                        ScadrialShards.RUIN
                                .getId(),
                        FERUCHEMIST_RUIN_CONNECTION
                );
            }
        }
    }

    private static void ensureMinimumShardConnection(
            SpiritwebData spiritweb,
            ResourceLocation shardId,
            int minimumStrength
    ) {
        String target =
                shardId.toString();

        ConnectionData currentConnection =
                spiritweb.getConnection(
                        ConnectionType.SHARD,
                        target
                );

        int currentStrength =
                currentConnection == null
                        ? ConnectionData.MIN_STRENGTH
                        : currentConnection.strength();

        if (currentStrength >= minimumStrength) {
            return;
        }

        spiritweb.setConnection(
                ConnectionType.SHARD,
                target,
                minimumStrength
        );
    }
}