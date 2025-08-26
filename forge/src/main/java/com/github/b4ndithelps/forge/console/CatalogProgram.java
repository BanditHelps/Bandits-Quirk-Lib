package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Catalog program: lists all gene vials stored in any Sample Refrigerator adjacent to the Bio Terminal.
 * Supports viewing details for a specific vial by index.
 */
public class CatalogProgram extends AbstractConsoleProgram {
    private enum View { LIST, DETAILS }

    private static final class VialEntry {
        final int fridgeIndex; // index within cachedFridges
        final int slotIndex;   // slot within that fridge
        final ItemStack stack;
        final String displayName;
        final String geneId;
        final Integer quality;
        final Gene.Category category;

        VialEntry(int fridgeIndex, int slotIndex, ItemStack stack, String displayName, String geneId, Integer quality, Gene.Category category) {
            this.fridgeIndex = fridgeIndex;
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.displayName = displayName;
            this.geneId = geneId;
            this.quality = quality;
            this.category = category;
        }
    }

    private final List<SampleRefrigeratorBlockEntity> cachedFridges = new ArrayList<>();
    private final List<VialEntry> entries = new ArrayList<>();
    private View view = View.LIST;
    private int openedIndex = -1; // index into entries

    @Override
    public String getName() { return "catalog"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        renderList(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        cachedFridges.clear();
        entries.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();
        for (var dir : Direction.values()) {
            var be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof SampleRefrigeratorBlockEntity fridge) cachedFridges.add(fridge);
        }
        // Collect vial entries
        for (int f = 0; f < cachedFridges.size(); f++) {
            var fridge = cachedFridges.get(f);
            for (int s = 0; s < SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                var st = fridge.getItem(s);
                if (st == null || st.isEmpty()) continue;
                if (!isGeneVial(st)) continue;
                var info = extractGeneInfo(st);
                entries.add(new VialEntry(f, s, st.copy(), info.displayName, info.geneId, info.quality, info.category));
            }
        }
        // Keep entries stable and pretty: sort by category then name
        entries.sort(Comparator.comparing((VialEntry e) -> e.category == null ? "zzz" : e.category.name())
                .thenComparing(e -> e.displayName == null ? "" : e.displayName));
    }

    private record GeneInfo(String displayName, String geneId, Integer quality, Gene.Category category) {}

    private GeneInfo extractGeneInfo(ItemStack vial) {
        String display = vial.hasCustomHoverName() ? vial.getHoverName().getString() : "Gene Vial";
        String geneId = "";
        Integer quality = null;
        Gene.Category category = null;
        CompoundTag tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            CompoundTag gene = tag.getCompound("gene");
            if (gene.contains("id", 8)) {
                geneId = gene.getString("id");
                try {
                    var g = GeneRegistry.get(new ResourceLocation(geneId));
                    if (g != null) category = g.getCategory();
                } catch (Exception ignored) {}
            }
            if (gene.contains("quality", 3)) quality = gene.getInt("quality");
            if (gene.contains("name", 8)) display = gene.getString("name");
        }
        return new GeneInfo(display, geneId, quality, category);
    }

    private boolean isGeneVial(ItemStack stack) {
        var it = stack.getItem();
        return it instanceof GeneVialItem
                || it == ModItems.GENE_VIAL_COSMETIC.get()
                || it == ModItems.GENE_VIAL_RESISTANCE.get()
                || it == ModItems.GENE_VIAL_BUILDER.get()
                || it == ModItems.GENE_VIAL_QUIRK.get();
    }

    private void renderList(ConsoleContext ctx) {
        ProgramScreenBuilder b = screen()
                .header("Sample Catalog")
                .line("Commands: list | info <n> | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        if (entries.isEmpty()) {
            b.line("No gene vials found in adjacent refrigerators.", ConsoleText.ColorTag.GRAY);
        } else {
            int idx = 1;
            for (VialEntry e : entries) {
                String cat = e.category == null ? "Unknown" : e.category.name();
                String line = String.format("%d) %s  [%s]  %s", idx, e.displayName, cat, e.quality == null ? "" : (e.quality + "%"));
                ConsoleText.ColorTag color = colorForCategory(e.category);
                b.line(ConsoleText.color(line, color));
                idx++;
            }
        }

        render(ctx, b.build());
        view = View.LIST;
        openedIndex = -1;
    }

    private ConsoleText.ColorTag colorForCategory(Gene.Category c) {
        if (c == null) return ConsoleText.ColorTag.WHITE;
        return switch (c) {
            case aesthetic -> ConsoleText.ColorTag.AQUA;
            case resistance -> ConsoleText.ColorTag.GREEN;
            case physique -> ConsoleText.ColorTag.YELLOW;
            case quirk_misc -> ConsoleText.ColorTag.RED;
        };
    }

    private void renderDetails(ConsoleContext ctx) {
        if (openedIndex < 0 || openedIndex >= entries.size()) { renderList(ctx); return; }
        VialEntry e = entries.get(openedIndex);
        ProgramScreenBuilder b = screen()
                .header("Vial Details")
                .line("Commands: back | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        b.line("Name: " + e.displayName, ConsoleText.ColorTag.WHITE);
        b.line("Gene ID: " + (e.geneId == null ? "" : e.geneId), ConsoleText.ColorTag.GRAY);
        b.line("Category: " + (e.category == null ? "Unknown" : e.category.name()), colorForCategory(e.category));
        b.line("Quality: " + (e.quality == null ? "?" : (e.quality + "%")), ConsoleText.ColorTag.YELLOW);
        b.blank();
        b.line(String.format("Source: Fridge %d, Slot %d", e.fridgeIndex + 1, e.slotIndex + 1), ConsoleText.ColorTag.GRAY);

        // Show any extra NBT fields of interest
        CompoundTag tag = e.stack.getTag();
        if (tag != null) {
            if (tag.contains("entity_name", 8)) b.line("From: " + tag.getString("entity_name"), ConsoleText.ColorTag.GRAY);
            if (tag.contains("entity_uuid", 8)) b.line("UUID: " + tag.getString("entity_uuid"), ConsoleText.ColorTag.GRAY);
        }

        render(ctx, b.build());
        view = View.DETAILS;
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        if ("list".equalsIgnoreCase(name)) {
            refresh(ctx);
            renderList(ctx);
            return true;
        }
        if ("info".equalsIgnoreCase(name)) {
            if (args.isEmpty()) { setStatusWarn("Usage: info <n>"); renderList(ctx); return true; }
            try {
                int n = Integer.parseInt(args.get(0)) - 1;
                if (n < 0 || n >= entries.size()) { setStatusErr("Invalid index"); renderList(ctx); return true; }
                openedIndex = n;
                renderDetails(ctx);
            } catch (Exception e) {
                setStatusErr("Invalid number");
                renderList(ctx);
            }
            return true;
        }
        if ("back".equalsIgnoreCase(name)) {
            renderList(ctx);
            return true;
        }
        if ("exit".equalsIgnoreCase(name)) {
            ctx.exitProgram();
            return true;
        }
        return false;
    }
}


