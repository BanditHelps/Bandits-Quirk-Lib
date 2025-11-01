package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class GeneSequencerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public GeneSequencerMenu(int id, Inventory playerInv, GeneSequencerBlockEntity be, ContainerData data) {
        super(ModMenus.GENE_SEQUENCER.get(), id);
        this.container = be;
        this.data = data;
        this.addDataSlots(data);

        // Make sure the input only allows tissue samples
        this.addSlot(new Slot(be, GeneSequencerBlockEntity.SLOT_INPUT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.TISSUE_SAMPLE.get();
            }
        });

        // Don't let players put anything in the output slot
        this.addSlot(new Slot(be, GeneSequencerBlockEntity.SLOT_OUTPUT, 98, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory
        addPlayerInventory(playerInv);
    }

    public GeneSequencerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.GENE_SEQUENCER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof GeneSequencerBlockEntity sequencer)) {
            this.container = new SimpleContainer(2);
            this.data = new SimpleContainerData(2);
        } else {
            this.container = sequencer;
            // Use the BE's live ContainerData so client sees progress/max updates
            this.data = sequencer.getContainerData();
        }
        this.addDataSlots(this.data);

        this.addSlot(new Slot((Container) this.container, GeneSequencerBlockEntity.SLOT_INPUT, 44, 35));
        this.addSlot(new Slot((Container) this.container, GeneSequencerBlockEntity.SLOT_OUTPUT, 98, 35));

        addPlayerInventory(playerInv);
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();

            int containerEnd = 2;
            int playerStart = containerEnd;
            int playerEnd = this.slots.size();

            if (index < containerEnd) {
                // Machine to the player
                if (!this.moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player to the machine (only accept the tissue item)
                if (stackInSlot.getItem() == ModItems.TISSUE_SAMPLE.get()) {
                    if (!this.moveItemStackTo(stackInSlot,
                            GeneSequencerBlockEntity.SLOT_INPUT,
                            GeneSequencerBlockEntity.SLOT_INPUT + 1,
                            false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}


