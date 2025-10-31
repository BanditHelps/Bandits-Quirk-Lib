package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Printer program: lets user select gene vials from adjacent refrigerators,
 * loads them into an adjacent BioPrinter, simulates processing, then outputs an Injector.
 */
public class PrinterProgram extends AbstractConsoleProgram {
    private static final int MAX_SELECTION = BioPrinterBlockEntity.SLOT_INPUT_COUNT;

    private final List<SampleRefrigeratorBlockEntity> cachedFridges = new ArrayList<>();
    private final List<BioPrinterBlockEntity> cachedPrinters = new ArrayList<>();

    private static final class VialEntry {
        final int fridgeIndex;
        final int slotIndex;
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

    private final List<VialEntry> entries = new ArrayList<>();
    private final List<Integer> selection = new ArrayList<>(); // indices into entries
    private int selectedPrinterIndex = -1;

    private boolean printingActive = false;
    private int progress = 0;
    private int maxProgress = 200;
    private List<VialEntry> stagedForPrinting = List.of();

    @Override
    public String getName() { return "printer"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        // If exactly one printer, select it by default
        if (cachedPrinters.size() == 1) selectedPrinterIndex = 0;
        renderList(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        cachedFridges.clear();
        cachedPrinters.clear();
        entries.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();
        java.util.Set<net.minecraft.world.level.block.entity.BlockEntity> connected = CableNetworkUtil.findConnected(level, pos,
                be -> be instanceof SampleRefrigeratorBlockEntity || be instanceof BioPrinterBlockEntity);
        for (var be : connected) {
            if (be instanceof SampleRefrigeratorBlockEntity fridge) cachedFridges.add(fridge);
            if (be instanceof BioPrinterBlockEntity printer) cachedPrinters.add(printer);
        }
        // collect vial entries
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
        // Sort for stable UX
        entries.sort(Comparator.comparing((VialEntry e) -> e.category == null ? "zzz" : e.category.name())
                .thenComparing(e -> e.displayName == null ? "" : e.displayName));
        // Drop out-of-range selections if entries changed
        selection.removeIf(i -> i < 0 || i >= entries.size());
        if (selectedPrinterIndex < 0 || selectedPrinterIndex >= cachedPrinters.size()) selectedPrinterIndex = -1;
    }

    private boolean isGeneVial(ItemStack stack) {
        var it = stack.getItem();
        return it instanceof GeneVialItem
                || it == ModItems.GENE_VIAL_COSMETIC.get()
                || it == ModItems.GENE_VIAL_RESISTANCE.get()
                || it == ModItems.GENE_VIAL_BUILDER.get()
                || it == ModItems.GENE_VIAL_QUIRK.get();
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
            if (gene.contains("name", 8))  display = gene.getString("name");
        }
        return new GeneInfo(display, geneId, quality, category);
    }

    private void renderList(ConsoleContext ctx) {
        ProgramScreenBuilder b = screen()
                .header("Bio Printer")
                .line("Commands: list | select <idx...> | remove <idx...> | clear | target <n> | build | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        if (cachedPrinters.isEmpty()) {
            b.line("No BioPrinter adjacent.", ConsoleText.ColorTag.RED);
        } else {
            b.line("Printers:", ConsoleText.ColorTag.WHITE);
            for (int i = 0; i < cachedPrinters.size(); i++) {
                boolean sel = i == selectedPrinterIndex;
                b.line(String.format("%d) %s", i + 1, sel ? "[Selected]" : ""), sel ? ConsoleText.ColorTag.AQUA : ConsoleText.ColorTag.GRAY);
            }
        }

        b.blank();
        if (entries.isEmpty()) {
            b.line("No gene vials in adjacent refrigerators.", ConsoleText.ColorTag.GRAY);
        } else {
            b.line("Available Vials:", ConsoleText.ColorTag.WHITE);
            int idx = 1;
            for (VialEntry e : entries) {
                boolean known = false;
                try { known = ctx.getBlockEntity().isGeneKnown(new ResourceLocation(e.geneId)); } catch (Exception ignored) {}
                String cat = (known && e.category != null) ? e.category.name() : "Unknown";
                String qual = (known && e.quality != null) ? (e.quality + "%") : "";
                String line = String.format("%d) %s  [%s]  %s", idx, e.displayName, cat, qual);
                ConsoleText.ColorTag color = known ? colorForCategory(e.category) : ConsoleText.ColorTag.WHITE;
                if (selection.contains(idx - 1)) {
                    line = "* " + line;
                    color = ConsoleText.ColorTag.YELLOW;
                }
                b.line(ConsoleText.color(line, color));
                idx++;
            }
        }

        if (printingActive) {
            int pct = maxProgress == 0 ? 0 : (progress * 100 / maxProgress);
            b.blank();
            b.line(ConsoleText.color("Printing in progress", ConsoleText.ColorTag.AQUA));
            b.progressBar(pct, 28);
        }

        render(ctx, b.build());
    }

