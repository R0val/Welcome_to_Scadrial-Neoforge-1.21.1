package net.rovalio.scadrialmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.ClientTemporalAllomancy;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalAllomancyManager;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalBubble;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class TemporalAllomancyNetworking {

    public static final int START = 0;
    public static final int KEEP = 1;
    public static final int RELEASE = 2;
    public static final int CANCEL = 3;

    private TemporalAllomancyNetworking() {}

    private static <T extends CustomPacketPayload>
    CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ScadrialMod.MOD_ID,
                        name
                )
        );
    }

    public record Input(
            ResourceLocation dimension,
            boolean bendalloy,
            int action
    ) implements CustomPacketPayload {

        public static final Type<Input> TYPE =
                TemporalAllomancyNetworking.type(
                        "temporal_input"
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, Input> CODEC =
                new StreamCodec<>() {

                    @Override
                    public Input decode(RegistryFriendlyByteBuf buffer) {
                        return new Input(
                                buffer.readResourceLocation(),
                                buffer.readBoolean(),
                                buffer.readUnsignedByte()
                        );
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            Input input
                    ) {
                        buffer.writeResourceLocation(input.dimension());
                        buffer.writeBoolean(input.bendalloy());
                        buffer.writeByte(input.action());
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record State(
            ResourceLocation dimension,
            List<TemporalBubble> bubbles
    ) implements CustomPacketPayload {

        public State {
            bubbles = List.copyOf(bubbles);
        }

        public static final Type<State> TYPE =
                TemporalAllomancyNetworking.type(
                        "temporal_state"
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
                new StreamCodec<>() {

                    @Override
                    public State decode(RegistryFriendlyByteBuf buffer) {
                        ResourceLocation dimension =
                                buffer.readResourceLocation();

                        int size = buffer.readVarInt();

                        if (size < 0
                                || size > TemporalAllomancyManager.MAX_BUBBLES) {
                            throw new IllegalArgumentException(
                                    "Invalid temporal bubble count"
                            );
                        }

                        List<TemporalBubble> bubbles =
                                new ArrayList<>(size);

                        for (int i = 0; i < size; i++) {
                            var owner = buffer.readUUID();

                            Vec3 center = new Vec3(
                                    buffer.readDouble(),
                                    buffer.readDouble(),
                                    buffer.readDouble()
                            );

                            bubbles.add(
                                    new TemporalBubble(
                                            owner,
                                            center,
                                            buffer.readDouble(),
                                            buffer.readDouble(),
                                            buffer.readBoolean(),
                                            buffer.readBoolean()
                                    )
                            );
                        }

                        return new State(dimension, bubbles);
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            State state
                    ) {
                        buffer.writeResourceLocation(state.dimension());
                        buffer.writeVarInt(state.bubbles().size());

                        for (TemporalBubble bubble : state.bubbles()) {
                            buffer.writeUUID(bubble.owner());

                            buffer.writeDouble(bubble.center().x);
                            buffer.writeDouble(bubble.center().y);
                            buffer.writeDouble(bubble.center().z);

                            buffer.writeDouble(bubble.radius());
                            buffer.writeDouble(bubble.outerRadius());

                            buffer.writeBoolean(bubble.bendalloy());
                            buffer.writeBoolean(bubble.boosted());
                        }
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("temporal_1");

        registrar.playToServer(
                Input.TYPE,
                Input.CODEC,
                (input, context) -> {
                    context.enqueueWork(() -> {
                        if (!(context.player() instanceof ServerPlayer player)
                                || player.hasDisconnected()) {
                            return;
                        }

                        TemporalAllomancyManager.input(player, input);
                    }).exceptionally(error -> {
                        ScadrialMod.LOGGER.error(
                                "Failed to process temporal input",
                                error
                        );

                        return null;
                    });
                }
        );

        registrar.playToClient(
                State.TYPE,
                State.CODEC,
                (state, context) -> {
                    context.enqueueWork(
                            () -> ClientTemporalAllomancy.receive(state)
                    ).exceptionally(error -> {
                        ScadrialMod.LOGGER.error(
                                "Failed to apply temporal state",
                                error
                        );

                        return null;
                    });
                }
        );
    }
}