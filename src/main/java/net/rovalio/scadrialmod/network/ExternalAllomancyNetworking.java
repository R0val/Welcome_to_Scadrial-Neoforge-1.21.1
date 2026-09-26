package net.rovalio.scadrialmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.ClientExternalAllomancy;
import net.rovalio.scadrialmod.power.allomancy.ExternalAllomancyPhysics;
import net.rovalio.scadrialmod.power.allomancy.MetalTarget;

import java.util.UUID;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ExternalAllomancyNetworking {

    private ExternalAllomancyNetworking() {
    }

    private static <T extends CustomPacketPayload>
    CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ScadrialMod.MOD_ID, path
                )
        );
    }

    public record Input(
            ResourceLocation dimension,
            boolean push,
            boolean pull
    ) implements CustomPacketPayload {

        public static final Type<Input> TYPE =
                ExternalAllomancyNetworking.type(
                        "external_allomancy_input"
                );

        public static final StreamCodec<ByteBuf, Input> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, Input::dimension,
                        ByteBufCodecs.BOOL, Input::push,
                        ByteBufCodecs.BOOL, Input::pull,
                        Input::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Selection(
            ResourceLocation dimension,
            MetalTarget target,
            boolean push,
            boolean pull
    ) implements CustomPacketPayload {

        public static final Type<Selection> TYPE =
                ExternalAllomancyNetworking.type(
                        "external_allomancy_selection"
                );

        public static final StreamCodec<ByteBuf, Selection> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, Selection::dimension,
                        MetalTarget.STREAM_CODEC, Selection::target,
                        ByteBufCodecs.BOOL, Selection::push,
                        ByteBufCodecs.BOOL, Selection::pull,
                        Selection::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Impulse(
            ResourceLocation dimension,
            UUID player,
            double x,
            double y,
            double z
    ) implements CustomPacketPayload {

        public static final Type<Impulse> TYPE =
                ExternalAllomancyNetworking.type(
                        "external_allomancy_impulse"
                );

        public static final StreamCodec<ByteBuf, Impulse> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, Impulse::dimension,
                        UUIDUtil.STREAM_CODEC, Impulse::player,
                        ByteBufCodecs.DOUBLE, Impulse::x,
                        ByteBufCodecs.DOUBLE, Impulse::y,
                        ByteBufCodecs.DOUBLE, Impulse::z,
                        Impulse::new
                );

        public Impulse {
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(z)) {
                throw new IllegalArgumentException(
                        "Nonfinite allomantic impulse"
                );
            }
        }

        public Vec3 velocityChange() {
            return new Vec3(x, y, z);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("external_physics_1");

        registrar.playToServer(
                Input.TYPE,
                Input.STREAM_CODEC,
                ExternalAllomancyPhysics::handleInput
        );

        registrar.playToClient(
                Selection.TYPE,
                Selection.STREAM_CODEC,
                (payload, context) ->
                        ClientExternalAllomancy.handleSelection(payload)
        );

        registrar.playToClient(
                Impulse.TYPE,
                Impulse.STREAM_CODEC,
                (payload, context) ->
                        ClientExternalAllomancy.handleImpulse(payload)
        );
    }
}