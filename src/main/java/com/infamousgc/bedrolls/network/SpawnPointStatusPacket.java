package com.infamousgc.bedrolls.network;

import com.infamousgc.bedrolls.client.BedrollClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnPointStatusPacket(boolean hasBedroll) implements CustomPacketPayload {

    public static final Type<SpawnPointStatusPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("bedrolls", "spawn_point_status")
    );
    public static final StreamCodec<FriendlyByteBuf, SpawnPointStatusPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SpawnPointStatusPacket::hasBedroll,
                    SpawnPointStatusPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnPointStatusPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BedrollClientData.hasBedroll = packet.hasBedroll());
    }
}