    private ConsoleText.ColorTag colorForCategory(Gene.Category c) {
        if (c == null) return ConsoleText.ColorTag.WHITE;
        return switch (c) {
            case cosmetic -> ConsoleText.ColorTag.AQUA;
            case resistance -> ConsoleText.ColorTag.GREEN;
            case builder -> ConsoleText.ColorTag.YELLOW;
            case quirk -> ConsoleText.ColorTag.RED;
        };
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        switch (name) {
            case "list":
                refresh(ctx);
                renderList(ctx);
                return true;
            case "select":
                if (args.isEmpty()) { setStatusWarn("Usage: select <idx...>"); renderList(ctx); return true; }
                for (String a : args) {
                    int i = parseIndex(a);
                    if (i < 0 || i >= entries.size()) { setStatusErr("Invalid index: " + a); renderList(ctx); return true; }
                    if (!selection.contains(i)) selection.add(i);
                }
                if (selection.size() > MAX_SELECTION) {
                    setStatusWarn("Selected more than " + MAX_SELECTION + "; truncating.");
                    while (selection.size() > MAX_SELECTION) selection.remove(selection.size() - 1);
                }
                renderList(ctx);
                return true;
            case "remove":
                if (args.isEmpty()) { setStatusWarn("Usage: remove <idx...>"); renderList(ctx); return true; }
                for (String a : args) {
                    int i = parseIndex(a);
                    selection.remove((Integer)i);
                }
                renderList(ctx);
                return true;
            case "clear":
                selection.clear();
                renderList(ctx);
                return true;
            case "target":
                if (args.isEmpty()) { setStatusWarn("Usage: target <n>"); renderList(ctx); return true; }
                int t = parseIndex(args.get(0));
                if (t < 0 || t >= cachedPrinters.size()) { setStatusErr("Invalid printer index"); renderList(ctx); return true; }
                selectedPrinterIndex = t;
                renderList(ctx);
                return true;
            case "build":
                doBuild(ctx);
                return true;
            case "exit":
                ctx.exitProgram();
                return true;
            default:
                return false;
        }
    }

