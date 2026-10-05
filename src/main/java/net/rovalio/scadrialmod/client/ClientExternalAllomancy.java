package net.rovalio.scadrialmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.ExternalAllomancyNetworking;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.physical.external.ExternalAllomancyMath;
import net.rovalio.scadrialmod.power.allomancy.physical.external.MetalTarget;
import net.rovalio.scadrialmod.power.allomancy.physical.external.MetalTargetSelector;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID, value = Dist.CLIENT)
public final class ClientExternalAllomancy {

    private static final int INPUT_REFRESH_TICKS = 5;

    private static KeyMapping pushKey;
    private static KeyMapping pullKey;

    private static ClientLevel lastLevel;
    private static LocalPlayer lastPlayer;

    private static boolean sentPush;
    private static boolean sentPull;
    private static boolean pushArmed;
    private static boolean pullArmed;

    private static int ticksSinceSend;

    private static MetalTarget preview = MetalTarget.NONE;
    private static ExternalAllomancyNetworking.Selection confirmed;

    private ClientExternalAllomancy() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        refreshContext(minecraft);

        if (minecraft.player == null
                || minecraft.level == null
                || pushKey == null
                || pullKey == null) {
            return;
        }

        boolean active = acceptsInput(minecraft);

        updateKeyArming(active);

        boolean temporalInput = ClientTemporalAllomancy.update(
                active,
                pushArmed && pushKey.isDown(),
                pullArmed && pullKey.isDown()
        );

        updatePreview(
                minecraft,
                active && !temporalInput
        );

        boolean push = active
                && !temporalInput
                && pushArmed
                && pushKey.isDown()
                && ClientAllomancyBurnState.isBurning(
                AllomanticFuel.STEEL
        );

        boolean pull = active
                && !temporalInput
                && pullArmed
                && pullKey.isDown()
                && ClientAllomancyBurnState.isBurning(
                AllomanticFuel.IRON
        );

        sendInput(minecraft, push, pull);

        boolean aluminium = ClientAllomancyBurnState.isBurning(
                AllomanticFuel.ALUMINIUM
        );

        AllomancyLoopSound.update(
                push && !aluminium,
                pull && !aluminium
        );
    }

    private static void refreshContext(Minecraft minecraft) {
        if (minecraft.level != lastLevel
                || minecraft.player != lastPlayer) {
            clear();

            lastLevel = minecraft.level;
            lastPlayer = minecraft.player;
        }
    }

    private static boolean acceptsInput(Minecraft minecraft) {
        return minecraft.screen == null
                && minecraft.isWindowActive()
                && !minecraft.isPaused()
                && minecraft.player.isAlive()
                && !minecraft.player.isSpectator();
    }

    private static void updateKeyArming(boolean active) {
        if (!active) {
            pushArmed = false;
            pullArmed = false;
            return;
        }

        if (!pushKey.isDown()) {
            pushArmed = true;
        }

        if (!pullKey.isDown()) {
            pullArmed = true;
        }
    }

    private static void updatePreview(
            Minecraft minecraft,
            boolean active
    ) {
        boolean perceiving =
                ClientAllomancyBurnState.isBurning(AllomanticFuel.IRON)
                        || ClientAllomancyBurnState.isBurning(AllomanticFuel.STEEL);

        preview = active && perceiving
                ? MetalTargetSelector.select(
                minecraft.player,
                ClientMetalSources.current(),
                preview
        )
                : MetalTarget.NONE;
    }

    private static void sendInput(
            Minecraft minecraft,
            boolean push,
            boolean pull
    ) {
        boolean changed = push != sentPush || pull != sentPull;
        boolean refresh = (push || pull)
                && ++ticksSinceSend >= INPUT_REFRESH_TICKS;

        if (!changed && !refresh) {
            return;
        }

        if (changed) {
            confirmed = null;
        }

        PacketDistributor.sendToServer(
                new ExternalAllomancyNetworking.Input(
                        minecraft.level.dimension().location(),
                        push,
                        pull
                )
        );

        sentPush = push;
        sentPull = pull;
        ticksSinceSend = 0;
    }

    public static MetalTarget selected() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return MetalTarget.NONE;
        }

        if ((sentPush || sentPull)
                && confirmed != null
                && confirmed.dimension().equals(
                minecraft.level.dimension().location()
        )) {
            return confirmed.target();
        }

        return preview;
    }

    public static void handleSelection(
            ExternalAllomancyNetworking.Selection payload
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level != null
                && payload.dimension().equals(
                minecraft.level.dimension().location()
        )) {
            confirmed = payload;
        }
    }

    public static void handleImpulse(
            ExternalAllomancyNetworking.Impulse payload
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null
                || minecraft.level == null
                || !player.isAlive()
                || player.isSpectator()
                || !payload.player().equals(player.getUUID())
                || !payload.dimension().equals(
                minecraft.level.dimension().location()
        )) {
            return;
        }

        Vec3 before = player.getDeltaMovement();
        Vec3 delta = payload.velocityChange();

        if (delta.y > 0.0
                && before.y < 0.0
                && !player.onGround()) {
            player.fallDistance *=
                    (float) ExternalAllomancyMath.brakingRatio(
                            -before.y, delta.y
                    );
        }

        player.setDeltaMovement(before.add(delta));
    }

    @SubscribeEvent
    public static void logout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        clear();
        ClientTemporalAllomancy.forgetWorld();
    }

    private static void clear() {
        AllomancyLoopSound.clear();
        ClientTemporalAllomancy.clear();

        preview = MetalTarget.NONE;
        confirmed = null;

        lastLevel = null;
        lastPlayer = null;

        sentPush = false;
        sentPull = false;

        pushArmed = false;
        pullArmed = false;

        ticksSinceSend = 0;
    }

    @EventBusSubscriber(
            modid = ScadrialMod.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class Keys {

        private Keys() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            pushKey = create(
                    "allomantic_push",
                    InputConstants.KEY_R
            );
            pullKey = create(
                    "allomantic_pull",
                    InputConstants.KEY_G
            );

            event.register(pushKey);
            event.register(pullKey);
        }

        private static KeyMapping create(String name, int key) {
            return new KeyMapping(
                    "key.welcome_to_scadrial." + name,
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    key,
                    "key.categories.welcome_to_scadrial"
            );
        }
    }
}