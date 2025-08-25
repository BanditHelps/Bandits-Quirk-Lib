package com.github.b4ndithelps.forge.blocks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class GeneSlicerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public GeneSlicerMenu(int id, Inventory playerInv, GeneSlicerBlockEntity be, ContainerData data) {
        super(ModMenus.GENE_SLICER.get(), id);
        this.container = be;
        this.data = data;
        this.addDataSlots(data);

        // Input on the left
        this.addSlot(new Slot(be, GeneSlicerBlockEntity.SLOT_INPUT, 26, 35));

        // 6 outputs arranged 3x2 on the right starting at x=98, y=26
        int startX = 98;
        int startY = 26;
        int idx = GeneSlicerBlockEntity.SLOT_OUTPUT_START;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(be, idx++, startX + col * 18, startY + row * 18));
            }
        }

        addPlayerInventory(playerInv);
    }

    public GeneSlicerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        super(ModMenus.GENE_SLICER.get(), id);
        var pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (!(be instanceof GeneSlicerBlockEntity slicer)) {
            this.container = new SimpleContainer(1 + GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT);
            this.data = new SimpleContainerData(2);
        } else {
            this.container = slicer;
            this.data = slicer.getContainerData();
        }
        this.addDataSlots(this.data);

        // Input
        this.addSlot(new Slot((Container) this.container, GeneSlicerBlockEntity.SLOT_INPUT, 26, 35));

        // Outputs 3x2
        int startX = 98;
        int startY = 26;
        int idx = GeneSlicerBlockEntity.SLOT_OUTPUT_START;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot((Container) this.container, idx++, startX + col * 18, startY + row * 18));
            }
        }

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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}


