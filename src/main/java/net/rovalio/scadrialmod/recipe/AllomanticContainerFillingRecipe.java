package net.rovalio.scadrialmod.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.rovalio.scadrialmod.item.custom.AllomanticContainerItem;

public final class AllomanticContainerFillingRecipe
        extends CustomRecipe {

    public AllomanticContainerFillingRecipe(
            CraftingBookCategory category
    ) {
        super(category);
    }

    @Override
    public boolean matches(
            CraftingInput input,
            Level level
    ) {
        return !createResult(input).isEmpty();
    }

    @Override
    public ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries
    ) {
        return createResult(input);
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width * height >= 2;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(
            CraftingInput input
    ) {
        // Ninguno de los dos ingredientes vuelve a la cuadrícula.
        return NonNullList.withSize(
                input.size(),
                ItemStack.EMPTY
        );
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ScadrialRecipeSerializers
                .ALLOMANTIC_CONTAINER_FILLING
                .get();
    }

    private static ItemStack createResult(
            CraftingInput input
    ) {
        ItemStack container = ItemStack.EMPTY;
        ItemStack metal = ItemStack.EMPTY;
        int occupiedSlots = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;

            if (occupiedSlots > 2) {
                return ItemStack.EMPTY;
            }

            if (stack.getItem()
                    instanceof AllomanticContainerItem) {
                container = stack;
            } else {
                metal = stack;
            }
        }

        if (occupiedSlots != 2
                || container.isEmpty()
                || metal.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return AllomanticContainerItem.fillForCrafting(
                container,
                metal
        );
    }
}