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
    private enum View { LIST, TASKS, DETAILS }
    private View view = View.LIST;
    private boolean showHelp = false;
    private int page = 0;
    private static final int GRID_COLUMNS = 2;
    private static final int GRID_ROWS = 6; // up to 18 candidates per screen
    private int selectedIndex = -1; // for DETAILS view

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
        // Keep candidates fresh and update current view
        refresh(ctx);
        render(ctx);
    }

    private void render(ConsoleContext ctx) {
        if (view == View.TASKS) {
            renderTasks(ctx);
        } else if (view == View.DETAILS) {
            renderDetails(ctx);
        } else {
            renderList(ctx);
        }
    }

    private void renderList(ConsoleContext ctx) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        ProgramScreenBuilder b = screen()
                .header("Identify Genes");
        if (showHelp) {
            b.line("Commands: list | start <id/index...> | tasks | info <idx> | page <n> | next | prev | help | exit", ConsoleText.ColorTag.GRAY);
        } else {
            b.line("Type 'help' for commands.", ConsoleText.ColorTag.GRAY);
        }

        if (!be.hasDatabase()) {
            b.line("Insert a Gene Database into the terminal slot.", ConsoleText.ColorTag.RED);
        }

        // Compact summary of current tasks (no verbose progress here)
        var tasks = be.getIdentificationTasks();
        if (!tasks.isEmpty()) {
            int active = 0, done = 0;
            for (var t : tasks) { if (t.complete) done++; else active++; }
            b.line(String.format("Research: %d active%s  (type 'tasks' for details)", active, done > 0 ? ", " + done + " done" : ""), ConsoleText.ColorTag.AQUA)
             .blank();
        }

        // Candidates grid (compact) - only show unknown genes
        List<Candidate> unknown = getUnknownCandidates(be);
        if (unknown.isEmpty()) {
            b.line("<none detected>", ConsoleText.ColorTag.GRAY);
        } else {
            int start = page * (GRID_COLUMNS * GRID_ROWS);
            int end = Math.min(unknown.size(), start + GRID_COLUMNS * GRID_ROWS);
            int totalPages = Math.max(1, (unknown.size() + (GRID_COLUMNS * GRID_ROWS) - 1) / (GRID_COLUMNS * GRID_ROWS));
            page = Math.max(0, Math.min(page, totalPages - 1));

            // Build grid rows
            for (int r = 0; r < GRID_ROWS; r++) {
                int base = start + r * GRID_COLUMNS;
                if (base >= end) break;
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < GRID_COLUMNS; c++) {
                    int idx = base + c;
                    if (idx >= end) break;
                    Candidate cand = unknown.get(idx);
                    String cell = String.format("%2d) %s", idx + 1, trimCell(cand.crypticName, 14));
                    row.append(padRight(cell, 26));
                }
                b.line(row.toString(), ConsoleText.ColorTag.WHITE);
            }
            // Footer with pagination
            if (totalPages > 1) {
                b.blank();
                b.centerLine("Page " + (page + 1) + "/" + totalPages + "  (next/prev/page <n>)");
            }
        }

        render(ctx, b.build());
    }

    private void renderTasks(ConsoleContext ctx) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        ProgramScreenBuilder b = screen()
                .header("Research Tasks")
                .line("Commands: back | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        var tasks = be.getIdentificationTasks();
        if (tasks.isEmpty()) {
            b.line("<no research tasks>", ConsoleText.ColorTag.GRAY);
        } else {
            for (var t : tasks) {
                int pct = t.max == 0 ? 0 : (t.progress * 100 / t.max);
                String resolved = "unknown";
                try { if (be.isGeneKnown(new ResourceLocation(t.geneId))) resolved = net.minecraft.network.chat.Component.translatable(t.geneId).getString(); } catch (Exception ignored) {}
                String status = (t.complete ? "Done" : pct + "%");
                b.twoColumn(resolved, status, 24);
            }
        }
        render(ctx, b.build());
    }

    private void renderDetails(ConsoleContext ctx) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        ProgramScreenBuilder b = screen()
                .header("Gene Details")
                .line("Commands: back | start <idx|id> | exit", ConsoleText.ColorTag.GRAY)
                .separator();
        List<Candidate> unknown = getUnknownCandidates(be);
        if (selectedIndex < 0 || selectedIndex >= unknown.size()) {
            b.line("<no selection>", ConsoleText.ColorTag.GRAY);
        } else {
            Candidate c = unknown.get(selectedIndex);
            boolean known = false;
            try { known = be.isGeneKnown(new ResourceLocation(c.geneId)); } catch (Exception ignored) {}
            String translated = known ? net.minecraft.network.chat.Component.translatable(c.geneId).getString() : "unknown";
            b.twoColumn("Index", String.valueOf(selectedIndex + 1), 12)
             .twoColumn("Label", c.crypticName, 12)
             .twoColumn("Translated", translated, 12)
             .twoColumn("Quality", c.quality + "%", 12);
        }
        render(ctx, b.build());
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        BioTerminalBlockEntity be = ctx.getBlockEntity();
        if ("list".equalsIgnoreCase(name)) {
            view = View.LIST;
            refresh(ctx);
            render(ctx);
            return true;
        }
        if ("tasks".equalsIgnoreCase(name)) {
            view = View.TASKS;
            render(ctx);
            return true;
        }
        if ("help".equalsIgnoreCase(name)) {
            showHelp = !showHelp;
            render(ctx);
            return true;
        }
        if ("info".equalsIgnoreCase(name)) {
            if (args.isEmpty()) { setStatusWarn("Usage: info <index>"); render(ctx); return true; }
            try {
                int idx = Integer.parseInt(args.get(0)) - 1;
                List<Candidate> unknown = getUnknownCandidates(be);
                if (idx < 0 || idx >= unknown.size()) { setStatusErr("Invalid index"); render(ctx); return true; }
                selectedIndex = idx;
                view = View.DETAILS;
                render(ctx);
            } catch (Exception e) {
                setStatusErr("Invalid index"); render(ctx);
            }
            return true;
        }
        if ("page".equalsIgnoreCase(name)) {
            if (args.isEmpty()) { setStatusWarn("Usage: page <n>"); render(ctx); return true; }
            try { page = Math.max(0, Integer.parseInt(args.get(0)) - 1); } catch (Exception ignored) { setStatusErr("Invalid page"); }
            render(ctx);
            return true;
        }
        if ("next".equalsIgnoreCase(name)) { page++; render(ctx); return true; }
        if ("prev".equalsIgnoreCase(name)) { page = Math.max(0, page - 1); render(ctx); return true; }
        if ("back".equalsIgnoreCase(name)) {
            if (view == View.TASKS || view == View.DETAILS) {
                view = View.LIST;
                render(ctx);
                return true;
            }
            return false;
        }
        if ("start".equalsIgnoreCase(name)) {
            if (args.isEmpty()) { setStatusWarn("Usage: start <gene_id/index...>"); render(ctx); return true; }
            int started = 0;
            for (String idStr : args) {
                ResourceLocation id = null;
                // allow numeric index
                try {
                    int idx = Integer.parseInt(idStr) - 1;
                    List<Candidate> unknown = getUnknownCandidates(be);
                    if (idx >= 0 && idx < unknown.size()) {
                        id = new ResourceLocation(unknown.get(idx).geneId);
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

    private List<Candidate> getUnknownCandidates(BioTerminalBlockEntity be) {
        List<Candidate> list = new ArrayList<>();
        for (Candidate c : candidates) {
            boolean known = false;
            try { known = be.isGeneKnown(new ResourceLocation(c.geneId)); } catch (Exception ignored) {}
            if (!known) list.add(c);
        }
        return list;
    }

    private String trimCell(String s, int width) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[^A-Za-z0-9_()]+", "");
        if (cleaned.length() <= width) return cleaned;
        return cleaned.substring(0, width);
    }

    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder b = new StringBuilder(s);
        while (b.length() < width) b.append(' ');
        return b.toString();
    }
}


