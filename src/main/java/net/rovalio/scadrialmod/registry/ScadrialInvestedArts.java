package net.rovalio.scadrialmod.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.CosmereAPI.registry.CosmereRegistries;
import net.rovalio.CosmereAPI.registry.definition.InvestedArtDefinition;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialInvestedArts {

    public static final DeferredRegister<
            InvestedArtDefinition
            > INVESTED_ARTS =
            DeferredRegister.create(
                    CosmereRegistries
                            .INVESTED_ART_REGISTRY,
                    ScadrialMod.MOD_ID
            );

    public static final DeferredHolder<
            InvestedArtDefinition,
            InvestedArtDefinition
            > ALLOMANCY =
            INVESTED_ARTS.register(
                    "allomancy",
                    InvestedArtDefinition::new
            );

    public static final DeferredHolder<
            InvestedArtDefinition,
            InvestedArtDefinition
            > FERUCHEMY =
            INVESTED_ARTS.register(
                    "feruchemy",
                    InvestedArtDefinition::new
            );

    public static final DeferredHolder<
            InvestedArtDefinition,
            InvestedArtDefinition
            > HEMALURGY =
            INVESTED_ARTS.register(
                    "hemalurgy",
                    InvestedArtDefinition::new
            );

    private ScadrialInvestedArts() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        INVESTED_ARTS.register(modEventBus);
    }
}