    private void doBuild(ConsoleContext ctx) {
        if (printingActive) { setStatusWarn("Already printing."); renderList(ctx); return; }
        if (selectedPrinterIndex < 0 || selectedPrinterIndex >= cachedPrinters.size()) { setStatusErr("Select a BioPrinter with 'target <n>'"); renderList(ctx); return; }
        if (selection.isEmpty()) { setStatusWarn("Select at least one vial."); renderList(ctx); return; }
        var printer = cachedPrinters.get(selectedPrinterIndex);
        // Ensure output empty
        if (!printer.getItem(BioPrinterBlockEntity.SLOT_OUTPUT).isEmpty()) { setStatusErr("Printer output not empty."); renderList(ctx); return; }
        // Count free input slots
        int free = 0;
        for (int i = 0; i < BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) if (printer.getItem(i).isEmpty()) free++;
        if (free < selection.size()) { setStatusErr("Printer needs " + selection.size() + " free inputs (has " + free + ")."); renderList(ctx); return; }

        // Stage entries and move items from fridges to printer inputs
        List<VialEntry> chosen = new ArrayList<>();
        for (int sel : selection) chosen.add(entries.get(sel));

        // Move actual stacks (consume from fridge)
        int nextSlot = 0;
        for (VialEntry e : chosen) {
            // find free input slot
            while (nextSlot < BioPrinterBlockEntity.SLOT_INPUT_COUNT && !printer.getItem(nextSlot).isEmpty()) nextSlot++;
            if (nextSlot >= BioPrinterBlockEntity.SLOT_INPUT_COUNT) { setStatusErr("Unexpected: ran out of input slots."); renderList(ctx); return; }
            ItemStack vial = cachedFridges.get(e.fridgeIndex).getItem(e.slotIndex);
            if (vial.isEmpty()) { setStatusErr("Source vial missing: fridge " + (e.fridgeIndex + 1) + ", slot " + (e.slotIndex + 1)); renderList(ctx); return; }
            // move single item
            cachedFridges.get(e.fridgeIndex).setItem(e.slotIndex, ItemStack.EMPTY);
            printer.setItem(nextSlot, vial);
            nextSlot++;
        }

        // Begin faux processing
        this.progress = 0;
        this.maxProgress = Math.max(100, 60 * chosen.size()); // simple scaling
        this.printingActive = true;
        this.stagedForPrinting = List.copyOf(chosen);
        setStatusInfo("Printing started");
        renderList(ctx);
    }

    @Override
    public void onTick(ConsoleContext ctx) {
        if (!printingActive) { renderList(ctx); return; }
        progress = Math.min(progress + 10, maxProgress);
        if (progress >= maxProgress) {
            // complete
            commitPrint(ctx);
            printingActive = false;
        }
        renderList(ctx);
    }

    private void commitPrint(ConsoleContext ctx) {
        try {
            if (selectedPrinterIndex < 0 || selectedPrinterIndex >= cachedPrinters.size()) { setStatusErr("Printer missing"); return; }
            var printer = cachedPrinters.get(selectedPrinterIndex);
            // Build genome from current inputs (not from staged list to reflect live state)
            ListTag genome = new ListTag();
            String entityName = null;
            String entityUuid = null;
            for (int i = 0; i < BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) {
                ItemStack st = printer.getItem(i);
                if (st.isEmpty()) continue;
                CompoundTag tag = st.getTag();
                if (tag != null && tag.contains("gene", 10)) {
                    CompoundTag g = tag.getCompound("gene");
                    genome.add(g.copy());
                }
                if (entityName == null && tag != null && tag.contains("entity_name", 8)) entityName = tag.getString("entity_name");
                if (entityUuid == null && tag != null && tag.contains("entity_uuid", 8)) entityUuid = tag.getString("entity_uuid");
            }
            // Clear inputs
            for (int i = 0; i < BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) printer.setItem(i, ItemStack.EMPTY);

            // Create injector
            ItemStack injector = new ItemStack(ModItems.INJECTOR.get());
            CompoundTag itag = injector.getOrCreateTag();
            itag.put("genome", genome);
            if (entityName != null) itag.putString("entity_name", entityName);
            if (entityUuid != null) itag.putString("entity_uuid", entityUuid);

            // Place in output
            if (!printer.getItem(BioPrinterBlockEntity.SLOT_OUTPUT).isEmpty()) {
                setStatusWarn("Output occupied; dropping injector");
                // If output occupied unexpectedly, try to place in first input else drop via container rules
                for (int i = 0; i < BioPrinterBlockEntity.SLOT_INPUT_COUNT; i++) {
                    if (printer.getItem(i).isEmpty()) { printer.setItem(i, injector); injector = ItemStack.EMPTY; break; }
                }
                if (!injector.isEmpty()) {
                    // No simple world drop API here; leave as is to avoid NPE. In practice, output should be empty.
                }
            } else {
                printer.setItem(BioPrinterBlockEntity.SLOT_OUTPUT, injector);
            }
            setStatusOk("Printing complete!");
        } catch (Exception e) {
            setStatusErr("Printing error: " + e.getMessage());
        }
    }

    private int parseIndex(String token) {
        try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; }
    }
}



