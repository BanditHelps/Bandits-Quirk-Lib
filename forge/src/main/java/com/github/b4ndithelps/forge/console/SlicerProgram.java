package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.GeneSlicerBlockEntity;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.*;

/**
 * Slicer program: interacts with adjacent GeneSlicer machines to separate genes
 * from a sequenced sample into individual gene vials.
 *
 * Commands (LIST view):
 *  - list
 *  - open <n>
 *  - exit
 *
 * Commands (DETAILS view):
 *  - slice <a> [b] [c] [d] [e] [f]  (indexes 1-based; up to 6 unique)
 *  - back | exit
 */
public class SlicerProgram extends AbstractConsoleProgram {
    private final List<GeneSlicerBlockEntity> cachedSlicers = new ArrayList<>();
    private enum View { LIST, DETAILS }
    private View view = View.LIST;
    private int openedIndex = -1;

    // Pending slice operation state
    private boolean slicingActive = false;
    private List<Integer> pendingGeneIndices = List.of();
    private List<ItemStack> pendingOutputs = List.of();
    private List<String> pendingGeneNames = List.of();

    @Override
    public String getName() { return "slicer"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        // If exactly one slicer is adjacent, open it immediately
        if (cachedSlicers.size() == 1) {
            openedIndex = 0;
            view = View.DETAILS;
            renderDetails(ctx);
        } else {
            renderList(ctx);
        }
    }

