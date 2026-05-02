package com.infamousgc.bedrolls;

import com.infamousgc.bedrolls.client.SpawnType;
import com.infamousgc.bedrolls.network.SpawnInfoPacket;
import com.infamousgc.bedrolls.util.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BedrollBlock extends Block {

    private static final String NBT_SPAWN_X = "SpawnX";
    private static final String NBT_SPAWN_Y = "SpawnY";
    private static final String NBT_SPAWN_Z = "SpawnZ";
    private static final String NBT_SPAWN_ANGLE = "SpawnAngle";
    private static final String NBT_SPAWN_DIMENSION = "SpawnDimension";
    private static final String NBT_SPAWN_FORCED = "SpawnForced";

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    private static final Map<Direction, VoxelShape> FOOT_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> HEAD_SHAPES = new EnumMap<>(Direction.class);

    static {
        // Foot: 14 wide, 4 tall
        FOOT_SHAPES.put(Direction.NORTH, Block.box(1, 0, 0, 15, 4, 16));
        FOOT_SHAPES.put(Direction.SOUTH, Block.box(1, 0, 0, 15, 4, 16));
        FOOT_SHAPES.put(Direction.EAST, Block.box(0, 0, 1, 16, 4, 15));
        FOOT_SHAPES.put(Direction.WEST, Block.box(0, 0, 1, 16, 4, 15));

        // NORTH: pillow at Z=0 end
        HEAD_SHAPES.put(Direction.NORTH, Shapes.or(
                Block.box(0, 0, 7, 16, 5, 14),   // blanket
                Block.box(1, 0, 0, 15, 2, 7),    // bottom strip
                Block.box(2, 2, 1, 14, 4, 7),    // pillow
                Block.box(1, 0, 14, 15, 4, 16)   // slab stub
        ));
        // SOUTH: pillow at Z=16 end (mirrored)
        HEAD_SHAPES.put(Direction.SOUTH, Shapes.or(
                Block.box(0, 0, 2, 16, 5, 9),    // blanket
                Block.box(1, 0, 9, 15, 2, 16),   // bottom strip
                Block.box(2, 2, 9, 14, 4, 15),   // pillow
                Block.box(1, 0, 0, 15, 4, 2)     // slab stub
        ));
        // EAST: pillow at X=16 end
        HEAD_SHAPES.put(Direction.EAST, Shapes.or(
                Block.box(2, 0, 0, 9, 5, 16),    // blanket
                Block.box(9, 0, 1, 16, 2, 15),   // bottom strip
                Block.box(9, 2, 2, 15, 4, 14),   // pillow
                Block.box(0, 0, 1, 2, 4, 15)     // slab stub
        ));
        // WEST: pillow at X=0 end
        HEAD_SHAPES.put(Direction.WEST, Shapes.or(
                Block.box(7, 0, 0, 14, 5, 16),   // blanket
                Block.box(0, 0, 1, 7, 2, 15),    // bottom strip
                Block.box(1, 2, 2, 7, 4, 14),    // pillow
                Block.box(14, 0, 1, 16, 4, 15)   // slab stub
        ));
    }

    public BedrollBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(0.1f)
                .noOcclusion()
                .sound(SoundType.WOOL));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos footPos = context.getClickedPos();
        BlockPos headPos = footPos.relative(context.getHorizontalDirection());

        // Check for nether-like dimensions (where beds don't work)
        if (!level.dimensionType().bedWorks()) {
            Config.NetherBedrollBehavior behavior = Config.NETHER_BEDROLL_BEHAVIOR.get();

            if (behavior == Config.NetherBedrollBehavior.DISALLOW) {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    player.displayClientMessage(
                            Component.translatable("block.bedrolls.bedroll.disallowed_here"), true);
                }
                return null;
            }
            // ALLOW and EXPLODE proceed to normal placement
            // (EXPLODE is handled in setPlacedBy after placement)
        }

        if (level.getBlockState(headPos).isAir()) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection())
                    .setValue(PART, BedPart.FOOT);
        }
        return null;
    }

    @Override
    public void onPlace(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        if (state.getValue(PART) == BedPart.FOOT) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            if (level.getBlockState(headPos).isAir()) {
                level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, @NotNull ItemStack stack) {
        if (level.isClientSide || !(placer instanceof ServerPlayer player)) return;

        // Nether-like dimension handling
        if (!level.dimensionType().bedWorks()) {
            Config.NetherBedrollBehavior behavior = Config.NETHER_BEDROLL_BEHAVIOR.get();

            if (behavior == Config.NetherBedrollBehavior.EXPLODE) {
                // Remove both halves so the explosion doesn't leave debris
                BlockPos headPos = pos.relative(state.getValue(FACING));
                level.removeBlock(headPos, false);
                level.removeBlock(pos, false);
                // Trigger bed-style explosion at foot position
                explodeLikeBed(level, pos);
                return;
            }

            if (behavior == Config.NetherBedrollBehavior.ALLOW) {
                // Placed for decoration but don't set spawn
                player.displayClientMessage(
                        Component.translatable("block.bedrolls.bedroll.no_spawn_here"), true);
                return;
            }
        }

        setRespawn(player, level, pos, state.getValue(FACING));
    }

    @Override
    public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        BlockPos footPos = state.getValue(PART) == BedPart.FOOT
                ? pos
                : pos.relative(state.getValue(FACING).getOpposite());

        // In nether-like dimensions, right-clicking explodes the bedroll (vanilla bed behavior)
        if (!level.dimensionType().bedWorks()) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.removeBlock(headPos, false);
            level.removeBlock(footPos, false);
            explodeLikeBed(level, footPos);
            return InteractionResult.SUCCESS;
        }

        setRespawn(serverPlayer, level, footPos, state.getValue(FACING));
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, BlockState state, @NotNull Player player) {
        BedPart part = state.getValue(PART);
        BlockPos otherPos = part == BedPart.FOOT
                ? pos.relative(state.getValue(FACING))
                : pos.relative(state.getValue(FACING).getOpposite());

        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.getBlock() == this && otherState.getValue(PART) != part) {
            level.levelEvent(2001, otherPos, Block.getId(otherState));
            level.removeBlock(otherPos, false);
        }

        if (!level.isClientSide && state.getValue(PART) == BedPart.FOOT) {
            MinecraftServer server = ((ServerLevel) level).getServer();

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (pos.equals(p.getRespawnPosition())) {
                    p.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false);
                    PacketDistributor.sendToPlayer(p, new SpawnInfoPacket(SpawnType.NONE));
                }
            }
            clearOfflineSpawns(server, pos);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(PART) == BedPart.FOOT
                ? FOOT_SHAPES.get(state.getValue(FACING))
                : HEAD_SHAPES.get(state.getValue(FACING));
    }

    private void explodeLikeBed(Level level, BlockPos pos) {
        level.explode(
                null,
                level.damageSources().badRespawnPointExplosion(pos.getCenter()),
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                5.0F,
                true,
                Level.ExplosionInteraction.BLOCK
        );
    }

    private void setRespawn(ServerPlayer serverPlayer, Level level, BlockPos pos, Direction facing) {
        if (serverPlayer.getRespawnPosition() != null && serverPlayer.getRespawnPosition().equals(pos)) return;
        float yaw = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case EAST -> 90f;
            case WEST -> 270f;
            default -> 0f;
        };
        serverPlayer.setRespawnPosition(level.dimension(), pos, yaw, true, false);
        serverPlayer.displayClientMessage(
                Component.translatable("block.bedrolls.bedroll.spawn_set"), true
        );
        PacketDistributor.sendToPlayer(serverPlayer, new SpawnInfoPacket(SpawnType.BEDROLL));
    }

    private void clearOfflineSpawns(MinecraftServer server, BlockPos targetPos) {
        File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        if (!playerDataDir.isDirectory()) return;

        File[] files = playerDataDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files == null) return;

        Set<UUID> onlineUUIDs = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            onlineUUIDs.add(p.getUUID());
        }

        for (File file : files) {
            String fileName = file.getName();
            UUID uuid;
            try {
                uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (onlineUUIDs.contains(uuid)) continue;

            try {
                CompoundTag tag = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
                if (tag.contains(NBT_SPAWN_X) && tag.contains(NBT_SPAWN_Y) && tag.contains(NBT_SPAWN_Z)) {
                    BlockPos spawnPos = new BlockPos(
                            tag.getInt(NBT_SPAWN_X), tag.getInt(NBT_SPAWN_Y), tag.getInt(NBT_SPAWN_Z)
                    );
                    if (spawnPos.equals(targetPos)) {
                        tag.remove(NBT_SPAWN_X);
                        tag.remove(NBT_SPAWN_Y);
                        tag.remove(NBT_SPAWN_Z);
                        tag.remove(NBT_SPAWN_ANGLE);
                        tag.remove(NBT_SPAWN_DIMENSION);
                        tag.remove(NBT_SPAWN_FORCED);
                        NbtIo.writeCompressed(tag, file.toPath());
                    }
                }
            } catch (IOException ignored) {
                // File may be corrupted, locked by another process, or unreadable.
                // Skip it and continue with the rest. Worst case: that player still has
                // their stale spawn point on next login, which is a minor inconvenience.
            }
        }
    }
}
