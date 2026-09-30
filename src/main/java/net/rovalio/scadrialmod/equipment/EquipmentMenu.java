package net.rovalio.scadrialmod.equipment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class EquipmentMenu extends AbstractContainerMenu {

    private final Inventory inventory;
    private final ItemStack owner;

    private final int ownerSlot;
    private final boolean belt;
    private final int storageSize;

    public EquipmentMenu(
            int id,
            Inventory inventory,
            RegistryFriendlyByteBuf buffer
    ) {
        this(
                id,
                inventory,
                buffer.readVarInt(),
                buffer.readBoolean(),
                false
        );
    }

    public EquipmentMenu(
            int id,
            Inventory inventory,
            int ownerSlot,
            boolean belt,
            boolean server
    ) {
        super(AllomanticEquipment.MENU.get(), id);

        this.inventory = inventory;
        this.ownerSlot = ownerSlot;
        this.belt = belt;
        this.storageSize = belt
                ? EquipmentStorage.BELT_SIZE
                : EquipmentStorage.POUCH_SIZE;
        this.owner = server
                ? inventory.getItem(ownerSlot)
                : ItemStack.EMPTY;

        SimpleContainer storage = new SimpleContainer(storageSize) {
            @Override
            public void setChanged() {
                super.setChanged();

                if (server && inventory.getItem(ownerSlot) == owner) {
                    EquipmentStorage.write(owner, getItems());
                    inventory.setChanged();
                }
            }
        };

        if (server) {
            var contents = EquipmentStorage.read(owner);

            for (int i = 0; i < storageSize; i++) {
                storage.setItem(i, contents.get(i));
            }
        }

        for (int i = 0; i < storageSize; i++) {
            final int pocket = i;

            addSlot(new Slot(
                    storage,
                    i,
                    (belt ? 8 : 17) + i * 18,
                    20
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return EquipmentStorage.accepts(
                            belt,
                            pocket,
                            stack
                    );
                }

                @Override
                public int getMaxStackSize() {
                    return belt ? 1 : 64;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addPlayerSlot(
                        9 + row * 9 + column,
                        8 + column * 18,
                        52 + row * 18
                );
            }
        }

        for (int i = 0; i < 9; i++) {
            addPlayerSlot(i, 8 + i * 18, 110);
        }
    }

    private void addPlayerSlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != ownerSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != ownerSlot;
            }
        });
    }

    public boolean isBelt() {
        return belt;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide()
                || (player.isAlive()
                && inventory.getItem(ownerSlot) == owner);
    }

    @Override
    public void clicked(
            int slot,
            int button,
            ClickType type,
            Player player
    ) {
        if (!stillValid(player)) {
            return;
        }

        if (type == ClickType.SWAP && button == ownerSlot) {
            return;
        }

        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player)
                || index < 0
                || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean moved = index < storageSize
                ? moveItemStackTo(
                stack,
                storageSize,
                slots.size(),
                true
        )
                : moveItemStackTo(
                stack,
                0,
                storageSize,
                false
        );

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return original;
    }
}