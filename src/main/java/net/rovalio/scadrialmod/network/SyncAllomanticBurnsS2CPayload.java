package net.rovalio.scadrialmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.List;

public record SyncAllomanticBurnsS2CPayload(List<String> fuels)
        implements CustomPacketPayload {

    public static final Type<SyncAllomanticBurnsS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "sync_allomantic_burns"
            ));

    public static final StreamCodec<ByteBuf, SyncAllomanticBurnsS2CPayload>
            STREAM_CODEC =
            ByteBufCodecs.stringUtf8(48)
                    .apply(ByteBufCodecs.list(18))
                    .map(
                            SyncAllomanticBurnsS2CPayload::new,
                            SyncAllomanticBurnsS2CPayload::fuels
                    );

    public SyncAllomanticBurnsS2CPayload {
        fuels = List.copyOf(fuels);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}