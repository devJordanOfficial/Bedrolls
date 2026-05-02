package com.infamousgc.bedrolls.util;

import com.infamousgc.bedrolls.Main;
import com.infamousgc.bedrolls.client.SpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnTypeHelper {

    public static SpawnType getSpawnType(ServerPlayer player) {
        BlockPos pos = player.getRespawnPosition();
        if (pos == null) return SpawnType.NONE;

        ServerLevel level = player.server.getLevel(player.getRespawnDimension());
        if (level == null) return SpawnType.NONE;

        BlockState state = level.getBlockState(pos);

        if (state.is(Main.BEDROLL.get())) {
            return SpawnType.BEDROLL;
        }

        if (state.getBlock() instanceof RespawnAnchorBlock) {
            int charges = state.getValue(RespawnAnchorBlock.CHARGE);
            return charges > 0 ? SpawnType.ANCHOR_CHARGED : SpawnType.ANCHOR_EMPTY;
        }

        return SpawnType.NONE;
    }
}
