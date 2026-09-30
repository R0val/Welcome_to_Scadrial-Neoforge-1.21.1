package net.rovalio.scadrialmod.equipment;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.rovalio.scadrialmod.item.ScadrialItems;

public final class EquipmentStorage {

    public static final int POUCH_SIZE = 8;
    public static final int BELT_SIZE = 9;
    public static final int POUCH_SLOT = 8;
    public static final int OFFHAND_SLOT = 40;
    public static final int BELT_EQUIPMENT_SLOT = 37;

    private EquipmentStorage() {}

    public static boolean isCoin(ItemStack stack) {
        return stack.is(ScadrialItems.COPPER_IMPERIAL.get())
                || stack.is(ScadrialItems.GOLD_IMPERIAL.get());
    }

    public static boolean isPouch(ItemStack stack) {
        return stack.is(ScadrialItems.COIN_POUCH.get());
    }

    public static boolean isBelt(ItemStack stack) {
        return stack.is(ScadrialItems.ALLOMANCER_TOOLBELT.get());
    }

    public static boolean isVial(ItemStack stack) {
        return stack.is(ScadrialItems.METAL_VIAL.get())
                || stack.is(ScadrialItems.WATER_METAL_VIAL.get())
                || stack.is(ScadrialItems.ALCOHOL_METAL_VIAL.get());
    }

    public static boolean accepts(
            boolean belt,
            int slot,
            ItemStack stack
    ) {
        int size = belt ? BELT_SIZE : POUCH_SIZE;

        if (slot < 0 || slot >= size) {
            return false;
        }

        if (!belt) {
            return isCoin(stack);
        }

        return slot == POUCH_SLOT
                ? isPouch(stack)
                : isVial(stack);
    }

    public static NonNullList<ItemStack> read(ItemStack owner) {
        int size = isBelt(owner) ? BELT_SIZE : POUCH_SIZE;

        NonNullList<ItemStack> items =
                NonNullList.withSize(size, ItemStack.EMPTY);

        owner.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        ).copyInto(items);

        return items;
    }

    public static void write(
            ItemStack owner,
            NonNullList<ItemStack> items
    ) {
        owner.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(items)
        );
    }

    public static int firstCoin(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return -1;
        }

        var items = read(pouch);

        for (int i = 0; i < items.size(); i++) {
            if (isCoin(items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static int findBelt(Player player) {
        return isBelt(
                player.getInventory().getItem(BELT_EQUIPMENT_SLOT)
        )
                ? BELT_EQUIPMENT_SLOT
                : -1;
    }

    public static int freeHand(Player player) {
        if (player.getMainHandItem().isEmpty()) {
            return player.getInventory().selected;
        }

        return player.getOffhandItem().isEmpty()
                ? OFFHAND_SLOT
                : -1;
    }
}