package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import com.github.b4ndithelps.forge.item.GeneDatabaseItem;

public class BioTerminalBlock extends Block implements EntityBlock {
    public BioTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            System.out.println("a");
            var be = level.getBlockEntity(pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                System.out.println("df");
                // Sneak-insert/extract database (handle extraction here; insertion is also handled by item use)
                if (player.isCrouching()) {
                    System.out.println("ee");
                    ItemStack held = player.getItemInHand(hand);
                    ItemStack slot = terminal.getItem(BioTerminalBlockEntity.SLOT_DISK);
                    // Extract when empty hand and slot occupied
                    if (held.isEmpty() && !slot.isEmpty()) {
                        ItemStack toGive = slot.copy();
                        terminal.setItem(BioTerminalBlockEntity.SLOT_DISK, ItemStack.EMPTY);
                        terminal.setChanged();
                        if (!player.addItem(toGive)) {
                            // drop at block position if inventory full
                            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, toGive);
                            level.addFreshEntity(drop);
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var be = level.getBlockEntity(pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                NetworkHooks.openScreen(serverPlayer, terminal, pos);
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
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof BioTerminalBlockEntity terminal) BioTerminalBlockEntity.serverTick(lvl, pos, st, terminal);
        };
    }
}


