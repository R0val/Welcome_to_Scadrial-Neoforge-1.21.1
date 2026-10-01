package net.rovalio.scadrialmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.PhysicalInternalAllomancyClient;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.physical.internal.PhysicalInternalAllomancyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class PhysicalInternalAllomancyNetworking {

    private static final Map<UUID, State> LAST_SENT = new HashMap<>();

    private PhysicalInternalAllomancyNetworking() {
    }

    public record State(
            UUID player,
            ResourceLocation dimension,
            double debt,
            double pewterStrength,
            double tinStrength
    ) implements CustomPacketPayload {

        public static final Type<State> TYPE =
                new Type<>(
                        ResourceLocation.fromNamespaceAndPath(
                                ScadrialMod.MOD_ID,
                                "internal_allomancy"
                        )
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, State>
                CODEC = new StreamCodec<>() {

            @Override
            public State decode(RegistryFriendlyByteBuf buffer) {
                return new State(
                        buffer.readUUID(),
                        buffer.readResourceLocation(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                );
            }

            @Override
            public void encode(
                    RegistryFriendlyByteBuf buffer,
                    State state
            ) {
                buffer.writeUUID(state.player());
                buffer.writeResourceLocation(state.dimension());
                buffer.writeDouble(state.debt());
                buffer.writeDouble(state.pewterStrength());
                buffer.writeDouble(state.tinStrength());
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void sync(ServerPlayer player) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);

        State state = new State(
                player.getUUID(),
                player.level().dimension().location(),
                data.getPewterDebt(),
                PhysicalInternalAllomancyManager.strength(
                        player,
                        data,
                        AllomanticFuel.PEWTER
                ),
                PhysicalInternalAllomancyManager.strength(
                        player,
                        data,
                        AllomanticFuel.TIN
                )
        );

        if (!state.equals(LAST_SENT.put(player.getUUID(), state))) {
            PacketDistributor.sendToPlayer(player, state);
        }
    }

    public static void forceSync(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        sync(player);
    }

    public static void forget(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
    }

    public static void clear() {
        LAST_SENT.clear();
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("internal_1").playToClient(
                State.TYPE,
                State.CODEC,
                (state, context) -> PhysicalInternalAllomancyClient.receive(state)
        );
    }
}
