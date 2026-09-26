package net.rovalio.scadrialmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.MetalTarget;

import java.util.OptionalDouble;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID, value = Dist.CLIENT)
public final class MetalSourceRenderer {

    private static final RenderType BLUE_LINES = RenderType.create(
            "welcome_to_scadrial_metal_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            4096,
            RenderType.CompositeState.builder()
                    .setShaderState(
                            RenderStateShard.RENDERTYPE_LINES_SHADER
                    )
                    .setLineState(
                            new RenderStateShard.LineStateShard(
                                    OptionalDouble.of(2.0)
                            )
                    )
                    .setTransparencyState(
                            RenderStateShard.TRANSLUCENT_TRANSPARENCY
                    )
                    .setDepthTestState(
                            RenderStateShard.NO_DEPTH_TEST
                    )
                    .setWriteMaskState(
                            RenderStateShard.COLOR_WRITE
                    )
                    .setCullState(
                            RenderStateShard.NO_CULL
                    )
                    .createCompositeState(false)
    );

    private record LineContext(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 chest,
            Vec3 camera,
            double radius,
            float partialTick,
            MetalTarget selected
    ) {
    }

    private MetalSourceRenderer() {
    }

    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        ClientMetalSources.clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        SyncMetalSourcesS2CPayload state =
                ClientMetalSources.current();

        if (!canRender(minecraft, state)) {
            return;
        }

        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();

        LineContext context = createContext(
                minecraft, event, state, buffers
        );

        for (var target : state.targets()) {
            drawTarget(
                    minecraft.level,
                    context,
                    MetalTarget.of(target)
            );
        }

        for (var block : state.blocks()) {
            drawTarget(
                    minecraft.level,
                    context,
                    MetalTarget.of(block)
            );
        }

        flush(buffers);
    }

    private static boolean canRender(
            Minecraft minecraft,
            SyncMetalSourcesS2CPayload state
    ) {
        if (minecraft.player == null
                || minecraft.level == null
                || state == null) {
            return false;
        }

        return minecraft.player.isAlive()
                && !minecraft.player.isSpectator()
                && state.dimension().equals(
                minecraft.level.dimension().location()
        )
                && (!state.targets().isEmpty()
                || !state.blocks().isEmpty())
                && (ClientAllomancyBurnState.isBurning(AllomanticFuel.IRON)
                || ClientAllomancyBurnState.isBurning(AllomanticFuel.STEEL));
    }

    private static LineContext createContext(
            Minecraft minecraft,
            RenderLevelStageEvent event,
            SyncMetalSourcesS2CPayload state,
            MultiBufferSource.BufferSource buffers
    ) {
        float partialTick = event.getPartialTick()
                .getGameTimeDeltaPartialTick(false);

        double height = Math.min(
                1.25, minecraft.player.getBbHeight() * 0.75
        );

        Vec3 chest = minecraft.player.getPosition(partialTick)
                .add(0.0, height, 0.0);

        PoseStack poses = new PoseStack();
        poses.mulPose(event.getModelViewMatrix());

        return new LineContext(
                buffers.getBuffer(BLUE_LINES),
                poses.last(),
                chest,
                event.getCamera().getPosition(),
                state.radius(),
                partialTick,
                ClientExternalAllomancy.selected()
        );
    }

    private static void drawTarget(
            ClientLevel level,
            LineContext context,
            MetalTarget target
    ) {
        Vec3 end = target.position(
                level, context.partialTick()
        );

        if (end == null) {
            return;
        }

        Vec3 direction = end.subtract(context.chest());
        double lengthSquared = direction.lengthSqr();

        if (lengthSquared < 1.0E-8
                || lengthSquared > context.radius() * context.radius()) {
            return;
        }

        Vec3 normal = direction.scale(
                1.0 / Math.sqrt(lengthSquared)
        );

        boolean selected = target.equals(context.selected());

        vertex(context, context.chest(), normal, selected);
        vertex(context, end, normal, selected);
    }

    private static void vertex(
            LineContext context,
            Vec3 worldPosition,
            Vec3 normal,
            boolean selected
    ) {
        Vec3 point = worldPosition.subtract(context.camera());

        int red = selected ? 125 : 70;
        int green = selected ? 225 : 155;
        int alpha = selected ? 255 : 200;

        context.vertices()
                .addVertex(
                        context.pose(),
                        (float) point.x,
                        (float) point.y,
                        (float) point.z
                )
                .setColor(red, green, 255, alpha)
                .setNormal(
                        context.pose(),
                        (float) normal.x,
                        (float) normal.y,
                        (float) normal.z
                );
    }

    private static void flush(
            MultiBufferSource.BufferSource buffers
    ) {
        RenderSystem.disableDepthTest();

        try {
            buffers.endBatch(BLUE_LINES);
        } finally {
            RenderSystem.enableDepthTest();
        }
    }
}