package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public class BioTerminalRefMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private BlockPos pos;

    public BioTerminalRefMenu(int id, Inventory playerInv, BioTerminalRefBlockEntity be, ContainerData data) {
        super(ModMenus.BIO_TERMINAL_REF.get(), id);
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.pos = be.getBlockPos();
        this.addDataSlots(data);
    }

    public BioTerminalRefMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.BIO_TERMINAL_REF.get(), id);
        this.pos = buf.readBlockPos();
        this.access = ContainerLevelAccess.NULL;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.BIO_TERMINAL_REF.get());
    }
}




