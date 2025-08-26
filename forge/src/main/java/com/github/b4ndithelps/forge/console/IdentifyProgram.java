package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSlicerBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Identify program: queue research tasks for genes from current sample (in sequencer/slicer)
 * or from gene vials in adjacent sample refrigerators. Shows progress of up to 3 concurrent tasks.
 */
@SuppressWarnings("removal")
public class IdentifyProgram extends AbstractConsoleProgram {
    private enum View { LIST }
    private View view = View.LIST;

    private static final class Candidate {
        final String source; // e.g., "SEQ", "SLICER", or "FRIDGE x,y"
        final String geneId;
        final int quality;
        final String crypticName;
        Candidate(String source, String geneId, int quality, String crypticName) {
            this.source = source; this.geneId = geneId; this.quality = quality; this.crypticName = crypticName;
        }
    }

    private final List<Candidate> candidates = new ArrayList<>();

    @Override
    public String getName() { return "identify"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        autoQueueIfPossible(ctx);
        render(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        candidates.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();

        // Collect from adjacent sequencer output
        for (var dir : Direction.values()) {
            var be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof GeneSequencerBlockEntity seq) {
                ItemStack out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                if (!out.isEmpty() && out.getItem() == ModItems.SEQUENCED_SAMPLE.get()) {
                    collectFromSample("SEQ", out.getTag());
                }
            }
            if (be instanceof GeneSlicerBlockEntity slicer) {
                ItemStack in = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
                if (!in.isEmpty() && in.getItem() == ModItems.SEQUENCED_SAMPLE.get()) {
                    collectFromSample("SLICER", in.getTag());
                }
            }
            if (be instanceof SampleRefrigeratorBlockEntity fridge) {
                for (int i = 0; i < SampleRefrigeratorBlockEntity.SLOT_COUNT; i++) {
                    ItemStack st = fridge.getItem(i);
                    if (st == null || st.isEmpty()) continue;
                    if (!isGeneVial(st)) continue;
                    CompoundTag tag = st.getTag();
                    if (tag != null && tag.contains("gene", 10)) {
                        CompoundTag g = tag.getCompound("gene");
                        String id = g.getString("id");
                        int q = g.getInt("quality");
                        String name = g.contains("name", 8) ? g.getString("name") : compactLabelFromId(id, q);
                        candidates.add(new Candidate("FRIDGE", id, q, name));
                    }
                }
            }
        }

        // Remove duplicates by geneId; prefer higher quality display
        java.util.Map<String, Candidate> byId = new java.util.HashMap<>();
        for (Candidate c : candidates) {
            var existing = byId.get(c.geneId);
            if (existing == null || c.quality > existing.quality) byId.put(c.geneId, c);
        }
        candidates.clear();
        candidates.addAll(byId.values());
        candidates.sort(java.util.Comparator.comparing((Candidate c) -> c.geneId));
    }

    private void collectFromSample(String source, CompoundTag tag) {
        if (tag == null || !tag.contains("genes", 9)) return;
        var list = tag.getList("genes", 10);
        for (int i = 0; i < list.size(); i++) {
            var g = list.getCompound(i);
            String id = g.getString("id");
            int q = g.getInt("quality");
            String name = g.contains("name", 8) ? g.getString("name") : compactLabelFromId(id, q);
            candidates.add(new Candidate(source, id, q, name));
        }
    }

    private boolean isGeneVial(ItemStack stack) {
        var it = stack.getItem();
        return it instanceof GeneVialItem
                || it == ModItems.GENE_VIAL_COSMETIC.get()
                || it == ModItems.GENE_VIAL_RESISTANCE.get()
                || it == ModItems.GENE_VIAL_BUILDER.get()
                || it == ModItems.GENE_VIAL_QUIRK.get();
    }

    @Override
    public void onTick(ConsoleContext ctx) {
        // Keep candidates fresh and auto-queue up to capacity
        refresh(ctx);
        autoQueueIfPossible(ctx);
        render(ctx);
    }

