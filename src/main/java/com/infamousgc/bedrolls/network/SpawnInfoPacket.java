package com.infamousgc.bedrolls.network;

import com.infamousgc.bedrolls.client.BedrollClientData;
import com.infamousgc.bedrolls.client.SpawnType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnInfoPacket(SpawnType spawnType) implements CustomPacketPayload {

    public static final Type<SpawnInfoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("bedrolls", "spawn_info")
    );
    public static final StreamCodec<FriendlyByteBuf, SpawnInfoPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    packet -> packet.spawnType.ordinal(),
                    ordinal -> new SpawnInfoPacket(SpawnType.values()[ordinal])
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BedrollClientData.spawnType = packet.spawnType());
    }
}
