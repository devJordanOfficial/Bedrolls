package com.infamousgc.bedrolls.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
                ResourceKey<Level> savedDim = player.getRespawnDimension();
                float savedAngle = player.getRespawnAngle();
                boolean savedForced = player.isRespawnForced();

                // Clear so vanilla respawn flow uses world spawn
                player.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false);

                // Restore on next tick (after vanilla respawn has completed
                player.server.tell(new TickTask(
                        player.server.getTickCount() + 2,
                        () -> {
                            if (savedPos != null) {
                                ServerPlayer current = player.server.getPlayerList().getPlayer(player.getUUID());
                                if (current != null) {
                                    current.setRespawnPosition(savedDim, savedPos, savedAngle, savedForced, false);
                                }
                            }
                        }
                ));
            }
        });
    }
}
