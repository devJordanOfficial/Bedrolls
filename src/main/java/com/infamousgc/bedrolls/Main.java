package com.infamousgc.bedrolls;

import com.infamousgc.bedrolls.network.RespawnAtWorldSpawnPacket;
import com.infamousgc.bedrolls.network.SpawnPointStatusPacket;
import com.infamousgc.bedrolls.network.WorldSpawnReadyPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(Main.MODID)
public class Main {

    public static final String MODID = "bedrolls";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<BedrollBlock> BEDROLL = BLOCKS.register("bedroll", BedrollBlock::new);
    public static final DeferredItem<BlockItem> BEDROLL_ITEM = ITEMS.registerItem("bedroll", props ->
            new BlockItem(BEDROLL.get(), props), new Item.Properties().stacksTo(1));

    public static final Map<UUID, SavedSpawn> PENDING_SPAWN_RESTORES = new ConcurrentHashMap<>();
    public record SavedSpawn(ResourceKey<Level> dim, BlockPos pos, float angle, boolean forced) {}

    public Main(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPackets);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static class GameEvents {
        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                boolean hasBedroll = player.getRespawnPosition() != null;
                PacketDistributor.sendToPlayer(player, new SpawnPointStatusPacket(hasBedroll));
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                SavedSpawn saved = PENDING_SPAWN_RESTORES.remove(player.getUUID());
                if (saved != null) {
                    player.setRespawnPosition(saved.dim, saved.pos, saved.angle, saved.forced, false);
                    PacketDistributor.sendToPlayer(player, new SpawnPointStatusPacket(true));
                }
            }
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(BEDROLL_ITEM);
        }
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RespawnAtWorldSpawnPacket.TYPE,
                RespawnAtWorldSpawnPacket.CODEC,
                RespawnAtWorldSpawnPacket::handle
        );
        registrar.playToClient(
                SpawnPointStatusPacket.TYPE,
                SpawnPointStatusPacket.CODEC,
                SpawnPointStatusPacket::handle
        );
        registrar.playToClient(
                WorldSpawnReadyPacket.TYPE,
                WorldSpawnReadyPacket.CODEC,
                WorldSpawnReadyPacket::handle
        );
    }
}
