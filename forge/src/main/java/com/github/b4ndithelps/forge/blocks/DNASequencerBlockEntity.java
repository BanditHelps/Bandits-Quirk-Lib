package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.console.BasicConsoleCommands;
import com.github.b4ndithelps.forge.console.ConsoleCommandRegistry;
import com.github.b4ndithelps.forge.console.ConsoleContext;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.BlockPos;
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

public class DNASequencerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, net.minecraft.world.WorldlyContainer {
    public static final int SLOT_DISK = 0;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ContainerData data = new SimpleContainerData(2); // 0: progress, 1: max

    private int progress;
    private int maxProgress = 200;
    private StringBuilder consoleBuffer = new StringBuilder();
    private String lastOutput = "";
    private boolean booted = false;

    // Simple scheduling support for console animations
    private final java.util.ArrayDeque<String> scheduledLines = new java.util.ArrayDeque<>();
    private int ticksUntilNextScheduledLine = 0;
    private int scheduledLineIntervalTicks = 0;

    public DNASequencerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DNA_SEQUENCER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DNASequencerBlockEntity be) {
        if (!be.booted) {
            // Ensure default commands are registered once server-side
            BasicConsoleCommands.registerDefaults(ConsoleCommandRegistry.getInstance());
            be.appendConsole("DNA Sequencer v0.1");
            be.appendConsole("Booting subsystems...");
            be.appendConsole("Ready. Type 'help' for commands.");
            be.booted = true;
            be.setChanged();
            if (be.level != null) be.level.sendBlockUpdated(be.worldPosition, state, state, 3);
        }

        // Drain scheduled console lines to allow simple typewriter/step animations
        if (!be.scheduledLines.isEmpty()) {
            if (be.ticksUntilNextScheduledLine <= 0) {
                String next = be.scheduledLines.pollFirst();
                if (next != null) be.appendConsole(next);
                be.ticksUntilNextScheduledLine = be.scheduledLineIntervalTicks;
            } else {
                be.ticksUntilNextScheduledLine--;
            }
        }
        ItemStack input = ItemStack.EMPTY; // no input slot in console-only mode
        if (!input.isEmpty() && input.getItem() == ModItems.TISSUE_SAMPLE.get()) {
            be.progress++;
            be.data.set(0, be.progress);
            be.data.set(1, be.maxProgress);
            if (be.progress >= be.maxProgress) {
                be.finishProcessing();
                be.progress = 0;
            }
            be.setChanged();
        } else {
            if (be.progress != 0) {
                be.progress = 0;
                be.data.set(0, 0);
                be.setChanged();
            }
        }
    }

    private void finishProcessing() {
        ItemStack input = ItemStack.EMPTY;
        if (input.isEmpty()) return;
        CompoundTag inTag = input.getTag();
        if (inTag == null) return;

        // Sequenced Sample
        ItemStack sequenced = new ItemStack(ModItems.SEQUENCED_SAMPLE.get());
        CompoundTag seqTag = sequenced.getOrCreateTag();
        seqTag.putLong("GenomeSeed", inTag.getLong("GenomeSeed"));
        if (inTag.contains("Traits")) seqTag.put("Traits", inTag.get("Traits"));
        seqTag.putInt("Quality", inTag.getInt("Quality"));
        seqTag.putString("SourceEntity", inTag.getString("EntityType"));

        // Readout
        ItemStack readout = new ItemStack(ModItems.READOUT.get());
        CompoundTag roTag = readout.getOrCreateTag();
        roTag.putLong("GenomeSeed", inTag.getLong("GenomeSeed"));
        roTag.putString("EncodedSequence", "ACTT-G7F-XXY-Z22");
        int q = Math.max(0, inTag.getInt("Quality") - 12);
        roTag.putInt("Quality", q);

        // No output slots in console-only mode
    }

    public void appendConsole(String text) {
        consoleBuffer.append(text);
        consoleBuffer.append('\n');
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> ((net.minecraft.server.level.ServerLevel)this.level).getChunkAt(this.worldPosition)),
                new com.github.b4ndithelps.forge.network.ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
        }
    }

    public void runCommand(String command) {
        appendConsole("> " + command);
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.isEmpty()) return;
        String[] split = trimmed.split("\\s+");
        String name = split[0];
        java.util.List<String> args = java.util.Arrays.asList(split).subList(1, split.length);
        var registry = ConsoleCommandRegistry.getInstance();
        var ctx = new ConsoleContext(this);
        registry.find(name).ifPresentOrElse(cmd -> cmd.execute(ctx, args), () -> {
            lastOutput = "Unknown command: " + name;
            appendConsole(lastOutput);
        });
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> ((net.minecraft.server.level.ServerLevel)this.level).getChunkAt(this.worldPosition)),
                new com.github.b4ndithelps.forge.network.ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
        }
    }

    public String getConsoleText() { return consoleBuffer.toString(); }

    public int getProgress() { return this.progress; }
    public int getMaxProgress() { return this.maxProgress; }

    // Client-side helper to update console text via packets
    public void clientSetConsoleText(String text) {
        this.consoleBuffer = new StringBuilder(text != null ? text : "");
    }

    // Schedule multiple lines with a delay between each
    public void queueConsoleLines(java.util.List<String> lines, int ticksBetween) {
        if (lines == null || lines.isEmpty()) return;
        this.scheduledLines.addAll(lines);
        this.scheduledLineIntervalTicks = Math.max(0, ticksBetween);
        this.ticksUntilNextScheduledLine = 0; // start immediately next tick
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
        tag.putString("ConsoleText", this.consoleBuffer.toString());
        tag.putBoolean("Booted", this.booted);
        tag.putInt("QueueInterval", this.scheduledLineIntervalTicks);
        tag.putInt("QueueTicks", this.ticksUntilNextScheduledLine);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
        this.consoleBuffer = new StringBuilder(tag.getString("ConsoleText"));
        this.booted = tag.contains("Booted") && tag.getBoolean("Booted");
        this.scheduledLineIntervalTicks = tag.contains("QueueInterval") ? tag.getInt("QueueInterval") : 0;
        this.ticksUntilNextScheduledLine = tag.contains("QueueTicks") ? tag.getInt("QueueTicks") : 0;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bandits_quirk_lib.dna_sequencer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DNASequencerMenu(id, inv, this, data);
    }

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
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, net.minecraft.core.Direction direction) { return index == SLOT_DISK; }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, net.minecraft.core.Direction direction) { return index == SLOT_DISK; }

    
}


