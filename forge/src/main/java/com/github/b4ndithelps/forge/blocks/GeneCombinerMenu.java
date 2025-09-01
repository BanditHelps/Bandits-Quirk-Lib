package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class GeneCombinerMenu extends AbstractContainerMenu {
    private final Container container;

    public GeneCombinerMenu(int id, Inventory playerInv, GeneCombinerBlockEntity be) {
        super(ModMenus.GENE_COMBINER.get(), id);
        this.container = be;

        // 4 input slots positioned around center
        // Top (x=62,y=17), Left (x=26,y=35), Right (x=98,y=35), Bottom (x=62,y=53)
        this.addSlot(new Slot(be, 0, 62, 17) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot(be, 1, 26, 35) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot(be, 2, 98, 35) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot(be, 3, 62, 53) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });

        // Output slot centered (x=134,y=35) like BioPrinter for consistency
        this.addSlot(new Slot(be, GeneCombinerBlockEntity.SLOT_OUTPUT, 134, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        addPlayerInventory(playerInv);
    }

    public GeneCombinerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.GENE_COMBINER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof GeneCombinerBlockEntity combiner)) {
            this.container = new SimpleContainer(GeneCombinerBlockEntity.SLOT_COUNT);
        } else {
            this.container = combiner;
        }

        this.addSlot(new Slot((Container) this.container, 0, 62, 17) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot((Container) this.container, 1, 26, 35) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot((Container) this.container, 2, 98, 35) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });
        this.addSlot(new Slot((Container) this.container, 3, 62, 53) { @Override public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); } });

        this.addSlot(new Slot((Container) this.container, GeneCombinerBlockEntity.SLOT_OUTPUT, 134, 35) { @Override public boolean mayPlace(ItemStack stack) { return false; } });

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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int containerEnd = GeneCombinerBlockEntity.SLOT_COUNT; // 0..4
            int playerStart = containerEnd;
            int playerEnd = this.slots.size();

            if (index < containerEnd) {
                // From machine to player
                if (!this.moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to machine: only gene vials into input slots (0..3)
                if (!isGeneVial(stackInSlot)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, 0, GeneCombinerBlockEntity.SLOT_INPUT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return itemstack;
    }

    private static boolean isGeneVial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        return item instanceof GeneVialItem
                || item == ModItems.GENE_VIAL_COSMETIC.get()
                || item == ModItems.GENE_VIAL_RESISTANCE.get()
                || item == ModItems.GENE_VIAL_BUILDER.get()
                || item == ModItems.GENE_VIAL_QUIRK.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}


