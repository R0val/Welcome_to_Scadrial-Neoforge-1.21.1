package net.rovalio.scadrialmod.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.CosmereAPI.registry.CosmereRegistries;
import net.rovalio.CosmereAPI.registry.definition.ShardDefinition;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialShards {

    public static final DeferredRegister<ShardDefinition>
            SHARDS =
            DeferredRegister.create(
                    CosmereRegistries.SHARD_REGISTRY,
                    ScadrialMod.MOD_ID
            );

    public static final DeferredHolder<
            ShardDefinition,
            ShardDefinition
            > RUIN =
            SHARDS.register(
                    "ruin",
                    ShardDefinition::new
            );

    public static final DeferredHolder<
            ShardDefinition,
            ShardDefinition
            > PRESERVATION =
            SHARDS.register(
                    "preservation",
                    ShardDefinition::new
            );

    private ScadrialShards() {
    }

    public static void register(
            IEventBus modEventBus
    ) {
        SHARDS.register(modEventBus);
    }
}