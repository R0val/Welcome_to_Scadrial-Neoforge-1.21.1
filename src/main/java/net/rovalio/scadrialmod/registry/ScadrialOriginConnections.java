package net.rovalio.scadrialmod.registry;

import net.minecraft.resources.ResourceLocation;
import net.rovalio.CosmereAPI.player.ConnectionData;
import net.rovalio.CosmereAPI.player.ConnectionType;
import net.rovalio.CosmereAPI.registry.OriginConnectionRegistry;

public final class ScadrialOriginConnections {

    //Temporary technical baseline.
    //It only represents the existence of the Connection, not a canonical or balanced strength.
    private static final int INITIAL_CONNECTION_STRENGTH =
            ConnectionData.MIN_STRENGTH + 1;

    private ScadrialOriginConnections() {
    }

    public static void register() {

        registerOriginConnections(
                ScadrialOrigins.NOBLE.getId()
        );

        registerOriginConnections(
                ScadrialOrigins.SKAA.getId()
        );

        registerOriginConnections(
                ScadrialOrigins.TERRIS.getId()
        );
    }

    private static void registerOriginConnections(
            ResourceLocation originId
    ) {
        OriginConnectionRegistry.register(
                originId,

                new ConnectionData(
                        ConnectionType.PLANET,
                        ScadrialPlanets.SCADRIAL
                                .getId()
                                .toString(),
                        INITIAL_CONNECTION_STRENGTH
                ),

                new ConnectionData(
                        ConnectionType.SHARD,
                        ScadrialShards.RUIN
                                .getId()
                                .toString(),
                        INITIAL_CONNECTION_STRENGTH
                ),

                new ConnectionData(
                        ConnectionType.SHARD,
                        ScadrialShards.PRESERVATION
                                .getId()
                                .toString(),
                        INITIAL_CONNECTION_STRENGTH
                )
        );
    }
}