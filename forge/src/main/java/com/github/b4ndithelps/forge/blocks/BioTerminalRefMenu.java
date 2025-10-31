package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BioTerminalRefMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;
    private BlockPos pos;

    public BioTerminalRefMenu(int id, Inventory playerInv, BioTerminalRefBlockEntity be, ContainerData data) {
        super(ModMenus.BIO_TERMINAL_REF.get(), id);
        this.container = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.pos = be.getBlockPos();
        this.addDataSlots(data);
        // Add database slot (position matches style of main terminal)
        this.addSlot(new Slot(be, BioTerminalRefBlockEntity.SLOT_DISK, -20, 6));
    }

    public BioTerminalRefMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.BIO_TERMINAL_REF.get(), id);
        this.pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(this.pos);
        if (be instanceof BioTerminalRefBlockEntity term) {
            this.container = term;
            this.data = new SimpleContainerData(1);
            this.access = ContainerLevelAccess.create(term.getLevel(), term.getBlockPos());
        } else {
            this.container = new SimpleContainer(1);
            this.data = new SimpleContainerData(1);
            this.access = ContainerLevelAccess.NULL;
        }
        this.addDataSlots(this.data);
        this.addSlot(new Slot((Container)this.container, BioTerminalRefBlockEntity.SLOT_DISK, -20, 6));
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




