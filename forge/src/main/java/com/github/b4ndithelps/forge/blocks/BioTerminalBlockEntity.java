package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.console.BasicConsoleCommands;
import com.github.b4ndithelps.forge.console.ConsoleCommandRegistry;
import com.github.b4ndithelps.forge.console.ConsoleContext;
import com.github.b4ndithelps.forge.console.ConsoleProgram;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ConsoleSyncS2CPacket;
import com.github.b4ndithelps.forge.network.ConsoleHistorySyncS2CPacket;
import com.github.b4ndithelps.forge.network.ProgramScreenSyncS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BioTerminalBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
	public static final int SLOT_DISK = 0;
	private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private final ContainerData data = new SimpleContainerData(2); // 0: progress, 1: max

	private int progress;
	private int maxProgress = 200;
	private StringBuilder consoleBuffer = new StringBuilder();
	private String lastOutput = "";
	private boolean booted = false;

	private static boolean COMMANDS_INITIALIZED = false;

	private final ArrayDeque<String> scheduledLines = new ArrayDeque<>();
	private int ticksUntilNextScheduledLine = 0;
	private int scheduledLineIntervalTicks = 0;

	private boolean singleLineAnimating = false;
	private String singleLineTarget = "";
	private int singleLineIndex = 0;
	private int singleLineIntervalTicks = 0;
	private int ticksUntilNextScheduledChar = 0;
	private boolean singleLineAppendNewlineWhenDone = true;

	private final ArrayList<String> commandHistory = new ArrayList<>();
	private int historyCursor = -1;
	private static final int COMMAND_HISTORY_LIMIT = 10;

	// Simple program stack (single active program sufficient for now)
	private ConsoleProgram activeProgram = null;
	private String programScreenText = "";
	private int programTickCounter = 0;

	public BioTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BIO_TERMINAL.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BioTerminalBlockEntity be) {
		if (!level.isClientSide && !COMMANDS_INITIALIZED) {
			BasicConsoleCommands.registerDefaults(ConsoleCommandRegistry.getInstance());
			COMMANDS_INITIALIZED = true;
		}

		if (!be.booted) {
			BasicConsoleCommands.registerDefaults(ConsoleCommandRegistry.getInstance());
			be.appendConsole("Bio Terminal v0.1");
			be.appendConsole("Booting subsystems...");
			be.appendConsole("Ready. Type 'help' for commands.");
			be.booted = true;
			be.setChanged();
			if (be.level != null) be.level.sendBlockUpdated(be.worldPosition, state, state, 3);
		}

		if (!be.scheduledLines.isEmpty()) {
			if (be.ticksUntilNextScheduledLine <= 0) {
				String next = be.scheduledLines.pollFirst();
				if (next != null) be.appendConsole(next);
				be.ticksUntilNextScheduledLine = be.scheduledLineIntervalTicks;
			} else {
				be.ticksUntilNextScheduledLine--;
			}
		}

		if (be.singleLineAnimating) {
			if (be.ticksUntilNextScheduledChar <= 0) {
				if (be.singleLineIndex < be.singleLineTarget.length()) {
					be.singleLineIndex++;
					String frame = be.singleLineTarget.substring(0, be.singleLineIndex);
					be.updateConsoleLastLine(frame);
					be.ticksUntilNextScheduledChar = be.singleLineIntervalTicks;
				}
				if (be.singleLineIndex >= be.singleLineTarget.length()) {
					be.singleLineAnimating = false;
					if (be.singleLineAppendNewlineWhenDone) {
						be.consoleBuffer.append('\n');
						be.setChanged();
						if (be.level != null && !be.level.isClientSide) {
							be.level.sendBlockUpdated(be.worldPosition, state, state, 3);
							BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)be.level).getChunkAt(be.worldPosition)),
								new ConsoleSyncS2CPacket(be.worldPosition, be.consoleBuffer.toString()));
						}
					}
				}
			} else {
				be.ticksUntilNextScheduledChar--;
			}
		}
		// Program auto-update tick (every 10 ticks)
		if (be.activeProgram != null && !level.isClientSide) {
			be.programTickCounter++;
			if (be.programTickCounter >= 10) {
				be.programTickCounter = 0;
				be.activeProgram.onTick(new ConsoleContext(be));
			}
		}

		ItemStack input = ItemStack.EMPTY;
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

		ItemStack sequenced = new ItemStack(ModItems.SEQUENCED_SAMPLE.get());
		CompoundTag seqTag = sequenced.getOrCreateTag();
		seqTag.putLong("GenomeSeed", inTag.getLong("GenomeSeed"));
		if (inTag.contains("Traits")) seqTag.put("Traits", inTag.get("Traits"));
		seqTag.putInt("Quality", inTag.getInt("Quality"));
		seqTag.putString("SourceEntity", inTag.getString("EntityType"));

		ItemStack readout = new ItemStack(ModItems.READOUT.get());
		CompoundTag roTag = readout.getOrCreateTag();
		roTag.putLong("GenomeSeed", inTag.getLong("GenomeSeed"));
		roTag.putString("EncodedSequence", "ACTT-G7F-XXY-Z22");
		int q = Math.max(0, inTag.getInt("Quality") - 12);
		roTag.putInt("Quality", q);
	}

	public void appendConsole(String text) {
		consoleBuffer.append(text);
		consoleBuffer.append('\n');
		setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)this.level).getChunkAt(this.worldPosition)),
				new ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
		}
	}

	public void clearConsole() {
		consoleBuffer = new StringBuilder();
		setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)this.level).getChunkAt(this.worldPosition)),
					new ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
		}
	}

	public void runCommand(String command) {
		appendConsole("> " + command);
		String trimmed = command == null ? "" : command.trim();
		if (trimmed.isEmpty()) return;
		if (commandHistory.isEmpty() || !commandHistory.get(commandHistory.size() - 1).equals(trimmed)) {
			commandHistory.add(trimmed);
			while (commandHistory.size() > COMMAND_HISTORY_LIMIT) commandHistory.remove(0);
		}
		historyCursor = -1;
		String[] split = trimmed.split("\\s+");
		String name = split[0];
		List<String> args = Arrays.asList(split).subList(1, split.length);
		var ctx = new ConsoleContext(this);

		// If a program is active, let it handle first
		if (activeProgram != null && activeProgram.handle(ctx, name, args)) {
			// handled by program
		} else {
			var registry = ConsoleCommandRegistry.getInstance();
			registry.find(name).ifPresentOrElse(cmd -> cmd.execute(ctx, args), () -> {
				lastOutput = "Unknown command: " + name;
				appendConsole(lastOutput);
			});
		}
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
			var serverLevel = (net.minecraft.server.level.ServerLevel)this.level;
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(this.worldPosition)),
				new ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
			// Also sync history immediately so client-side history navigation reflects new entries
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(this.worldPosition)),
				new ConsoleHistorySyncS2CPacket(this.worldPosition, new ArrayList<>(this.commandHistory), this.historyCursor));
		}
	}

	public String getConsoleText() { return consoleBuffer.toString(); }

	public int getProgress() { return this.progress; }
	public int getMaxProgress() { return this.maxProgress; }

	public void clientSetConsoleText(String text) {
		this.consoleBuffer = new StringBuilder(text != null ? text : "");
	}

	public void clientSetHistory(List<String> history, int cursor) {
		this.commandHistory.clear();
		if (history != null) {
			this.commandHistory.addAll(history);
		}
		this.historyCursor = cursor;
	}

	public String historyPrev() {
		if (commandHistory.isEmpty()) return "";
		if (historyCursor == -1) historyCursor = commandHistory.size() - 1;
		else if (historyCursor > 0) historyCursor--;
		return commandHistory.get(historyCursor);
	}

	public String historyNext() {
		if (commandHistory.isEmpty()) return "";
		if (historyCursor == -1) return "";
		if (historyCursor < commandHistory.size() - 1) {
			historyCursor++;
			return commandHistory.get(historyCursor);
		} else {
			historyCursor = -1;
			return "";
		}
	}

	public void queueConsoleLines(List<String> lines, int ticksBetween) {
		if (lines == null || lines.isEmpty()) return;
		this.scheduledLines.addAll(lines);
		this.scheduledLineIntervalTicks = Math.max(0, ticksBetween);
		this.ticksUntilNextScheduledLine = 0;
	}

	public void queueSingleConsoleLine(String line, int ticksBetween) {
		if (line == null) line = "";
		if (line.isEmpty()) return;
		if (consoleBuffer.length() > 0 && consoleBuffer.charAt(consoleBuffer.length() - 1) != '\n') {
			consoleBuffer.append('\n');
		}
		this.singleLineAnimating = true;
		this.singleLineTarget = line;
		this.singleLineIndex = 0;
		this.singleLineIntervalTicks = Math.max(0, ticksBetween);
		this.ticksUntilNextScheduledChar = 0;
		this.singleLineAppendNewlineWhenDone = true;
	}

	// --- Program control API ---
	public void pushProgram(ConsoleProgram program) {
		if (program == null) return;
		if (this.activeProgram != null) this.activeProgram.onExit(new ConsoleContext(this));
		this.activeProgram = program;
		this.activeProgram.onEnter(new ConsoleContext(this));
	}

	public void exitCurrentProgram() {
		if (this.activeProgram != null) {
			this.activeProgram.onExit(new ConsoleContext(this));
			this.activeProgram = null;
			appendConsole("Exited program.");
			setProgramScreenText("");
		}
	}

	public void setProgramScreenText(String text) {
		this.programScreenText = text == null ? "" : text;
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)this.level).getChunkAt(this.worldPosition)),
				new ProgramScreenSyncS2CPacket(this.worldPosition, this.programScreenText));
		}
	}

	public String getProgramScreenText() { return this.programScreenText; }

	private void updateConsoleLastLine(String newContent) {
		int start = 0;
		int lastNewline = consoleBuffer.lastIndexOf("\n");
		if (lastNewline >= 0) start = lastNewline + 1;
		consoleBuffer.delete(start, consoleBuffer.length());
		consoleBuffer.append(newContent);
		setChanged();
		if (this.level != null && !this.level.isClientSide) {
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> ((ServerLevel)this.level).getChunkAt(this.worldPosition)),
				new ConsoleSyncS2CPacket(this.worldPosition, this.consoleBuffer.toString()));
		}
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
		ListTag hist = new ListTag();
		for (String cmd : this.commandHistory) hist.add(StringTag.valueOf(cmd));
		tag.put("CommandHistory", hist);
		tag.putInt("HistoryCursor", this.historyCursor);
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
		this.commandHistory.clear();
		if (tag.contains("CommandHistory", 9)) {
			ListTag list = tag.getList("CommandHistory", 8);
			for (int i = 0; i < list.size(); i++) {
				this.commandHistory.add(list.getString(i));
			}
		}
		if (tag.contains("HistoryCursor")) this.historyCursor = tag.getInt("HistoryCursor");
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		this.saveAdditional(tag);
		return tag;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.bandits_quirk_lib.bio_terminal");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new BioTerminalMenu(id, inv, this, data);
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
	public int[] getSlotsForFace(Direction side) { return new int[]{SLOT_DISK}; }
	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) { return index == SLOT_DISK; }
	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return index == SLOT_DISK; }

	// Client-side setter for program screen
	public void clientSetProgramScreenText(String text) {
		this.programScreenText = text == null ? "" : text;
	}

	public String getProgramScreenTextClient() { return this.programScreenText; }
}


