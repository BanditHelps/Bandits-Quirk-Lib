package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.SequencerStateS2CPacket;
import com.github.b4ndithelps.genetics.GeneticsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Processes a tissue sample into a sequenced sample when commanded by an adjacent BioTerminal.
 */
public class GeneSequencerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
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
                // Broadcast analyzed completion to clients for ref-screen refresh
                if (!level.isClientSide) {
                    BQLNetwork.CHANNEL.send(
                            PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel) level).getChunkAt(pos)),
                            new SequencerStateS2CPacket(pos, false, true)
                    );
                }
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
        // New schema propagation (ensure names exist; if not, add deterministic names)
        if (inTag.contains("genes", 9)) {
            ListTag inGenes = inTag.getList("genes", 10);
            ListTag outGenes = new ListTag();
            String uuidStr = inTag.getString("entity_uuid");
            UUID uuid = null;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception ignored) {
            }
            for (int i = 0; i < inGenes.size(); i++) {
                CompoundTag gIn = inGenes.getCompound(i);
                CompoundTag gOut = gIn.copy();
                if (!gOut.contains("name", 8)) {
                    String id = gOut.getString("id");
                    if (uuid != null && id != null && !id.isEmpty()) {
                        String display = GeneticsHelper.generateStableGeneName(uuid, new ResourceLocation(id), i);
                        gOut.putString("name", display);
                    }
                }
                outGenes.add(gOut);
            }
            seqTag.put("genes", outGenes);
        }
        if (inTag.contains("entity_name", 8)) seqTag.putString("entity_name", inTag.getString("entity_name"));
        if (inTag.contains("entity_uuid", 8)) seqTag.putString("entity_uuid", inTag.getString("entity_uuid"));
        // propagate or initialize layout salt so layout differs per sample
        if (inTag.contains("layout_salt", 4)) seqTag.putLong("layout_salt", inTag.getLong("layout_salt"));
        else seqTag.putLong("layout_salt", this.level != null ? this.level.random.nextLong() : 0L);
        // Legacy compatibility -> convert
        if (!seqTag.contains("genes", 9)) {
            if (inTag.contains("Traits", 9)) {
                ListTag legacy = inTag.getList("Traits", 8);
                ListTag out = new ListTag();
                int q = inTag.getInt("Quality");
                for (int i = 0; i < legacy.size(); i++) {
                    String trait = legacy.getString(i);
                    CompoundTag g = new CompoundTag();
                    g.putString("id", "bandits_quirk_lib:legacy." + trait.toLowerCase());
                    g.putInt("quality", q > 0 ? q : 50);
                    // deterministic name fallback not possible without original uuid; use pseudo
                    String name = "gene_" + String.format("%04x", Math.abs(trait.hashCode()) & 0xFFFF);
                    g.putString("name", name);
                    out.add(g);
                }
                seqTag.put("genes", out);
            }
        }

        items.set(SLOT_OUTPUT, sequenced);
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // Control API for BioTerminal
    public void startProcessing() {
        this.running = true;
    }

    public void stopProcessing() {
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * Exposes container data for menu syncing on client.
     */
    public ContainerData getContainerData() {
        return data;
    }

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
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{SLOT_INPUT, SLOT_OUTPUT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUTPUT;
    }
}