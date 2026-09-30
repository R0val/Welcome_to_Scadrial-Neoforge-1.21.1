package net.rovalio.scadrialmod.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.rovalio.scadrialmod.equipment.EquipmentMenu;

public final class EquipmentScreen
        extends AbstractContainerScreen<EquipmentMenu> {

    public EquipmentScreen(
            EquipmentMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        imageWidth = 176;
        imageHeight = 136;
        inventoryLabelY = 42;
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF40382E
        );

        graphics.fill(
                leftPos + 2,
                topPos + 2,
                leftPos + imageWidth - 2,
                topPos + imageHeight - 2,
                0xFFD2C5AC
        );

        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;

            graphics.fill(
                    x - 1,
                    y - 1,
                    x + 17,
                    y + 17,
                    0xFF574E41
            );

            graphics.fill(
                    x,
                    y,
                    x + 16,
                    y + 16,
                    0xFF9B8D76
            );
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}