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

        // Machine inventory
        this.addSlot(new Slot(be, DNASequencerBlockEntity.SLOT_INPUT, 43, 34));
        this.addSlot(new Slot(be, DNASequencerBlockEntity.SLOT_DISK, 7, 53));
        this.addSlot(new Slot(be, DNASequencerBlockEntity.SLOT_OUT_A, 89, 33));
        this.addSlot(new Slot(be, DNASequencerBlockEntity.SLOT_OUT_B, 115, 33));

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    // Client-side constructor using buffer with BlockPos from NetworkHooks.openScreen
    public DNASequencerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.DNA_SEQUENCER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof DNASequencerBlockEntity sequencer)) {
            // Fallback to dummy container to avoid crash; should not happen normally
            this.container = new SimpleContainer(4);
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.NULL;
        } else {
            this.container = sequencer;
            this.data = new SimpleContainerData(2);
            this.access = ContainerLevelAccess.create(sequencer.getLevel(), sequencer.getBlockPos());
        }
        this.pos = pos;

        this.addDataSlots(this.data);

        // Machine inventory
        this.addSlot(new Slot((Container) this.container, DNASequencerBlockEntity.SLOT_INPUT, 26, 35));
        this.addSlot(new Slot((Container) this.container, DNASequencerBlockEntity.SLOT_DISK, 26, 57));
        this.addSlot(new Slot((Container) this.container, DNASequencerBlockEntity.SLOT_OUT_A, 134, 26));
        this.addSlot(new Slot((Container) this.container, DNASequencerBlockEntity.SLOT_OUT_B, 134, 50));

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.DNA_SEQUENCER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 4) {
                if (!this.moveItemStackTo(stack, 4, 40, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }
}


