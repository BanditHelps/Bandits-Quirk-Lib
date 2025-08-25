package com.github.b4ndithelps.forge.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Accepts a sequenced sample and, when commanded by a BioTerminal program, slices it into up to six outputs.
 * Actual slicing logic will be implemented by the program; this BE just provides inventory and running state.
 */
public class GeneSlicerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_START = 1; // 1..6 inclusive
    public static final int SLOT_OUTPUT_COUNT = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(1 + SLOT_OUTPUT_COUNT, ItemStack.EMPTY);
    private final ContainerData data = new SimpleContainerData(2); // 0 progress, 1 max

    private int progress = 0;
    private int maxProgress = 200;
    private boolean running = false;

    public GeneSlicerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_SLICER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneSlicerBlockEntity be) {
        if (!be.running) return; // processing is orchestrated by BioTerminal program; keep a progress bar if desired
        be.progress = Math.min(be.progress + 1, be.maxProgress);
        be.data.set(0, be.progress);
        be.data.set(1, be.maxProgress);
        if (be.progress >= be.maxProgress) {
            // Program is expected to populate outputs; we simply stop running here.
            be.progress = 0;
            be.running = false;
        }
        be.setChanged();
    }

    // Control API for BioTerminal/programs
    public void startProcessing() { this.running = true; }
    public void stopProcessing() { this.running = false; }
    public boolean isRunning() { return running; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public ContainerData getContainerData() { return data; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putBoolean("Running", this.running);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 200;
        this.running = tag.contains("Running") && tag.getBoolean("Running");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bandits_quirk_lib.gene_slicer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GeneSlicerMenu(id, inv, this, data);
    }

    // Inventory impl
    @Override
    public int getContainerSize() { return items.size(); }
    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }
    @Override
    public ItemStack removeItem(int slot, int amount) { ItemStack r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); }
    @Override
    public boolean stillValid(Player player) { return player.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64.0; }
    @Override
    public void clearContent() { items.clear(); }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[1 + SLOT_OUTPUT_COUNT];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) { return index == SLOT_INPUT; }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return index >= SLOT_OUTPUT_START; }
}


