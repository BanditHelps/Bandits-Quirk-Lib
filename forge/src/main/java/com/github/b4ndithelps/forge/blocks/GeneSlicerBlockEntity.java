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
    private final java.util.ArrayList<ItemStack> pendingOutputs = new java.util.ArrayList<>();

    public GeneSlicerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_SLICER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneSlicerBlockEntity be) {
        if (!be.running) return; // processing is orchestrated by BioTerminal program; keep a progress bar if desired
        be.progress = Math.min(be.progress + 1, be.maxProgress);
        be.data.set(0, be.progress);
        be.data.set(1, be.maxProgress);
        if (be.progress >= be.maxProgress) {
            // On completion, place any pending outputs into free slots
            if (!be.pendingOutputs.isEmpty()) {
                java.util.ArrayList<ItemStack> remaining = new java.util.ArrayList<>();
                for (ItemStack vial : be.pendingOutputs) {
                    boolean placed = false;
                    for (int i = 0; i < SLOT_OUTPUT_COUNT; i++) {
                        int slot = SLOT_OUTPUT_START + i;
                        if (be.getItem(slot).isEmpty()) { be.setItem(slot, vial); placed = true; break; }
                    }
                    if (!placed) remaining.add(vial);
                }
                be.pendingOutputs.clear();
                be.pendingOutputs.addAll(remaining);
            }
            be.progress = 0;
            // If we still have unplaced outputs due to full inventory, keep running briefly to retry next tick
            if (be.pendingOutputs.isEmpty()) {
                be.running = false;
            } else {
                // Continue running until outputs can be placed; avoid immediate 0-duration loop by bumping max
                be.maxProgress = Math.max(5, be.maxProgress);
            }
            // Broadcast completion so client-side ref screen updates immediately
            if (!level.isClientSide) {
                java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                ItemStack in = be.getItem(SLOT_INPUT);
                if (!in.isEmpty() && in.getItem() == com.github.b4ndithelps.forge.item.ModItems.SEQUENCED_SAMPLE.get()) {
                    net.minecraft.nbt.CompoundTag tag = in.getTag();
                    if (tag != null && tag.contains("genes", 9)) {
                        var list = tag.getList("genes", 10);
                        for (int i = 0; i < list.size(); i++) {
                            net.minecraft.nbt.CompoundTag g = list.getCompound(i);
                            String label = g.getString("name");
                            if (label == null || label.isEmpty()) label = g.getString("id");
                            if (label == null || label.isEmpty()) label = "gene_" + Integer.toString(i + 1);
                            labels.add(label);
                        }
                    }
                }
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> ((net.minecraft.server.level.ServerLevel)level).getChunkAt(pos)),
                        new com.github.b4ndithelps.forge.network.SlicerStateS2CPacket(pos, false, labels)
                );
            }
        }
        be.setChanged();
    }

    /** Queue outputs to be placed when processing completes. Also starts processing. */
    public void enqueueOutputs(java.util.List<ItemStack> outputs) {
        this.pendingOutputs.clear();
        if (outputs != null) this.pendingOutputs.addAll(outputs);
        this.progress = 0;
        this.data.set(0, 0);
        this.data.set(1, this.maxProgress);
        this.running = true;
        setChanged();
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
        if (!this.pendingOutputs.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (ItemStack st : this.pendingOutputs) list.add(st.save(new net.minecraft.nbt.CompoundTag()));
            tag.put("PendingOutputs", list);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
        this.maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 200;
        this.running = tag.contains("Running") && tag.getBoolean("Running");
        this.pendingOutputs.clear();
        if (tag.contains("PendingOutputs", 9)) {
            net.minecraft.nbt.ListTag list = tag.getList("PendingOutputs", 10);
            for (int i = 0; i < list.size(); i++) {
                this.pendingOutputs.add(ItemStack.of(list.getCompound(i)));
            }
        }
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


