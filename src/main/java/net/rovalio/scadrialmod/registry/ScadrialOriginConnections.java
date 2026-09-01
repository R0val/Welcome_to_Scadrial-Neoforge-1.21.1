package net.rovalio.scadrialmod.registry;

import net.minecraft.resources.ResourceLocation;
import net.rovalio.CosmereAPI.player.ConnectionData;
import net.rovalio.CosmereAPI.player.ConnectionType;
import net.rovalio.CosmereAPI.registry.OriginConnectionRegistry;

public final class ScadrialOriginConnections {

    //Temporary technical baseline.
    //It only represents the existence of the Connection, not a canonical or balanced strength.
    private static final int SCADRIAL_INITIAL_CONNECTION = 16;

    private static final int RUIN_INITIAL_CONNECTION = 15;

    private static final int PRESERVATION_INITIAL_CONNECTION = 16;

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
                        SCADRIAL_INITIAL_CONNECTION
                ),

                new ConnectionData(
                        ConnectionType.SHARD,
                        ScadrialShards.RUIN
                                .getId()
                                .toString(),
                        RUIN_INITIAL_CONNECTION
                ),

                new ConnectionData(
                        ConnectionType.SHARD,
                        ScadrialShards.PRESERVATION
                                .getId()
                                .toString(),
                        PRESERVATION_INITIAL_CONNECTION
                )
        );
    }
}