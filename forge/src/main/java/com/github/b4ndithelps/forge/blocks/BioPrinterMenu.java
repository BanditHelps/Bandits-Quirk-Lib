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

public class BioPrinterMenu extends AbstractContainerMenu {
    private final Container container;

    public BioPrinterMenu(int id, Inventory playerInv, BioPrinterBlockEntity be) {
        super(ModMenus.BIO_PRINTER.get(), id);
        this.container = be;

        // 12 input slots arranged 4x3 starting at (26,17), spacing 18
        int startX = 26;
        int startY = 17;
        int idx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                final int slotIndex = idx++;
                this.addSlot(new Slot(be, slotIndex, startX + col * 18, startY + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); }
                });
            }
        }

        // Output slot at (134,35). No insertion allowed
        this.addSlot(new Slot(be, BioPrinterBlockEntity.SLOT_OUTPUT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        addPlayerInventory(playerInv);
    }

    public BioPrinterMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.BIO_PRINTER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof BioPrinterBlockEntity printer)) {
            this.container = new SimpleContainer(BioPrinterBlockEntity.SLOT_COUNT);
        } else {
            this.container = printer;
        }

        // 12 inputs 4x3
        int startX = 26;
        int startY = 17;
        int idx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                this.addSlot(new Slot((Container) this.container, idx++, startX + col * 18, startY + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) { return isGeneVial(stack); }
                });
            }
        }

        // Output
        this.addSlot(new Slot((Container) this.container, BioPrinterBlockEntity.SLOT_OUTPUT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

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

            int containerEnd = BioPrinterBlockEntity.SLOT_COUNT; // 0..12
            int playerStart = containerEnd;
            int playerEnd = this.slots.size();

            if (index < containerEnd) {
                // From machine to player
                if (!this.moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to machine: only gene vials into input slots (0..11)
                if (!isGeneVial(stackInSlot)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stackInSlot, 0, BioPrinterBlockEntity.SLOT_INPUT_COUNT, false)) {
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