    private void refresh(ConsoleContext ctx) {
        cachedSlicers.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();
        for (var dir : Direction.values()) {
            var be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof GeneSlicerBlockEntity gsb) cachedSlicers.add(gsb);
        }
    }

    private void renderList(ConsoleContext ctx) {
        ProgramScreenBuilder b = screen()
                .header("Gene Slicer")
                .line("Commands: list | open <n> | exit", ConsoleText.ColorTag.GRAY)
                .blank();
        if (cachedSlicers.isEmpty()) {
            b.line("No connected slicers.", ConsoleText.ColorTag.GRAY);
        } else {
            b.line("Connected Slicers:", ConsoleText.ColorTag.WHITE);
            for (int i = 0; i < cachedSlicers.size(); i++) {
                var s = cachedSlicers.get(i);
                int pct = s.getMaxProgress() == 0 ? 0 : (s.getProgress() * 100 / s.getMaxProgress());
                var in = s.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
                String sampleInfo = in.isEmpty() ? "<empty>" : (in.getItem() == ModItems.SEQUENCED_SAMPLE.get() ? describeSample(in) : "<invalid>");
                b.line((i + 1) + ") [" + bar(pct, 20) + "] " + pct + "% " + (s.isRunning() ? "(Running) " : "(Idle) ") + sampleInfo);
            }
        }
        render(ctx, b.build());
    }

    private String describeSample(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return "Sequenced Sample: 0 genes";
        int count = tag.contains("genes", 9) ? tag.getList("genes", 10).size() : 0;
        return "Sequenced Sample: " + count + " genes";
    }

    private String bar(int pct, int width) {
        int filled = Math.max(0, Math.min(width, (pct * width) / 100));
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < width; i++) r.append(i < filled ? '|' : '.');
        return r.toString();
    }

    private void renderDetails(ConsoleContext ctx) {
        if (openedIndex < 0 || openedIndex >= cachedSlicers.size()) { view = View.LIST; renderList(ctx); return; }
        var slicer = cachedSlicers.get(openedIndex);
        var input = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
        ProgramScreenBuilder b = screen()
                .header("Gene Slicer")
                .line("Commands: slice <idx...> | back | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        if (input.isEmpty() || input.getItem() != ModItems.SEQUENCED_SAMPLE.get()) {
            b.line("Insert a sequenced_sample in the slicer input.", ConsoleText.ColorTag.RED);
        } else {
            CompoundTag tag = input.getTag();
            java.util.List<String> lines = new ArrayList<>();
            int total = 0;
            if (tag != null && tag.contains("genes", 9)) {
                var genes = tag.getList("genes", 10);
                total = genes.size();
                for (int i = 0; i < genes.size(); i++) {
                    var g = genes.getCompound(i);
                    String id = g.getString("id");
                    int q = g.getInt("quality");
                    String name = g.contains("name", 8) ? g.getString("name") : compactLabelFromId(id, q);
                    boolean selected = slicingActive && pendingGeneIndices.contains(i);
                    String line = String.format("%d) %s  (%s, %d%%)", i + 1, name, id, q);
                    lines.add(selected ? ConsoleText.color(line, ConsoleText.ColorTag.YELLOW) : line);
                }
            }
            if (lines.isEmpty()) {
                b.line("No genes present.", ConsoleText.ColorTag.GRAY);
            } else {
                b.line("Genes in sample:", ConsoleText.ColorTag.WHITE);
                for (String ln : lines) b.line(ln);
            }

            b.blank();
            int pct = slicer.getMaxProgress() == 0 ? 0 : (slicer.getProgress() * 100 / slicer.getMaxProgress());
            if (slicer.isRunning() || slicingActive) {
                b.line(ConsoleText.color("Slicing in progress", ConsoleText.ColorTag.AQUA));
                b.progressBar(pct, 28);
                if (pendingGeneNames != null && !pendingGeneNames.isEmpty()) {
                    for (String n : pendingGeneNames) {
                        b.line(ConsoleText.color(" - " + n, ConsoleText.ColorTag.YELLOW));
                    }
                }
            } else {
                b.line("Ready.", ConsoleText.ColorTag.GRAY);
            }
            b.blank().line("Tip: use 'slice 1 2 3' to slice up to 6 at once.", ConsoleText.ColorTag.GRAY);
        }

        render(ctx, b.build());
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, java.util.List<String> args) {
        switch (name) {
            case "list":
                refresh(ctx);
                view = View.LIST;
                openedIndex = -1;
                slicingActive = false;
                renderList(ctx);
                return true;
            case "open":
                if (args.isEmpty()) { ctx.println("Usage: open <n>"); return true; }
                int idx = parseIndex(args.get(0));
                if (idx < 0 || idx >= cachedSlicers.size()) { ctx.println("Invalid index"); return true; }
                openedIndex = idx;
                view = View.DETAILS;
                slicingActive = false;
                renderDetails(ctx);
                return true;
            case "slice":
                if (view != View.DETAILS) { ctx.println("Open a slicer first: open <n>"); return true; }
                if (openedIndex < 0 || openedIndex >= cachedSlicers.size()) { ctx.println("Invalid slicer state"); return true; }
                var slicer = cachedSlicers.get(openedIndex);
                var input = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
                if (input.isEmpty() || input.getItem() != ModItems.SEQUENCED_SAMPLE.get()) { ctx.println("Insert a sequenced_sample first."); return true; }
                var tag = input.getTag();
                if (tag == null || !tag.contains("genes", 9)) { ctx.println("Sample has no genes to slice."); return true; }
                var genes = tag.getList("genes", 10);
                if (args.isEmpty()) { ctx.println("Usage: slice <idx...>"); return true; }
                if (args.size() > GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT) { ctx.println("Can slice at most 6 genes at once."); return true; }
                // Parse unique 1-based indices -> 0-based
                java.util.Set<Integer> set = new java.util.LinkedHashSet<>();
                for (String a : args) {
                    int gi = parseIndex(a);
                    if (gi < 0 || gi >= genes.size()) { ctx.println("Invalid index: " + a); return true; }
                    set.add(gi);
                }
                java.util.List<Integer> toSlice = new java.util.ArrayList<>(set);
                // Check free outputs
                int free = 0;
                for (int i = 0; i < GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT; i++) if (slicer.getItem(GeneSlicerBlockEntity.SLOT_OUTPUT_START + i).isEmpty()) free++;
                if (free < toSlice.size()) { ctx.println("Not enough free output slots (need " + toSlice.size() + ", have " + free + ")."); return true; }

                // Build pending outputs now, validate categories
                java.util.List<ItemStack> outputs = new java.util.ArrayList<>();
                java.util.List<String> names = new java.util.ArrayList<>();
                for (int gi : toSlice) {
                    CompoundTag g = genes.getCompound(gi);
                    String idStr = g.getString("id");
                    int quality = g.getInt("quality");
                    String nameStr = g.contains("name", 8) ? g.getString("name") : compactLabelFromId(idStr, quality);
                    Item vialItem = pickVialForGene(idStr);
                    ItemStack vial = new ItemStack(vialItem);
                    CompoundTag vtag = vial.getOrCreateTag();
                    // Preserve original gene data under a single subtag for future systems
                    vtag.put("gene", g.copy());
                    // Propagate sample metadata for traceability
                    if (tag.contains("entity_uuid", 8)) vtag.putString("entity_uuid", tag.getString("entity_uuid"));
                    if (tag.contains("entity_name", 8)) vtag.putString("entity_name", tag.getString("entity_name"));
                    // Custom display name includes gene name
                    vial.setHoverName(Component.literal("Gene Vial - " + nameStr).withStyle(ChatFormatting.WHITE));
                    outputs.add(vial);
                    names.add(nameStr);
                }

                // Stage and start processing
                pendingGeneIndices = toSlice;
                pendingOutputs = outputs;
                pendingGeneNames = names;
                slicingActive = true;
                slicer.startProcessing();
                renderDetails(ctx);
                return true;
            case "back":
                if (view == View.DETAILS) {
                    view = View.LIST;
                    openedIndex = -1;
                    slicingActive = false;
                    renderList(ctx);
                    return true;
                }
                return false;
            case "exit":
                ctx.exitProgram();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onTick(ConsoleContext ctx) {
        if (view == View.DETAILS && openedIndex >= 0 && openedIndex < cachedSlicers.size()) {
            var slicer = cachedSlicers.get(openedIndex);
            if (slicingActive) {
                if (!slicer.isRunning()) {
                    // Processing finished -> commit outputs and mutate input
                    commitSlice(ctx, slicer);
                    slicingActive = false;
                }
                renderDetails(ctx);
            } else {
                // Periodic refresh of progress/status
                renderDetails(ctx);
            }
        } else if (view == View.LIST) {
            renderList(ctx);
        }
    }

    private void commitSlice(ConsoleContext ctx, GeneSlicerBlockEntity slicer) {
        try {
            var input = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
            if (input.isEmpty() || input.getItem() != ModItems.SEQUENCED_SAMPLE.get()) {
                setStatusErr("Slicing failed: input was removed.");
                return;
            }
            CompoundTag tag = input.getTag();
            if (tag == null || !tag.contains("genes", 9)) {
                setStatusErr("Slicing failed: sample has no genes.");
                return;
            }
            var genes = tag.getList("genes", 10);
            // Place vials in first free outputs
            int outPlaced = 0;
            outer: for (ItemStack vial : pendingOutputs) {
                for (int i = 0; i < GeneSlicerBlockEntity.SLOT_OUTPUT_COUNT; i++) {
                    int slot = GeneSlicerBlockEntity.SLOT_OUTPUT_START + i;
                    if (slicer.getItem(slot).isEmpty()) {
                        slicer.setItem(slot, vial);
                        outPlaced++;
                        continue outer;
                    }
                }
                // No space -> stop placing further
                break;
            }
            if (outPlaced != pendingOutputs.size()) {
                setStatusWarn("Outputs filled mid-process; placed " + outPlaced + "/" + pendingOutputs.size());
            } else {
                setStatusOk("Slicing complete: " + outPlaced + " vial(s) created.");
            }
            // Remove genes by descending indices
            java.util.List<Integer> sorted = new java.util.ArrayList<>(pendingGeneIndices);
            sorted.sort(java.util.Comparator.reverseOrder());
            for (int idx : sorted) {
                if (idx >= 0 && idx < genes.size()) genes.remove(idx);
            }
            if (genes.isEmpty()) {
                // Leave an empty list or clear entirely
                tag.remove("genes");
            }
            slicer.setItem(GeneSlicerBlockEntity.SLOT_INPUT, input); // ensure setChanged
        } catch (Exception e) {
            setStatusErr("Slicing error: " + e.getMessage());
        }
    }

    private int parseIndex(String token) {
        try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; }
    }

    private String compactLabelFromId(String id, int quality) {
        return "gene_" + String.format("%04x", Math.abs((id + "_" + quality).hashCode()) & 0xFFFF);
    }

    private Item pickVialForGene(String idStr) {
        try {
            Gene g = GeneRegistry.get(new ResourceLocation(idStr));
            if (g != null) {
                return switch (g.getCategory()) {
                    case resistance -> ModItems.GENE_VIAL_RESISTANCE.get();
                    case physique -> ModItems.GENE_VIAL_BUILDER.get();
                    case aesthetic -> ModItems.GENE_VIAL_COSMETIC.get();
                    case quirk_misc -> ModItems.GENE_VIAL_QUIRK.get();
                };
            }
        } catch (Exception ignored) {}
        // Fallback
        return ModItems.GENE_VIAL_BUILDER.get();
    }
}


