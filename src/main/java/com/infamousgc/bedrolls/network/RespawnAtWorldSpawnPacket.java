package com.infamousgc.bedrolls.network;

import com.infamousgc.bedrolls.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RespawnAtWorldSpawnPacket() implements CustomPacketPayload {

    public static final Type<RespawnAtWorldSpawnPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("bedrolls", "respawn_at_world_spawn")
    );
    public static final StreamCodec<FriendlyByteBuf, RespawnAtWorldSpawnPacket> CODEC =
            StreamCodec.unit(new RespawnAtWorldSpawnPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RespawnAtWorldSpawnPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockPos savedPos = player.getRespawnPosition();

                if (savedPos != null) {
                    Main.PENDING_SPAWN_RESTORES.put(player.getUUID(), new Main.SavedSpawn(
                            player.getRespawnDimension(),
                            savedPos,
                            player.getRespawnAngle(),
                            player.isRespawnForced()
                    ));
                }

                player.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false);
                PacketDistributor.sendToPlayer(player, new WorldSpawnReadyPacket());
            }
        });
    }
}
