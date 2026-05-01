package com.infamousgc.bedrolls.event;

import com.infamousgc.bedrolls.Main;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Main.MODID)
public class BedInteractEvents {

    @SubscribeEvent
    public static void onBedRightClick(PlayerInteractEvent.RightClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() instanceof BedBlock) {
            if (!event.getLevel().dimensionType().bedWorks()) {
                return;
            }

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);

            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("block.bedrolls.bed.disabled"));
            }
        }
    }
}