    private void render(ConsoleContext ctx) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        ProgramScreenBuilder b = screen()
                .header("Identify Genes")
                .line("Commands: list | start <id/index...> | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        if (!be.hasDatabase()) {
            b.line("Insert a Gene Database into the terminal slot.", ConsoleText.ColorTag.RED);
        }

        // Show current tasks
        var tasks = be.getIdentificationTasks();
        int active = 0;
        for (var t : tasks) if (!t.complete) active++;
        if (!tasks.isEmpty()) {
            b.line("Active Research:", ConsoleText.ColorTag.AQUA);
            for (var t : tasks) {
                int pct = t.max == 0 ? 0 : (t.progress * 100 / t.max);
                String line = String.format(" - %s  %d%% %s", t.geneId, pct, t.complete ? "(Done)" : "");
                b.line(line, t.complete ? ConsoleText.ColorTag.GREEN : ConsoleText.ColorTag.GRAY);
            }
            b.blank();
        }

        // List candidates that are not known
        b.line("Candidates:", ConsoleText.ColorTag.WHITE);
        if (candidates.isEmpty()) {
            b.line("<none detected>", ConsoleText.ColorTag.GRAY);
        } else {
            int i = 1;
            for (Candidate c : candidates) {
                boolean known = false;
                try { known = be.isGeneKnown(new ResourceLocation(c.geneId)); } catch (Exception ignored) {}
                String label = i + ") " + c.crypticName + "  [" + c.geneId + "]" + (known ? " (Known)" : "");
                b.line(label, known ? ConsoleText.ColorTag.GREEN : ConsoleText.ColorTag.WHITE);
                i++;
            }
        }

        render(ctx, b.build());
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        if ("list".equalsIgnoreCase(name)) {
            refresh(ctx);
            render(ctx);
            return true;
        }
        if ("start".equalsIgnoreCase(name)) {
            if (args.isEmpty()) { setStatusWarn("Usage: start <gene_id/index...>"); render(ctx); return true; }
            int started = 0;
            for (String idStr : args) {
                ResourceLocation id = null;
                // allow numeric index
                try {
                    int idx = Integer.parseInt(idStr) - 1;
                    if (idx >= 0 && idx < candidates.size()) {
                        id = new ResourceLocation(candidates.get(idx).geneId);
                    }
                } catch (Exception ignored) {}
                if (id == null) {
                    try { id = new ResourceLocation(idStr); } catch (Exception e) { setStatusErr("Bad id: " + idStr); continue; }
                }
                // Find best quality candidate for this id
                int q = 50;
                for (Candidate c : candidates) if (samePath(c.geneId, id)) q = Math.max(q, c.quality);
                if (be.startIdentification(id, q)) started++;
            }
            setStatusOk("Queued " + started + " identification(s). Max 3 run at once.");
            render(ctx);
            return true;
        }
        if ("exit".equalsIgnoreCase(name)) { ctx.exitProgram(); return true; }
        return false;
    }

    private String compactLabelFromId(String id, int quality) {
        return "gene_" + String.format("%04x", Math.abs((id + "_" + quality).hashCode()) & 0xFFFF);
    }

    private boolean samePath(String a, ResourceLocation b) {
        try { return new ResourceLocation(a).getPath().equals(b.getPath()); } catch (Exception e) { return a != null && a.endsWith(b.getPath()); }
    }

    private void autoQueueIfPossible(ConsoleContext ctx) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        if (!be.hasDatabase()) return;
        int active = 0;
        for (var t : be.getIdentificationTasks()) if (!t.complete) active++;
        if (active >= 3) return;
        for (Candidate c : candidates) {
            boolean known = false;
            try { known = be.isGeneKnown(new ResourceLocation(c.geneId)); } catch (Exception ignored) {}
            if (known) continue;
            if (be.canStartIdentification(new ResourceLocation(c.geneId))) {
                be.startIdentification(new ResourceLocation(c.geneId), c.quality);
                active++;
                if (active >= 3) break;
            }
        }
    }
}


