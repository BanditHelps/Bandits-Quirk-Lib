package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

public class ResearchTableBlock extends Block {
    public ResearchTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player != null && player.getServer() != null) {
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), "openguiscreen research_screen");
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}