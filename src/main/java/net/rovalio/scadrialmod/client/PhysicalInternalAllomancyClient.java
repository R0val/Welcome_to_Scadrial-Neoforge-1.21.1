package net.rovalio.scadrialmod.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.PhysicalInternalAllomancyNetworking;
import net.rovalio.scadrialmod.power.allomancy.physical.internal.PhysicalInternalAllomancyMath;
import net.rovalio.scadrialmod.world.dimension.ScadrialDimensions;

import java.util.Locale;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        value = Dist.CLIENT
)
public final class PhysicalInternalAllomancyClient {

    public static final boolean MISTS_IN_OVERWORLD = true;
    public static final float MIST_FAR_DISTANCE = 24.0F;

    private static KeyMapping zoomKey;

    private static ClientLevel lastLevel;
    private static LocalPlayer lastPlayer;

    private static double tin;
    private static double pewter;
    private static double debt;

    private static float mist;
    private static float zoom = 1.0F;

    private PhysicalInternalAllomancyClient() {
    }

    public static void receive(PhysicalInternalAllomancyNetworking.State state) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || mc.level == null
                || !state.player().equals(mc.player.getUUID())
                || !state.dimension().equals(
                mc.level.dimension().location()
        )) {
            return;
        }

        if (mc.level != lastLevel || mc.player != lastPlayer) {
            clear();
        }

        lastLevel = mc.level;
        lastPlayer = mc.player;

        tin = finite(state.tinStrength());
        pewter = finite(state.pewterStrength());
        debt = finite(state.debt());
    }

    private static double finite(double value) {
        return Double.isFinite(value)
                ? Math.max(0.0, value)
                : 0.0;
    }

    public static double tinStrength() {
        Minecraft mc = Minecraft.getInstance();

        boolean valid =
                mc.level == lastLevel
                        && mc.player == lastPlayer
                        && mc.player != null
                        && mc.player.isAlive()
                        && !mc.player.isSpectator();

        return valid ? tin : 0.0;
    }

    public static float brightness(float original) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || visionImpaired()) {
            return original;
        }

        float enhancement = (float) PhysicalInternalAllomancyMath.nightVision(
                tinStrength()
        );

        return Mth.lerp(enhancement, original, 1.0F);
    }

    public static float soundGain() {
        return (float) PhysicalInternalAllomancyMath.soundGain(tinStrength());
    }

    public static float hearingRange() {
        return (float) PhysicalInternalAllomancyMath.hearingRange(tinStrength());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != lastLevel || mc.player != lastPlayer) {
            clear();

            lastLevel = mc.level;
            lastPlayer = mc.player;
        }

        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        boolean zooming =
                tinStrength() > 0.0
                        && zoomKey != null
                        && zoomKey.isDown()
                        && mc.screen == null
                        && mc.isWindowActive();

        float targetZoom = zooming
                ? (float) PhysicalInternalAllomancyMath.zoom(tinStrength())
                : 1.0F;

        zoom = tinStrength() == 0.0
                ? 1.0F
                : Mth.lerp(0.4F, zoom, targetZoom);

        mist = Mth.lerp(0.1F, mist, desiredMist(mc));
    }

    private static float desiredMist(Minecraft mc) {
        var dimension = mc.level.dimension();

        boolean enabled =
                dimension.equals(ScadrialDimensions.SCADRIAL)
                        || (MISTS_IN_OVERWORLD
                        && dimension.equals(Level.OVERWORLD));

        if (!enabled
                || !mc.level.canSeeSky(
                mc.gameRenderer
                        .getMainCamera()
                        .getBlockPosition()
        )) {
            return 0.0F;
        }

        long time = Math.floorMod(mc.level.getDayTime(), 24000L);

        float dusk = Mth.clamp(
                (time - 12000.0F) / 2000.0F,
                0.0F,
                1.0F
        );

        float dawn = Mth.clamp(
                (24000.0F - time) / 2000.0F,
                0.0F,
                1.0F
        );

        return dusk
                * dawn
                * (float) PhysicalInternalAllomancyMath.mistVisibility(
                tinStrength()
        );
    }

    private static boolean visionImpaired() {
        var player = Minecraft.getInstance().player;

        return player != null
                && (player.hasEffect(MobEffects.BLINDNESS)
                || player.hasEffect(MobEffects.DARKNESS));
    }

    private static boolean canRenderMist(ViewportEvent event) {
        return mist > 0.001F
                && !visionImpaired()
                && event.getCamera().getFluidInCamera() == FogType.NONE;
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (!canRenderMist(event)
                || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }

        float end = Math.min(
                event.getFarPlaneDistance(),
                MIST_FAR_DISTANCE
        );

        event.setFarPlaneDistance(
                Mth.lerp(mist, event.getFarPlaneDistance(), end)
        );

        event.setNearPlaneDistance(
                Mth.lerp(mist, event.getNearPlaneDistance(), 2.0F)
        );

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!canRenderMist(event)) {
            return;
        }

        float blend = 0.65F * mist;

        event.setRed(Mth.lerp(blend, event.getRed(), 0.57F));
        event.setGreen(Mth.lerp(blend, event.getGreen(), 0.59F));
        event.setBlue(Mth.lerp(blend, event.getBlue(), 0.61F));
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov() && tinStrength() > 0.0) {
            event.setFOV(
                    Math.max(5.0, event.getFOV() / zoom)
            );
        }
    }

    private static void hud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || mc.options.hideGui
                || !mc.player.isAlive()
                || mc.player.isSpectator()
                || (pewter <= 0.0 && debt <= 0.0)) {
            return;
        }

        String amount = String.format(
                Locale.ROOT,
                "%.2f",
                Math.ceil(debt * 100.0) / 100.0
        );

        var text = Component.translatable(
                "hud.welcome_to_scadrial.pewter_debt",
                amount
        );

        int color = debt >= mc.player.getHealth()
                ? 0xFFFF5555
                : 0xFFD7DBDF;

        int y = graphics.guiHeight() - 32;

        graphics.fill(
                4,
                y - 3,
                12 + mc.font.width(text),
                y + 12,
                0x99000000
        );

        graphics.drawString(
                mc.font,
                text,
                8,
                y,
                color,
                true
        );
    }

    private static void clear() {
        tin = 0.0;
        pewter = 0.0;
        debt = 0.0;

        mist = 0.0F;
        zoom = 1.0F;

        lastLevel = null;
        lastPlayer = null;
    }

    @SubscribeEvent
    public static void logout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        clear();
    }

    @EventBusSubscriber(
            modid = ScadrialMod.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class Registration {

        private Registration() {
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            zoomKey = new KeyMapping(
                    "key.welcome_to_scadrial.tin_zoom",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_C,
                    "key.categories.welcome_to_scadrial"
            );

            event.register(zoomKey);
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(
                    VanillaGuiLayers.HOTBAR,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "pewter_debt"
                    ),
                    (graphics, timer) -> hud(graphics)
            );
        }
    }
}