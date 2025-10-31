package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.GeneDatabaseItem;
import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;

public class BioTerminalRefBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.WorldlyContainer {
    public static final int SLOT_DISK = 0;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ContainerData data = new SimpleContainerData(1);

    // Identification queue (max 3 concurrent)
    private static final int MAX_IDENTIFICATIONS = 3;
    private final java.util.List<IdentificationTask> identificationTasks = new java.util.ArrayList<>();

    public static final class IdentificationTask {
        public String geneId = "";
        public int quality = 0;
        public int progress = 0;
        public int max = 0;
        public boolean complete = false;
    }

    public BioTerminalRefBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_TERMINAL_REF.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BioTerminalRefBlockEntity be) {
        if (level.isClientSide) return;
        // Advance identification tasks while DB present
        int running = 0;
        for (IdentificationTask task : be.identificationTasks) {
            if (task.complete) continue;
            if (running >= MAX_IDENTIFICATIONS) break;
            if (!be.hasDatabase()) break;
            task.progress = Math.min(task.progress + 1, Math.max(1, task.max));
            running++;
            if (task.progress >= task.max) {
                ResourceLocation gid;
                try { gid = new ResourceLocation(task.geneId); } catch (Exception e) { gid = null; }
                if (gid != null && be.hasDatabase()) {
                    be.markGeneKnown(gid);
                    task.complete = true;
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bandits_quirk_lib.bio_terminal_ref");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BioTerminalRefMenu(id, inv, this, data);
    }

    // --- Disk helpers ---
    public ItemStack getDatabaseStack() { return this.items.get(SLOT_DISK); }
    public void setDatabaseStack(ItemStack stack) { this.items.set(SLOT_DISK, stack); setChanged(); }
    public boolean hasDatabase() { ItemStack s = getDatabaseStack(); return s != null && !s.isEmpty() && s.getItem() instanceof GeneDatabaseItem; }
    public boolean isGeneKnown(ResourceLocation id) { return GeneDatabaseItem.isKnown(getDatabaseStack(), id); }
    public void markGeneKnown(ResourceLocation id) { GeneDatabaseItem.addKnown(getDatabaseStack(), id); setChanged(); }

    // --- Identification API (mirrors main terminal behavior) ---
    public java.util.List<IdentificationTask> getIdentificationTasks() { return java.util.Collections.unmodifiableList(this.identificationTasks); }

    public boolean canStartIdentification(ResourceLocation geneId) {
        if (geneId == null) return false;
        if (!hasDatabase()) return false;
        if (isGeneKnown(geneId)) return false;
        int active = 0;
        for (IdentificationTask t : identificationTasks) if (!t.complete) active++;
        return active < MAX_IDENTIFICATIONS;
    }

    public boolean startIdentification(ResourceLocation geneId, int quality) {
        if (!canStartIdentification(geneId)) return false;
        Gene g = null;
        try { g = GeneRegistry.get(geneId); } catch (Exception ignored) {}
        int ticks = computeIdentifyTicks(g, quality);
        IdentificationTask t = new IdentificationTask();
        t.geneId = geneId.toString();
        t.quality = quality;
        t.max = Math.max(1, ticks);
        t.progress = 0;
        t.complete = false;
        this.identificationTasks.add(t);
        setChanged();
        return true;
    }

    private int computeIdentifyTicks(Gene g, int quality) {
        int base;
        if (g == null) base = 20 * 15;
        else {
            base = switch (g.getRarity()) {
                case common -> 20 * 5;
                case uncommon -> 20 * 10;
                case rare -> 20 * 20;
                case very_rare -> 20 * 40;
            };
        }
        int qualityPenalty = Math.max(0, 100 - Math.max(0, Math.min(100, quality)));
        int extra = (int)Math.round(base * (qualityPenalty / 200.0));
        return Math.max(20 * 3, base + extra);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        ListTag tasks = new ListTag();
        for (IdentificationTask t : identificationTasks) {
            CompoundTag ct = new CompoundTag();
            ct.putString("GeneId", t.geneId);
            ct.putInt("Quality", t.quality);
            ct.putInt("Progress", t.progress);
            ct.putInt("Max", Math.max(1, t.max));
            ct.putBoolean("Complete", t.complete);
            tasks.add(ct);
        }
        tag.put("IdentifyTasks", tasks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
        this.identificationTasks.clear();
        if (tag.contains("IdentifyTasks", 9)) {
            ListTag tasks = tag.getList("IdentifyTasks", 10);
            for (int i = 0; i < tasks.size(); i++) {
                CompoundTag ct = tasks.getCompound(i);
                IdentificationTask t = new IdentificationTask();
                t.geneId = ct.getString("GeneId");
                t.quality = ct.getInt("Quality");
                t.progress = ct.getInt("Progress");
                t.max = Math.max(1, ct.getInt("Max"));
                t.complete = ct.contains("Complete") && ct.getBoolean("Complete");
                this.identificationTasks.add(t);
            }
        }
    }

    // --- Container impl ---
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
    public int[] getSlotsForFace(net.minecraft.core.Direction side) { return new int[]{SLOT_DISK}; }
    @Override
    public boolean canPlaceItem(int index, ItemStack stack) { return index == SLOT_DISK && stack != null && !stack.isEmpty() && stack.getItem() instanceof GeneDatabaseItem; }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, net.minecraft.core.Direction direction) { return canPlaceItem(index, stack); }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, net.minecraft.core.Direction direction) { return index == SLOT_DISK; }
}




