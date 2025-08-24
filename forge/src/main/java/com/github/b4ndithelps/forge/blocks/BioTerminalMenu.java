package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class BioTerminalMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;
    private BlockPos pos;

    public BioTerminalMenu(int id, Inventory playerInv, BioTerminalBlockEntity be, ContainerData data) {
        super(ModMenus.BIO_TERMINAL.get(), id);
        this.container = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        this.addDataSlots(data);
        this.pos = be.getBlockPos();

        this.addSlot(new Slot(be, BioTerminalBlockEntity.SLOT_DISK, -20, 6));
    }

    public BioTerminalMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.BIO_TERMINAL.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof BioTerminalBlockEntity terminal)) {
            this.container = new SimpleContainer(1);
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.NULL;
        } else {
            this.container = terminal;
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.create(terminal.getLevel(), terminal.getBlockPos());
        }
        this.pos = pos;

        this.addDataSlots(this.data);

        this.addSlot(new Slot((Container) this.container, BioTerminalBlockEntity.SLOT_DISK, -20, 6));
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.BIO_TERMINAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}


