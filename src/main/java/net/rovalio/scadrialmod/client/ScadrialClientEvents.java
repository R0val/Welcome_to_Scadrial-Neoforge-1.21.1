package net.rovalio.scadrialmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.screen.AllomancyWheelScreen;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID, value = Dist.CLIENT)
public final class ScadrialClientEvents {

    private static KeyMapping openWheel;

    private ScadrialClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openWheel == null) {
            return;
        }
        while (openWheel.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new AllomancyWheelScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAllomancyState.clear();
        ClientAllomancyBurnState.clear();
    }

    @EventBusSubscriber(
            modid = ScadrialMod.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class KeyMappings {

        private KeyMappings() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            openWheel = new KeyMapping(
                    "key.welcome_to_scadrial.allomancy_wheel",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.welcome_to_scadrial"
            );
            event.register(openWheel);
        }
    }
}
