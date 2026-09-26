package net.rovalio.scadrialmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.ScadrialMod;

public record ToggleAllomanticBurnC2SPayload(String fuel)
        implements CustomPacketPayload {

    public static final Type<ToggleAllomanticBurnC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "toggle_allomantic_burn"
            ));

    public static final StreamCodec<ByteBuf, ToggleAllomanticBurnC2SPayload>
            STREAM_CODEC =
            ByteBufCodecs.stringUtf8(48).map(
                    ToggleAllomanticBurnC2SPayload::new,
                    ToggleAllomanticBurnC2SPayload::fuel
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}