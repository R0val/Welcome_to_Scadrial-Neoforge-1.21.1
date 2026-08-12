package net.rovalio.scadrialmod.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.CosmereAPI.registry.CosmereRegistries;
import net.rovalio.CosmereAPI.registry.definition.OriginDefinition;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialOrigins {

    //Register data in Cosmere API

    public static final DeferredRegister<OriginDefinition> ORIGINS =
            DeferredRegister.create(
                    CosmereRegistries.ORIGIN_REGISTRY,
                    ScadrialMod.MOD_ID
            );


    //Scadrial origins: Terris, Noble and Skaa

    public static final DeferredHolder<OriginDefinition, OriginDefinition> TERRIS =
            ORIGINS.register(
                    "terris",
                    () -> new OriginDefinition(
                            ScadrialPlanets.SCADRIAL.getKey()
                    )
            );

    public static final DeferredHolder<OriginDefinition, OriginDefinition> NOBLE =
            ORIGINS.register(
                    "noble",
                    () -> new OriginDefinition(
                            ScadrialPlanets.SCADRIAL.getKey()
                    )
            );

    public static final DeferredHolder<OriginDefinition, OriginDefinition> SKAA =
            ORIGINS.register(
                    "skaa",
                    () -> new OriginDefinition(
                            ScadrialPlanets.SCADRIAL.getKey()
                    )
            );


    private ScadrialOrigins() {
    }


    public static void register(IEventBus modEventBus) {
        ORIGINS.register(modEventBus);
    }
}