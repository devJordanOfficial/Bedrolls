package com.infamousgc.bedrolls.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WorldSpawnReadyPacket() implements CustomPacketPayload {

    public static final Type<WorldSpawnReadyPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("bedrolls", "world_spawn_ready")
    );
    public static final StreamCodec<FriendlyByteBuf, WorldSpawnReadyPacket> CODEC =
            StreamCodec.unit(new WorldSpawnReadyPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WorldSpawnReadyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.respawn();
                mc.setScreen(null);
            }
        });
    }
}
