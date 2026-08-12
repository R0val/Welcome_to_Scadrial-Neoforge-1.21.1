package net.rovalio.scadrialmod.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.CosmereAPI.registry.CosmereRegistries;
import net.rovalio.CosmereAPI.registry.definition.PlanetDefinition;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.world.dimension.ScadrialDimensions;

public final class ScadrialPlanets {

    //Register data in Cosmere API

    public static final DeferredRegister<PlanetDefinition> PLANETS =
            DeferredRegister.create(
                    CosmereRegistries.PLANET_REGISTRY,
                    ScadrialMod.MOD_ID
            );


    //Defines Scadrial as a planet type object

    public static final DeferredHolder<PlanetDefinition, PlanetDefinition> SCADRIAL =
            PLANETS.register(
                    "scadrial",
                    () -> new PlanetDefinition(
                            ScadrialDimensions.SCADRIAL
                    )
            );


    private ScadrialPlanets() {
    }


    public static void register(IEventBus modEventBus) {
        PLANETS.register(modEventBus);
    }
}