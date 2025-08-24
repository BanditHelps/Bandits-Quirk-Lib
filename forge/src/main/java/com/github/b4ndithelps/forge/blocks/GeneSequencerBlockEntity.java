package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
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
 * Processes a tissue sample into a sequenced sample when commanded by an adjacent BioTerminal.
 */
public class GeneSequencerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, net.minecraft.world.WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final ContainerData data = new SimpleContainerData(2); // 0 progress, 1 max

    private int progress = 0;
    private int maxProgress = 200;
    private boolean running = false; // only runs when commanded

    public GeneSequencerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_SEQUENCER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneSequencerBlockEntity be) {
        if (!be.running) return;
        ItemStack input = be.items.get(SLOT_INPUT);
        if (!input.isEmpty() && input.getItem() == ModItems.TISSUE_SAMPLE.get()) {
            be.progress++;
            be.data.set(0, be.progress);
            be.data.set(1, be.maxProgress);
            if (be.progress >= be.maxProgress) {
                be.finishProcessing();
                be.progress = 0;
                be.running = false;
            }
            be.setChanged();
        } else {
            if (be.progress != 0) {
                be.progress = 0;
                be.data.set(0, 0);
                be.setChanged();
                be.running = false;
            }
        }
    }

    private void finishProcessing() {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) return;
        CompoundTag inTag = input.getTag();
        if (inTag == null) return;

        ItemStack sequenced = new ItemStack(ModItems.SEQUENCED_SAMPLE.get());
        CompoundTag seqTag = sequenced.getOrCreateTag();
        seqTag.putLong("GenomeSeed", inTag.getLong("GenomeSeed"));
        if (inTag.contains("Traits")) seqTag.put("Traits", inTag.get("Traits"));
        seqTag.putInt("Quality", inTag.getInt("Quality"));
        seqTag.putString("SourceEntity", inTag.getString("EntityType"));

        items.set(SLOT_OUTPUT, sequenced);
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        setChanged();
    }

    // Control API for BioTerminal
    public void startProcessing() { this.running = true; }
    public void stopProcessing() { this.running = false; }
    public boolean isRunning() { return running; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

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
        return Component.translatable("block.bandits_quirk_lib.gene_sequencer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GeneSequencerMenu(id, inv, this, data);
    }

    // Inventory
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
    public int[] getSlotsForFace(Direction side) { return new int[]{SLOT_INPUT, SLOT_OUTPUT}; }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) { return index == SLOT_INPUT; }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return index == SLOT_OUTPUT; }
}


