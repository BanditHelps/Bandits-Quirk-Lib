package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class DNASequencerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;
    private BlockPos pos;

    public DNASequencerMenu(int id, Inventory playerInv, DNASequencerBlockEntity be, ContainerData data) {
        super(ModMenus.DNA_SEQUENCER.get(), id);
        this.container = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        this.addDataSlots(data);
        this.pos = be.getBlockPos();

        // Single storage drive slot only; positioned near top-left (outside console background)
        this.addSlot(new Slot(be, DNASequencerBlockEntity.SLOT_DISK, -20, 6));
    }

    // Client-side constructor using buffer with BlockPos from NetworkHooks.openScreen
    public DNASequencerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.DNA_SEQUENCER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof DNASequencerBlockEntity sequencer)) {
            // Fallback to dummy container to avoid crash; should not happen normally
            this.container = new SimpleContainer(1);
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.NULL;
        } else {
            this.container = sequencer;
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.create(sequencer.getLevel(), sequencer.getBlockPos());
        }
        this.pos = pos;

        this.addDataSlots(this.data);

        // Only the storage drive slot
        this.addSlot(new Slot((Container) this.container, DNASequencerBlockEntity.SLOT_DISK, -20, 6));
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
        return stillValid(this.access, player, ModBlocks.DNA_SEQUENCER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Disable quick-move for console-only UI
        return ItemStack.EMPTY;
    }
}


