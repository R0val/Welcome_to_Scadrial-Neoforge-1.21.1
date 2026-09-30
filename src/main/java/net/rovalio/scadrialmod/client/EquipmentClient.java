package net.rovalio.scadrialmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.screen.EquipmentScreen;
import net.rovalio.scadrialmod.equipment.AllomanticEquipment;
import net.rovalio.scadrialmod.equipment.EquipmentNetworking;
import net.rovalio.scadrialmod.equipment.EquipmentStorage;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        value = Dist.CLIENT
)
public final class EquipmentClient {

    private static EquipmentNetworking.Selection selected =
            new EquipmentNetworking.Selection(-1, -1, -1);

    private static ClientLevel lastLevel;
    private static boolean pendingSelection;

    private EquipmentClient() {}

    public static void selection(
            EquipmentNetworking.Selection value
    ) {
        selected = value;
        pendingSelection = value.pocket() >= 0;
    }

    public static void projectileMotion(
            EquipmentNetworking.Motion motion
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null
                || !(mc.level.getEntity(motion.id())
                instanceof AbstractArrow arrow)) {
            return;
        }

        arrow.setPos(motion.position());
        arrow.setDeltaMovement(motion.velocity());
        arrow.inGround = motion.embedded();

        if (!motion.embedded()) {
            arrow.inGroundTime = 0;
            arrow.shakeTime = 0;
        }
    }

    private static boolean active(Minecraft mc) {
        return mc.player != null
                && mc.level != null
                && mc.screen == null
                && mc.isWindowActive()
                && !mc.isPaused()
                && mc.player.isAlive()
                && !mc.player.isSpectator();
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(
                new EquipmentNetworking.Action(action)
        );
    }

    private static void select(int slot) {
        pendingSelection = true;
        send(slot);
    }

    private static void stow() {
        if (pendingSelection || selected.pocket() >= 0) {
            send(EquipmentNetworking.STOW);
        }

        pendingSelection = false;
    }

    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        if (!active(mc)
                || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (mc.options.keyInventory.matches(
                event.getKey(),
                event.getScanCode()
        )) {
            if (Screen.hasShiftDown()
                    && EquipmentStorage.findBelt(mc.player) >= 0) {

                while (mc.options.keyInventory.consumeClick()) {
                    // Suppress the ordinary inventory screen.
                }

                mc.options.keyInventory.setDown(false);
                send(EquipmentNetworking.OPEN_BELT);
            } else {
                stow();
            }

            return;
        }

        for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
            var key = mc.options.keyHotbarSlots[i];

            if (!key.matches(event.getKey(), event.getScanCode())) {
                continue;
            }

            if (Screen.hasAltDown()
                    && EquipmentStorage.findBelt(mc.player) >= 0) {

                while (key.consumeClick()) {
                    // Suppress ordinary hotbar selection.
                }

                key.setDown(false);
                select(i);
            } else {
                stow();
            }

            return;
        }
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (!active(mc)
                || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (mc.options.keyInventory.matchesMouse(
                event.getButton()
        )) {
            if (Screen.hasShiftDown()
                    && EquipmentStorage.findBelt(mc.player) >= 0) {
                event.setCanceled(true);
                send(EquipmentNetworking.OPEN_BELT);
            } else {
                stow();
            }

            return;
        }

        for (int i = 0; i < mc.options.keyHotbarSlots.length; i++) {
            if (!mc.options.keyHotbarSlots[i]
                    .matchesMouse(event.getButton())) {
                continue;
            }

            if (Screen.hasAltDown()
                    && EquipmentStorage.findBelt(mc.player) >= 0) {
                event.setCanceled(true);
                select(i);
            } else {
                stow();
            }

            return;
        }
    }

    @SubscribeEvent
    public static void scroll(
            InputEvent.MouseScrollingEvent event
    ) {
        if (active(Minecraft.getInstance())) {
            stow();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != lastLevel
                || mc.player == null
                || !mc.player.isAlive()) {

            selected = new EquipmentNetworking.Selection(
                    -1,
                    -1,
                    -1
            );

            pendingSelection = false;
            lastLevel = mc.level;
        }
    }

    private static void renderHotbar(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || mc.options.hideGui
                || mc.player.isSpectator()) {
            return;
        }

        int beltSlot = EquipmentStorage.findBelt(mc.player);

        if (beltSlot < 0) {
            return;
        }

        var contents = EquipmentStorage.read(
                mc.player.getInventory().getItem(beltSlot)
        );

        int left = (graphics.guiWidth() - 180) / 2;
        int top = graphics.guiHeight() - 80;

        for (int i = 0; i < EquipmentStorage.BELT_SIZE; i++) {
            int x = left + i * 20;

            boolean drawn = selected.belt() == beltSlot
                    && selected.pocket() == i;

            ItemStack stack = contents.get(i);

            if (drawn
                    && selected.hand() >= 0
                    && selected.hand() < mc.player.getInventory()
                    .getContainerSize()) {
                stack = mc.player.getInventory().getItem(
                        selected.hand()
                );
            }

            graphics.fill(
                    x,
                    top,
                    x + 20,
                    top + 20,
                    drawn ? 0xFFE0B559 : 0xCC6A6051
            );

            graphics.fill(
                    x + 1,
                    top + 1,
                    x + 19,
                    top + 19,
                    0xDD211F1B
            );

            graphics.renderItem(stack, x + 2, top + 2);

            graphics.renderItemDecorations(
                    mc.font,
                    stack,
                    x + 2,
                    top + 2
            );
        }
    }

    @EventBusSubscriber(
            modid = ScadrialMod.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class Registration {

        private Registration() {}

        @SubscribeEvent
        public static void screens(
                RegisterMenuScreensEvent event
        ) {
            event.register(
                    AllomanticEquipment.MENU.get(),
                    EquipmentScreen::new
            );
        }

        @SubscribeEvent
        public static void renderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            event.registerEntityRenderer(
                    AllomanticEquipment.COIN.get(),
                    context -> new ThrownItemRenderer<>(
                            context,
                            0.6F,
                            false
                    )
            );
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                    VanillaGuiLayers.HOTBAR,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "toolbelt"
                    ),
                    (graphics, delta) -> renderHotbar(graphics)
            );
        }
    }
}