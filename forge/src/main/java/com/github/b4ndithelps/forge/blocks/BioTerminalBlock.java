package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.github.b4ndithelps.forge.item.GeneDatabaseItem;

public class BioTerminalBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BioTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                // Sneak extract/insert DB like main terminal
                if (player.isCrouching()) {
                    ItemStack held = player.getItemInHand(hand);
                    ItemStack slot = terminal.getDatabaseStack();
                    // Extract when empty hand and slot occupied
                    if (held.isEmpty() && !slot.isEmpty()) {
                        ItemStack toGive = slot.copy();
                        terminal.setDatabaseStack(ItemStack.EMPTY);
                        terminal.setChanged();
                        level.sendBlockUpdated(pos, state, state, 3);
                        if (!player.addItem(toGive)) {
                            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, toGive);
                            level.addFreshEntity(drop);
                        }
                        return InteractionResult.CONSUME;
                    }
                    // Insert when holding a database and slot empty
                    if (!held.isEmpty() && held.getItem() instanceof GeneDatabaseItem && slot.isEmpty()) {
                        ItemStack toInsert = held.copy();
                        toInsert.setCount(1);
                        terminal.setDatabaseStack(toInsert);
                        held.shrink(1);
                        terminal.setChanged();
                        level.sendBlockUpdated(pos, state, state, 3);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BioTerminalBlockEntity provider) {
                NetworkHooks.openScreen(serverPlayer, provider, pos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioTerminalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, p, st, be) -> {
            if (be instanceof BioTerminalBlockEntity terminal) BioTerminalBlockEntity.serverTick(lvl, p, st, terminal);
        };
    }
}




