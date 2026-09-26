package net.rovalio.scadrialmod.item.custom;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialDataComponents {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(ScadrialMod.MOD_ID);

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<AllomanticContainerContents>
            > ALLOMANTIC_CONTENTS =
            COMPONENTS.registerComponentType(
                    "allomantic_contents",
                    builder -> builder.persistent(
                            AllomanticContainerContents.CODEC
                    )
            );

    private ScadrialDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}