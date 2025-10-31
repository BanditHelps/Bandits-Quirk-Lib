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
                        // Enforce start preconditions: not running, has tissue sample input, and output is empty
                        if (seq.isRunning()) return; // already running
                        ItemStack in = seq.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
                        if (in.isEmpty() || in.getItem() != ModItems.TISSUE_SAMPLE.get()) return; // no valid input
                        ItemStack out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (out != null && !out.isEmpty()) return; // output occupied

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
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[VIALS]", "", -1, false, 0, 0, -1, -1));
                    for (int f = 0; f < fridges.size(); f++) {
                        var fridge = fridges.get(f);
                        for (int s = 0; s < com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                            var st = fridge.getItem(s);
                            if (st == null || st.isEmpty()) continue;
                            if (!isGeneVial(st)) continue;
                            net.minecraft.nbt.CompoundTag tag = st.getTag();
                            String gid = "";
                            int quality = -1;
                            if (tag != null && tag.contains("gene", 10)) {
                                var g = tag.getCompound("gene");
                                gid = g.contains("id", 8) ? g.getString("id") : "";
                                quality = g.contains("quality", 3) ? g.getInt("quality") : -1;
                            }
                            boolean known = false;
                            if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term && gid != null && !gid.isEmpty()) {
                                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                if (rl != null) {
                                    try { known = term.isGeneKnown(rl); } catch (Exception ignored) {}
                                }
                            }
                            String label = labelFromVial(st);
                            int prog = 0, max = 0;
                            if (!known && gid != null && !gid.isEmpty() && player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term2) {
                                for (var t : term2.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                            }
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("VIAL", label, gid, quality, known, prog, max, f, s));
                        }
                    }
                }
                // Sequencers outputs
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity> sequencers = new java.util.ArrayList<>();
                var connectedSeq = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity);
                for (var t : connectedSeq) if (t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity g) sequencers.add(g);
                if (!sequencers.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[SAMPLES]", "", -1, false, 0, 0, -1, -1));
                    for (int i = 0; i < sequencers.size(); i++) {
                        var seq = sequencers.get(i);
                        var out = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!out.isEmpty() && out.getTag() != null && out.getTag().contains("genes", 9)) {
                            var listTag = out.getTag().getList("genes", 10);
                            for (int gi = 0; gi < listTag.size(); gi++) {
                                var g = listTag.getCompound(gi);
                                String gid = g.getString("id");
                                int quality = g.contains("quality", 3) ? g.getInt("quality") : -1;
                                boolean known = false;
                                if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term && gid != null && !gid.isEmpty()) {
                                    net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                    if (rl != null) {
                                        try { known = term.isGeneKnown(rl); } catch (Exception ignored) {}
                                    }
                                }
                                String label = g.contains("name", 8) ? g.getString("name") : (gid == null ? "" : gid);
                                int prog = 0, max = 0;
                                if (!known && gid != null && !gid.isEmpty() && player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term3) {
                                    for (var t : term3.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                                }
                                list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SEQUENCED_GENE", label, gid, quality, known, prog, max, i, gi, seq.getBlockPos()));
                            }
                        }
                    }
                }
                // Send to requesting player
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.CatalogEntriesS2CPacket(this.terminalPos, list)
                );
            } else if (this.action != null && this.action.startsWith("catalog.identify:")) {
                // parse geneId and quality and schedule identification on the ref terminal
                String payload = this.action.substring("catalog.identify:".length());
                String gidStr = payload;
                int quality = 50;
                int lastColon = payload.lastIndexOf(':');
                if (lastColon >= 0) {
                    gidStr = payload.substring(0, lastColon);
                    try { quality = Integer.parseInt(payload.substring(lastColon + 1)); } catch (Exception ignored) {}
                }
                net.minecraft.resources.ResourceLocation gid = null;
                {
                    net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(gidStr);
                    if (rl != null) { gid = rl; }
                }
                if (gid == null) {
                    // Fallback: match by path in registry
                    try {
                        for (com.github.b4ndithelps.genetics.Gene g : com.github.b4ndithelps.genetics.GeneRegistry.all()) {
                            if (g.getId().getPath().equals(gidStr)) { gid = g.getId(); break; }
                        }
                    } catch (Exception ignored) {}
                }
                var beRef = player.level().getBlockEntity(this.terminalPos);
                if (gid != null && beRef instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term) {
                    // Diagnostic details
                    boolean hasDb = term.hasDatabase();
                    boolean wasKnown = false;
                    try { wasKnown = term.isGeneKnown(gid); } catch (Exception ignored) {}
                    int active = 0; for (var t : term.getIdentificationTasks()) if (!t.complete) active++;
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Server] catalog.identify request for " + gid + " q=" + quality + " hasDb=" + hasDb + " known=" + wasKnown + " active=" + active));
                    boolean started = term.startIdentification(gid, quality);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Server] catalog.identify started=" + started));
                    // Immediately sync catalog so client sees queued/progress state
                    if (started) {
                        // Reuse the sync builder to send back to this player only
                        java.util.ArrayList<com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO> list = new java.util.ArrayList<>();
                        // Fridges
                        java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> fridges = new java.util.ArrayList<>();
                        var connectedFridges = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                        for (var t : connectedFridges) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) fridges.add(f);
                        if (!fridges.isEmpty()) {
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[VIALS]", "", -1, false, 0, 0, -1, -1));
                            for (int f = 0; f < fridges.size(); f++) {
                                var fridge = fridges.get(f);
                                for (int s = 0; s < com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                                    var st = fridge.getItem(s);
                                    if (st == null || st.isEmpty()) continue;
                                    if (!isGeneVial(st)) continue;
                                    net.minecraft.nbt.CompoundTag tag = st.getTag();
                                    String egid = ""; int eq = -1;
                                    if (tag != null && tag.contains("gene", 10)) {
                                        var g = tag.getCompound("gene");
                                        egid = g.contains("id", 8) ? g.getString("id") : "";
                                        eq = g.contains("quality", 3) ? g.getInt("quality") : -1;
                                    }
                                    boolean isKnown = false; int prog = 0, max = 0;
                                    if (egid != null && !egid.isEmpty()) {
                                        net.minecraft.resources.ResourceLocation rlEg = net.minecraft.resources.ResourceLocation.tryParse(egid);
                                        if (rlEg != null) { try { isKnown = term.isGeneKnown(rlEg); } catch (Exception ignored) {} }
                                        for (var t : term.getIdentificationTasks()) if (!t.complete && samePath(egid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                                    }
                                    String label = labelFromVial(st);
                                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("VIAL", label, egid, eq, isKnown, prog, max, f, s));
                                }
                            }
                        }
                        // Sequencers
                        java.util.ArrayList<com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity> sequencers = new java.util.ArrayList<>();
                        var connectedSeq = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity);
                        for (var t : connectedSeq) if (t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity g2) sequencers.add(g2);
                        if (!sequencers.isEmpty()) {
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[SAMPLES]", "", -1, false, 0, 0, -1, -1));
                            for (int i = 0; i < sequencers.size(); i++) {
                                var seq = sequencers.get(i);
                                var out = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                                if (!out.isEmpty() && out.getTag() != null && out.getTag().contains("genes", 9)) {
                                    var listTag = out.getTag().getList("genes", 10);
                                    for (int gi = 0; gi < listTag.size(); gi++) {
                                        var g = listTag.getCompound(gi);
                                        String egid = g.getString("id");
                                        int eq = g.contains("quality", 3) ? g.getInt("quality") : -1;
                                        boolean isKnown = false; int prog = 0, max = 0;
                                        if (egid != null && !egid.isEmpty()) {
                                            net.minecraft.resources.ResourceLocation rlEg = net.minecraft.resources.ResourceLocation.tryParse(egid);
                                            if (rlEg != null) { try { isKnown = term.isGeneKnown(rlEg); } catch (Exception ignored) {} }
                                            for (var t : term.getIdentificationTasks()) if (!t.complete && samePath(egid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                                        }
                                        String label = g.contains("name", 8) ? g.getString("name") : (egid == null ? "" : egid);
                                        list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SEQUENCED_GENE", label, egid, eq, isKnown, prog, max, i, gi, seq.getBlockPos()));
                                    }
                                }
                            }
                        }
                        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new com.github.b4ndithelps.forge.network.CatalogEntriesS2CPacket(this.terminalPos, list)
                        );
                    }
                }
            } else if (this.action != null && this.action.startsWith("slice.start:") && this.targetPos != null) {
                // Client requests slicing specific indices from a connected GeneSlicer
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (target instanceof GeneSlicerBlockEntity slicer) {
                    var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSlicerBlockEntity);
                    boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                    if (!ok) return; // not connected

                    // Do not allow starting while running
                    if (slicer.isRunning()) return;

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

                    // Determine current free slots to limit immediate planned outputs
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
                    if (outputs.isEmpty()) return;
                    // Remove genes from input descending immediately so they disappear from UI
                    java.util.ArrayList<Integer> sorted = new java.util.ArrayList<>(toSlice);
                    sorted.sort(java.util.Comparator.reverseOrder());
                    for (int idx : sorted) { if (idx >= 0 && idx < genes.size()) genes.remove(idx); }
                    // If no genes remain, delete the sequenced sample from input
                    if (genes.isEmpty()) {
                        input = ItemStack.EMPTY;
                    }
                    slicer.setItem(GeneSlicerBlockEntity.SLOT_INPUT, input);
                    // Queue outputs to be placed when processing completes and start running
                    slicer.enqueueOutputs(outputs);
                    // Immediately notify the requesting player of new labels and running state
                    java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                    if (!input.isEmpty() && input.getTag() != null && input.getTag().contains("genes", 9)) {
                        var newList = input.getTag().getList("genes", 10);
                        for (int i = 0; i < newList.size(); i++) {
                            CompoundTag g = newList.getCompound(i);
                            String label = g.getString("name");
                            if (label == null || label.isEmpty()) label = g.getString("id");
                            if (label == null || label.isEmpty()) label = "gene_" + Integer.toString(i + 1);
                            labels.add(label);
                        }
                    }
                    com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new com.github.b4ndithelps.forge.network.SlicerStateS2CPacket(this.targetPos, true, labels)
                    );
                }
            } else if ("slice.sync".equals(this.action)) {
                // Send current running state and input labels for connected slicers to the requesting player
                var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSlicerBlockEntity);
                for (var t : connected) {
                    if (t instanceof GeneSlicerBlockEntity s) {
                        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
                        ItemStack in = s.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
                        if (!in.isEmpty()) {
                            CompoundTag tag = in.getTag();
                            if (tag != null && tag.contains("genes", 9)) {
                                var list = tag.getList("genes", 10);
                                for (int i = 0; i < list.size(); i++) {
                                    CompoundTag g = list.getCompound(i);
                                    String label = g.getString("name");
                                    if (label == null || label.isEmpty()) label = g.getString("id");
                                    if (label == null || label.isEmpty()) label = "gene_" + Integer.toString(i + 1);
                                    labels.add(label);
                                }
                            }
                        }
                        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new com.github.b4ndithelps.forge.network.SlicerStateS2CPacket(s.getBlockPos(), s.isRunning(), labels)
                        );
                    }
                }
            } else if (this.action != null && this.action.startsWith("comb.start:") && this.targetPos != null) {
                // Client requests combining selected vials from connected refrigerators into a target GeneCombiner
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (!(target instanceof com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity comb)) return;

                // Validate combiner is connected to terminal
                var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity);
                boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                if (!ok) return;

                // Output must be empty before starting
                if (!comb.getItem(com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_OUTPUT).isEmpty()) return;

                // Determine current free inputs
                int free = 0;
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++)
                    if (comb.getItem(i).isEmpty()) free++;
                if (free <= 0) return;

                // Build fridge list (same ordering used by catalog.sync so indices align)
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> fridges = new java.util.ArrayList<>();
                var connectedFridges = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                for (var t : connectedFridges) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) fridges.add(f);

                // Parse selections: "f-s,f-s,..."
                String payload = this.action.substring("comb.start:".length());
                java.util.LinkedHashSet<int[]> picks = new java.util.LinkedHashSet<>();
                if (!payload.isEmpty()) {
                    for (String token : payload.split(",")) {
                        int dash = token.indexOf('-');
                        if (dash <= 0) continue;
                        try {
                            int fi = Integer.parseInt(token.substring(0, dash).trim());
                            int si = Integer.parseInt(token.substring(dash + 1).trim());
                            picks.add(new int[]{fi, si});
                        } catch (Exception ignored) {}
                    }
                }
                if (picks.isEmpty()) return;

                // Move up to free inputs
                for (int[] ref : picks) {
                    if (free <= 0) break;
                    int fi = ref[0], si = ref[1];
                    if (fi < 0 || fi >= fridges.size()) continue;
                    var fridge = fridges.get(fi);
                    if (si < 0 || si >= com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT) continue;
                    ItemStack st = fridge.getItem(si);
                    if (st.isEmpty() || !isGeneVial(st)) continue;
                    // find target input slot
                    int tgt = -1;
                    for (int i = 0; i < com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++)
                        if (comb.getItem(i).isEmpty()) { tgt = i; break; }
                    if (tgt < 0) break;
                    fridge.setItem(si, ItemStack.EMPTY);
                    comb.setItem(tgt, st);
                    free--;
                }

                // Gather provided ingredients from current inputs
                java.util.ArrayList<com.github.b4ndithelps.genetics.GeneCombinationService.GeneIngredient> provided = new java.util.ArrayList<>();
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
                    ItemStack st = comb.getItem(i);
                    if (st.isEmpty()) continue;
                    CompoundTag tag = st.getTag();
                    if (tag != null && tag.contains("gene", 10)) {
                        CompoundTag g = tag.getCompound("gene");
                        String id = g.getString("id");
                        int q = g.contains("quality", 3) ? g.getInt("quality") : 0;
                        try {
                            provided.add(new com.github.b4ndithelps.genetics.GeneCombinationService.GeneIngredient(new net.minecraft.resources.ResourceLocation(id), q));
                        } catch (Exception ignored) {}
                    }
                }
                if (provided.isEmpty()) return;

                // Determine resulting gene by matching recipe
                com.github.b4ndithelps.genetics.Gene result = null;
                try {
                    for (com.github.b4ndithelps.genetics.Gene g : com.github.b4ndithelps.genetics.GeneRegistry.all()) {
                        if (!g.isCombinable() || g.getCombinationRecipe() == null) continue;
                        boolean matches = false;
                        try {
                            var server = player.getServer();
                            matches = com.github.b4ndithelps.genetics.GeneCombinationService.matchesRecipe(server, g, provided);
                        } catch (Exception ignored) {}
                        if (matches) { result = g; break; }
                    }
                } catch (Exception ignored) {}

                // Consume inputs
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) comb.setItem(i, ItemStack.EMPTY);

                ItemStack output;
                if (result != null) {
                    ItemStack vial;
                    switch (result.getCategory()) {
                        case cosmetic -> vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_COSMETIC.get());
                        case resistance -> vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_RESISTANCE.get());
                        case builder -> vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_BUILDER.get());
                        case quirk -> vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_QUIRK.get());
                        default -> vial = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.GENE_VIAL_BUILDER.get());
                    }
                    CompoundTag gene = new CompoundTag();
                    gene.putString("id", result.getId().toString());
                    int q = 0; for (var ing : provided) q = Math.max(q, ing.quality);
                    gene.putInt("quality", Math.max(result.getQualityMin(), Math.min(result.getQualityMax(), q)));
                    gene.putString("name", compactLabelFromId(result.getId().toString(), q));
                    CompoundTag vtag = vial.getOrCreateTag();
                    vtag.put("gene", gene);
                    output = vial;
                } else {
                    output = new ItemStack(com.github.b4ndithelps.forge.item.ModItems.FAILED_SAMPLE.get());
                }

                // Place in output or first free input slot
                if (comb.getItem(com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_OUTPUT).isEmpty()) {
                    comb.setItem(com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_OUTPUT, output);
                } else {
                    for (int i = 0; i < com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
                        if (comb.getItem(i).isEmpty()) { comb.setItem(i, output); break; }
                    }
                }

                // Send feedback to the requesting player
                boolean success = (result != null);
                String message = success ? "Combination complete" : "Combination failed";
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.CombinerStateS2CPacket(this.targetPos, success, message)
                );

                // Immediately send updated catalog entries so client vial list refreshes
                java.util.ArrayList<com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO> list = new java.util.ArrayList<>();
                // Fridges section
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> frList = new java.util.ArrayList<>();
                var connFr = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                for (var t : connFr) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) frList.add(f);
                if (!frList.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[VIALS]", "", -1, false, 0, 0, -1, -1));
                    for (int f = 0; f < frList.size(); f++) {
                        var fridge = frList.get(f);
                        for (int s = 0; s < com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                            var st = fridge.getItem(s);
                            if (st == null || st.isEmpty()) continue;
                            if (!isGeneVial(st)) continue;
                            net.minecraft.nbt.CompoundTag tag = st.getTag();
                            String gid = ""; int quality = -1;
                            if (tag != null && tag.contains("gene", 10)) {
                                var g = tag.getCompound("gene");
                                gid = g.contains("id", 8) ? g.getString("id") : "";
                                quality = g.contains("quality", 3) ? g.getInt("quality") : -1;
                            }
                            boolean known = false;
                            if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term && gid != null && !gid.isEmpty()) {
                                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                if (rl != null) { try { known = term.isGeneKnown(rl); } catch (Exception ignored) {} }
                            }
                            String label = labelFromVial(st);
                            int prog = 0, max = 0;
                            if (!known && gid != null && !gid.isEmpty() && player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term2) {
                                for (var t : term2.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                            }
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("VIAL", label, gid, quality, known, prog, max, f, s));
                        }
                    }
                }
                // Sequencers section
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity> seql = new java.util.ArrayList<>();
                var connSeq = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity);
                for (var t : connSeq) if (t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity g2) seql.add(g2);
                if (!seql.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[SAMPLES]", "", -1, false, 0, 0, -1, -1));
                    for (int i = 0; i < seql.size(); i++) {
                        var seq = seql.get(i);
                        var out = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!out.isEmpty() && out.getTag() != null && out.getTag().contains("genes", 9)) {
                            var listTag = out.getTag().getList("genes", 10);
                            for (int gi = 0; gi < listTag.size(); gi++) {
                                var g = listTag.getCompound(gi);
                                String gid = g.getString("id");
                                int q = g.contains("quality", 3) ? g.getInt("quality") : -1;
                                boolean known = false; int prog = 0, max = 0;
                                if (gid != null && !gid.isEmpty()) {
                                    net.minecraft.resources.ResourceLocation rlEg = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                    if (rlEg != null) {
                                        if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term) {
                                            try { known = term.isGeneKnown(rlEg); } catch (Exception ignored) {}
                                            for (var t : term.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                                        }
                                    }
                                }
                                String label = g.contains("name", 8) ? g.getString("name") : (gid == null ? "" : gid);
                                list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SEQUENCED_GENE", label, gid, q, known, prog, max, i, gi, seq.getBlockPos()));
                            }
                        }
                    }
                }
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.CatalogEntriesS2CPacket(this.terminalPos, list)
                );
            } else if (this.action != null && this.action.startsWith("print.start:") && this.targetPos != null) {
                // Client requests printing selected vials into a connected BioPrinter
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (!(target instanceof com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity printer)) return;

                // Validate printer is connected to terminal
                var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity);
                boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                if (!ok) return;

                // Output must be empty
                if (!printer.getItem(com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_OUTPUT).isEmpty()) return;

                // Count free inputs
                int free = 0;
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++)
                    if (printer.getItem(i).isEmpty()) free++;
                if (free <= 0) return;

                // Build fridge list in terminal network order
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> fridges = new java.util.ArrayList<>();
                var connectedFridges = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                for (var t : connectedFridges) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) fridges.add(f);

                // Parse selections: f-s,f-s
                String payload = this.action.substring("print.start:".length());
                java.util.LinkedHashSet<int[]> picks = new java.util.LinkedHashSet<>();
                if (!payload.isEmpty()) {
                    for (String token : payload.split(",")) {
                        int dash = token.indexOf('-');
                        if (dash <= 0) continue;
                        try {
                            int fi = Integer.parseInt(token.substring(0, dash).trim());
                            int si = Integer.parseInt(token.substring(dash + 1).trim());
                            picks.add(new int[]{fi, si});
                        } catch (Exception ignored) {}
                    }
                }
                if (picks.isEmpty()) return;

                // Move selected vials into printer input slots (up to free)
                int placed = 0;
                for (int[] ref : picks) {
                    if (placed >= free) break;
                    int fi = ref[0], si = ref[1];
                    if (fi < 0 || fi >= fridges.size()) continue;
                    var fridge = fridges.get(fi);
                    if (si < 0 || si >= com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT) continue;
                    ItemStack st = fridge.getItem(si);
                    if (st.isEmpty() || !isGeneVial(st)) continue;
                    int tgt = -1;
                    for (int i = 0; i < com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++)
                        if (printer.getItem(i).isEmpty()) { tgt = i; break; }
                    if (tgt < 0) break;
                    fridge.setItem(si, ItemStack.EMPTY);
                    printer.setItem(tgt, st);
                    placed++;
                }
                if (placed <= 0) return;

                // Build genome from current inputs
                net.minecraft.nbt.ListTag genome = new net.minecraft.nbt.ListTag();
                String entityName = null;
                String entityUuid = null;
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) {
                    ItemStack st = printer.getItem(i);
                    if (st.isEmpty()) continue;
                    net.minecraft.nbt.CompoundTag tag = st.getTag();
                    if (tag != null && tag.contains("gene", 10)) {
                        net.minecraft.nbt.CompoundTag g = tag.getCompound("gene");
                        genome.add(g.copy());
                    }
                    if (entityName == null && tag != null && tag.contains("entity_name", 8)) entityName = tag.getString("entity_name");
                    if (entityUuid == null && tag != null && tag.contains("entity_uuid", 8)) entityUuid = tag.getString("entity_uuid");
                }
                // Clear inputs
                for (int i = 0; i < com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) printer.setItem(i, ItemStack.EMPTY);

                // Create injector
                ItemStack injector = new ItemStack(ModItems.INJECTOR.get());
                net.minecraft.nbt.CompoundTag itag = injector.getOrCreateTag();
                itag.put("genome", genome);
                if (entityName != null) itag.putString("entity_name", entityName);
                if (entityUuid != null) itag.putString("entity_uuid", entityUuid);

                // Place output
                if (printer.getItem(com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_OUTPUT).isEmpty()) {
                    printer.setItem(com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_OUTPUT, injector);
                } else {
                    for (int i = 0; i < com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) {
                        if (printer.getItem(i).isEmpty()) { printer.setItem(i, injector); break; }
                    }
                }

                // Send feedback to player
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.PrinterStateS2CPacket(this.targetPos, true, "Printing complete!")
                );

                // Send updated catalog entries to refresh vials list
                java.util.ArrayList<com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO> list = new java.util.ArrayList<>();
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity> frList = new java.util.ArrayList<>();
                var connFr = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity);
                for (var t : connFr) if (t instanceof com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity f) frList.add(f);
                if (!frList.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[VIALS]", "", -1, false, 0, 0, -1, -1));
                    for (int f = 0; f < frList.size(); f++) {
                        var fridge = frList.get(f);
                        for (int s = 0; s < com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                            var st = fridge.getItem(s);
                            if (st == null || st.isEmpty()) continue;
                            if (!isGeneVial(st)) continue;
                            net.minecraft.nbt.CompoundTag tag = st.getTag();
                            String gid = ""; int quality = -1;
                            if (tag != null && tag.contains("gene", 10)) {
                                var g = tag.getCompound("gene");
                                gid = g.contains("id", 8) ? g.getString("id") : "";
                                quality = g.contains("quality", 3) ? g.getInt("quality") : -1;
                            }
                            boolean known = false;
                            if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term && gid != null && !gid.isEmpty()) {
                                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                if (rl != null) { try { known = term.isGeneKnown(rl); } catch (Exception ignored) {} }
                            }
                            String label = labelFromVial(st);
                            int prog = 0, max = 0;
                            if (!known && gid != null && !gid.isEmpty() && player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term2) {
                                for (var t : term2.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                            }
                            list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("VIAL", label, gid, quality, known, prog, max, f, s));
                        }
                    }
                }
                java.util.ArrayList<com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity> seql = new java.util.ArrayList<>();
                var connSeq = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity);
                for (var t : connSeq) if (t instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity g2) seql.add(g2);
                if (!seql.isEmpty()) {
                    list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SECTION", "[SAMPLES]", "", -1, false, 0, 0, -1, -1));
                    for (int i = 0; i < seql.size(); i++) {
                        var seq = seql.get(i);
                        var out = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!out.isEmpty() && out.getTag() != null && out.getTag().contains("genes", 9)) {
                            var listTag = out.getTag().getList("genes", 10);
                            for (int gi = 0; gi < listTag.size(); gi++) {
                                var g = listTag.getCompound(gi);
                                String gid = g.getString("id");
                                int q = g.contains("quality", 3) ? g.getInt("quality") : -1;
                                boolean known = false; int prog = 0, max = 0;
                                if (gid != null && !gid.isEmpty()) {
                                    net.minecraft.resources.ResourceLocation rlEg = net.minecraft.resources.ResourceLocation.tryParse(gid);
                                    if (rlEg != null) {
                                        if (player.level().getBlockEntity(this.terminalPos) instanceof com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity term) {
                                            try { known = term.isGeneKnown(rlEg); } catch (Exception ignored) {}
                                            for (var t : term.getIdentificationTasks()) if (!t.complete && samePath(gid, t.geneId)) { prog = t.progress; max = Math.max(1, t.max); break; }
                                        }
                                    }
                                }
                                String label = g.contains("name", 8) ? g.getString("name") : (gid == null ? "" : gid);
                                list.add(new com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.EntryDTO("SEQUENCED_GENE", label, gid, q, known, prog, max, i, gi, seq.getBlockPos()));
                            }
                        }
                    }
                }
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.github.b4ndithelps.forge.network.CatalogEntriesS2CPacket(this.terminalPos, list)
                );
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

    private static boolean samePath(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        net.minecraft.resources.ResourceLocation ra = net.minecraft.resources.ResourceLocation.tryParse(a);
        net.minecraft.resources.ResourceLocation rb = net.minecraft.resources.ResourceLocation.tryParse(b);
        if (ra != null && rb != null) {
            return ra.getPath().equals(rb.getPath());
        }
        int ia = a.indexOf(':'); int ib = b.indexOf(':');
        String pa = ia >= 0 ? a.substring(ia + 1) : a;
        String pb = ib >= 0 ? b.substring(ib + 1) : b;
        return pa.equals(pb);
    }

    private static String compactLabelFromId(String id, int quality) {
        return "gene_" + String.format("%04x", Math.abs((id + "_" + quality).hashCode()) & 0xFFFF);
    }
}


