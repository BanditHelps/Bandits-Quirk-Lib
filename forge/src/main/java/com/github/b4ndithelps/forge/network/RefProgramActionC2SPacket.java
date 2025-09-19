package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSlicerBlockEntity;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Simple C2S message for the ref-screen programs to request actions, e.g. start a sequencer.
 */
public class RefProgramActionC2SPacket {
    private final BlockPos terminalPos;
    private final String action;
    private final BlockPos targetPos; // optional, may be null for some actions

    public RefProgramActionC2SPacket(BlockPos terminalPos, String action, BlockPos targetPos) {
        this.terminalPos = terminalPos;
        this.action = action == null ? "" : action;
        this.targetPos = targetPos;
    }

    public static void encode(RefProgramActionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeUtf(msg.action, 128);
        buf.writeBoolean(msg.targetPos != null);
        if (msg.targetPos != null) buf.writeBlockPos(msg.targetPos);
    }

    public static RefProgramActionC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos term = buf.readBlockPos();
        String act = buf.readUtf(128);
        BlockPos target = null;
        if (buf.readBoolean()) target = buf.readBlockPos();
        return new RefProgramActionC2SPacket(term, act, target);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.level() == null) return;
            BlockEntity be = player.level().getBlockEntity(this.terminalPos);
            if (!(be instanceof BioTerminalRefBlockEntity)) return;

            if ("analyze.start".equals(this.action) && this.targetPos != null) {
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (target instanceof GeneSequencerBlockEntity seq) {
                    // Validate the sequencer is connected via cables to the terminal
                    var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSequencerBlockEntity);
                    boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                    if (ok) {
                        seq.startProcessing();
                        // Notify clients immediately that it is running
                        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> ((net.minecraft.server.level.ServerLevel)player.level()).getChunkAt(this.targetPos)),
                            new com.github.b4ndithelps.forge.network.SequencerStateS2CPacket(this.targetPos, true, false)
                        );
                    }
                }
            } else if ("analyze.sync".equals(this.action)) {
                // Send current state for all connected sequencers to the requesting player
                var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSequencerBlockEntity);
                for (var t : connected) {
                    if (t instanceof GeneSequencerBlockEntity seq) {
                        boolean running = seq.isRunning();
                        boolean analyzed = false;
                        var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                        analyzed = !out.isEmpty() && out.getTag() != null;
                        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new com.github.b4ndithelps.forge.network.SequencerStateS2CPacket(seq.getBlockPos(), running, analyzed)
                        );
                    }
                }
            } else if ("catalog.sync".equals(this.action)) {
                // Build catalog entries for connected fridges and sequencers and send to player
                java.util.ArrayList<com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO> list = new java.util.ArrayList<>();
                // Fridges first
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> fridges = new java.util.ArrayList<>();
                var connectedFridges = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                for (var t : connectedFridges) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) fridges.add(f);
                if (!fridges.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[VIALS]", -1, -1));
                    for (int f = 0; f < fridges.size(); f++) {
                        var fridge = fridges.get(f);
                        for (int s = 0; s < com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                            var st = fridge.getItem(s);
                            if (st == null || st.isEmpty()) continue;
                            if (!isGeneVial(st)) continue;
                            String label = labelFromVial(st);
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("VIAL", label, f, s));
                        }
                    }
                }
                // Sequencers outputs
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity> sequencers = new java.util.ArrayList<>();
                var connectedSeq = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity);
                for (var t : connectedSeq) if (t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity g) sequencers.add(g);
                if (!sequencers.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[SAMPLES]", -1, -1));
                    for (int i = 0; i < sequencers.size(); i++) {
                        var seq = sequencers.get(i);
                        var out = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!out.isEmpty()) {
                            String label = "Sequenced Sample " + (i + 1);
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SEQUENCED_SAMPLE", label, i, -1));
                        }
                    }
                }
                // Send to requesting player
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.CatalogEntriesS2CPacket(this.terminalPos, list)
                );
            } else if (this.action != null && this.action.startsWith("slice.start:") && this.targetPos != null) {
                // Client requests slicing specific indices from a connected GeneSlicer
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (target instanceof GeneSlicerBlockEntity slicer) {
                    var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSlicerBlockEntity);
                    boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                    if (!ok) return; // not connected

                    // Parse comma-separated 0-based indices after prefix
                    String payload = this.action.substring("slice.start:".length());
                    java.util.LinkedHashSet<Integer> indices = new java.util.LinkedHashSet<>();
                    if (!payload.isEmpty()) {
                        for (String s : payload.split(",")) {
                            try { indices.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
                        }
                    }
                    if (indices.isEmpty()) return;

                    ItemStack input = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
                    if (input.isEmpty() || input.getItem() != ModItems.SEQUENCED_SAMPLE.get()) return;
                    CompoundTag tag = input.getTag();
                    if (tag == null || !tag.contains("genes", 9)) return;
                    var genes = tag.getList("genes", 10);

                    // Ensure free outputs available
                    int free = 0;
                    for (int i = 0; i < GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT; i++)
                        if (slicer.getItem(GeneSlicerBlockEntity.SLOT_OUTPUT_START + i).isEmpty()) free++;
                    if (free <= 0) return;

                    // Build outputs up to free slots
                    java.util.ArrayList<Integer> toSlice = new java.util.ArrayList<>(indices);
                    if (toSlice.size() > GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT) {
                        toSlice = new java.util.ArrayList<>(toSlice.subList(0, GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT));
                    }
                    if (toSlice.size() > free) {
                        toSlice = new java.util.ArrayList<>(toSlice.subList(0, free));
                    }
                    java.util.ArrayList<ItemStack> outputs = new java.util.ArrayList<>();
                    for (int gi : toSlice) {
                        if (gi < 0 || gi >= genes.size()) continue;
                        CompoundTag g = genes.getCompound(gi);
                        // Minimal vial: encode gene under "gene" subtag. Client will pick item by category later if needed.
                        ItemStack vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_BUILDER.get());
                        CompoundTag vtag = vial.getOrCreateTag();
                        vtag.put("gene", g.copy());
                        if (tag.contains("entity_uuid", 8)) vtag.putString("entity_uuid", tag.getString("entity_uuid"));
                        if (tag.contains("entity_name", 8)) vtag.putString("entity_name", tag.getString("entity_name"));
                        outputs.add(vial);
                    }
                    // Place vials
                    int placed = 0;
                    outer: for (ItemStack vial : outputs) {
                        for (int i = 0; i < GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT; i++) {
                            int slot = GeneSlicerBlockEntity.SLOT_OUTPUT_START + i;
                            if (slicer.getItem(slot).isEmpty()) { slicer.setItem(slot, vial); placed++; break; }
                        }
                    }
                    if (placed > 0) {
                        // Remove genes from input descending
                        java.util.ArrayList<Integer> sorted = new java.util.ArrayList<>(toSlice);
                        sorted.sort(java.util.Comparator.reverseOrder());
                        for (int idx : sorted) { if (idx >= 0 && idx < genes.size()) genes.remove(idx); }
                        slicer.setItem(GeneSlicerBlockEntity.SLOT_INPUT, input);
                        slicer.startProcessing(); // show short progress animation
                    }
                }
            }
        });
        return true;
    }

    private static boolean isGeneVial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        return item instanceof com.github.b4ndithelps.forge.item.GeneVialItem
                || item == ModItems.GENE_VIAL_COSMETIC.get()
                || item == ModItems.GENE_VIAL_RESISTANCE.get()
                || item == ModItems.GENE_VIAL_BUILDER.get()
                || item == ModItems.GENE_VIAL_QUIRK.get();
    }

    private static String labelFromVial(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return "";
        net.minecraft.nbt.CompoundTag tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            net.minecraft.nbt.CompoundTag g = tag.getCompound("gene");
            String name = g.contains("name", 8) ? g.getString("name") : null;
            String id = g.contains("id", 8) ? g.getString("id") : null;
            if (name != null && !name.isEmpty()) return name;
            if (id != null && !id.isEmpty()) return id;
        }
        return vial.getItem().getDescription().getString();
    }
}


