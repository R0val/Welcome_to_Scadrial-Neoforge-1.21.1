package net.rovalio.scadrialmod.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.function.Supplier;

public final class ScadrialRecipeSerializers {

    private static final DeferredRegister<RecipeSerializer<?>>
            SERIALIZERS =
            DeferredRegister.create(
                    Registries.RECIPE_SERIALIZER,
                    ScadrialMod.MOD_ID
            );

    public static final Supplier<
            RecipeSerializer<AllomanticContainerFillingRecipe>
            > ALLOMANTIC_CONTAINER_FILLING =
            SERIALIZERS.register(
                    "allomantic_container_filling",
                    () -> new SimpleCraftingRecipeSerializer<>(
                            AllomanticContainerFillingRecipe::new
                    )
            );

    private ScadrialRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}