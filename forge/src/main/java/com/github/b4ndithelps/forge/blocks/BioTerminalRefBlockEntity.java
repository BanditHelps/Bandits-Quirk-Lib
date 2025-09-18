package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;

public class BioTerminalRefBlockEntity extends BlockEntity implements MenuProvider {
    private final ContainerData data = new SimpleContainerData(1);

    public BioTerminalRefBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_TERMINAL_REF.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bandits_quirk_lib.bio_terminal_ref");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BioTerminalRefMenu(id, inv, this, data);
    }
}




