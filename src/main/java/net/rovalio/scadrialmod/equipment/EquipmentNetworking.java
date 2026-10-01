package net.rovalio.scadrialmod.equipment;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.EquipmentClient;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class EquipmentNetworking {

    public static final int STOW = -1;
    public static final int OPEN_BELT = -3;

    private EquipmentNetworking() {}

    public record Action(int slot) implements CustomPacketPayload {

        public static final Type<Action> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ScadrialMod.MOD_ID,
                        "equipment_action"
                )
        );

        public static final StreamCodec<ByteBuf, Action> CODEC =
                ByteBufCodecs.VAR_INT.map(
                        Action::new,
                        Action::slot
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Selection(
            int belt,
            int pocket,
            int hand
    ) implements CustomPacketPayload {

        public static final Type<Selection> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ScadrialMod.MOD_ID,
                        "equipment_selection"
                )
        );

        public static final StreamCodec<ByteBuf, Selection> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, Selection::belt,
                        ByteBufCodecs.VAR_INT, Selection::pocket,
                        ByteBufCodecs.VAR_INT, Selection::hand,
                        Selection::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Motion(
            int id,
            Vec3 position,
            Vec3 velocity,
            boolean embedded
    ) implements CustomPacketPayload {

        public static final Type<Motion> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        ScadrialMod.MOD_ID,
                        "projectile_motion"
                )
        );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                Motion
                > CODEC = new StreamCodec<>() {

            @Override
            public Motion decode(RegistryFriendlyByteBuf buffer) {
                return new Motion(
                        buffer.readVarInt(),
                        readVector(buffer),
                        readVector(buffer),
                        buffer.readBoolean()
                );
            }

            @Override
            public void encode(
                    RegistryFriendlyByteBuf buffer,
                    Motion value
            ) {
                buffer.writeVarInt(value.id());

                writeVector(buffer, value.position());
                writeVector(buffer, value.velocity());

                buffer.writeBoolean(value.embedded());
            }
        };

        private static Vec3 readVector(
                RegistryFriendlyByteBuf buffer
        ) {
            return new Vec3(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            );
        }

        private static void writeVector(
                RegistryFriendlyByteBuf buffer,
                Vec3 vector
        ) {
            buffer.writeDouble(vector.x);
            buffer.writeDouble(vector.y);
            buffer.writeDouble(vector.z);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void sync(
            ServerPlayer player,
            int belt,
            int pocket,
            int hand
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new Selection(belt, pocket, hand)
        );
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("equipment_3");

        registrar.playToServer(
                Action.TYPE,
                Action.CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer player)) {
                        return;
                    }

                    if (payload.slot() == STOW) {
                        AllomanticEquipment.stow(player);
                    } else if (payload.slot() == OPEN_BELT) {
                        AllomanticEquipment.openEquipped(player);
                    } else {
                        AllomanticEquipment.select(
                                player,
                                payload.slot()
                        );
                    }
                }
        );

        registrar.playToClient(
                Selection.TYPE,
                Selection.CODEC,
                (payload, context) ->
                        EquipmentClient.selection(payload)
        );

        registrar.playToClient(
                Motion.TYPE,
                Motion.CODEC,
                (payload, context) ->
                        EquipmentClient.projectileMotion(payload)
        );
    }
}