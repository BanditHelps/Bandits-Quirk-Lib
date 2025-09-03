package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneCombinationService;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.GeneCombinerBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Combiner program: controls adjacent GeneCombiner blocks, shows contents, and performs gene combination.
 */
public class CombinerProgram extends AbstractConsoleProgram {
    private final List<GeneCombinerBlockEntity> cachedCombiners = new ArrayList<>();
    private final List<SampleRefrigeratorBlockEntity> cachedFridges = new ArrayList<>();
    private int selectedCombiner = -1;
    private boolean running = false;
    private int progress = 0;
    private int maxProgress = 200;

    @Override
    public String getName() { return "combiner"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        if (cachedCombiners.size() == 1) selectedCombiner = 0;
        render(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        cachedCombiners.clear();
        cachedFridges.clear();
        var be = ctx.getBlockEntity();
        var level = be.getLevel();
        var pos = be.getBlockPos();
        for (var dir : Direction.values()) {
            var neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof GeneCombinerBlockEntity c) cachedCombiners.add(c);
            if (neighbor instanceof SampleRefrigeratorBlockEntity f) cachedFridges.add(f);
        }
        if (selectedCombiner < 0 || selectedCombiner >= cachedCombiners.size()) selectedCombiner = -1;
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        switch (name) {
            case "list": refresh(ctx); render(ctx); return true;
            case "target":
                if (args.isEmpty()) { setStatusWarn("Usage: target <n>"); render(ctx); return true; }
                int t = parseIndex(args.get(0));
                if (t < 0 || t >= cachedCombiners.size()) { setStatusErr("Invalid combiner index"); render(ctx); return true; }
                selectedCombiner = t;
                render(ctx);
                return true;
            case "load":
                doLoadFromFridges(ctx);
                return true;
            case "combine":
                doCombine(ctx);
                return true;
            case "exit": ctx.exitProgram(); return true;
            default: return false;
        }
    }

    private void render(ConsoleContext ctx) {
        ProgramScreenBuilder b = screen()
                .header("Gene Combiner")
                .line("Commands: list | target <n> | load | combine | exit", ConsoleText.ColorTag.GRAY)
                .separator();

        if (cachedCombiners.isEmpty()) {
            b.line("No GeneCombiner adjacent.", ConsoleText.ColorTag.RED);
        } else {
            b.line("Combiners:", ConsoleText.ColorTag.WHITE);
            for (int i = 0; i < cachedCombiners.size(); i++) {
                boolean sel = i == selectedCombiner;
                var c = cachedCombiners.get(i);
                String status = describeCombiner(c);
                b.line(String.format("%d) %s %s", i + 1, sel ? "[Selected]" : "", status), sel ? ConsoleText.ColorTag.AQUA : ConsoleText.ColorTag.GRAY);
            }
        }

        if (running) {
            int pct = maxProgress == 0 ? 0 : (progress * 100 / maxProgress);
            b.blank();
            b.line("Combining...", ConsoleText.ColorTag.AQUA);
            b.progressBar(pct, 28);
        }

        render(ctx, b.build());
    }

    private String describeCombiner(GeneCombinerBlockEntity c) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
            ItemStack st = c.getItem(i);
            labels.add(st.isEmpty() ? "-" : shortLabel(st));
        }
        String out = c.getItem(GeneCombinerBlockEntity.SLOT_OUTPUT).isEmpty() ? "<empty>" : shortLabel(c.getItem(GeneCombinerBlockEntity.SLOT_OUTPUT));
        return String.format("Inputs: %s | Out: %s", String.join(", ", labels), out);
    }

    private String shortLabel(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return "-";
        CompoundTag tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            CompoundTag g = tag.getCompound("gene");
            String name = g.contains("name", 8) ? g.getString("name") : "gene";
            Integer q = g.contains("quality", 3) ? g.getInt("quality") : null;
            return q == null ? name : (name + " (" + q + "%)");
        }
        return vial.getHoverName().getString();
    }

    private void doLoadFromFridges(ConsoleContext ctx) {
        if (selectedCombiner < 0 || selectedCombiner >= cachedCombiners.size()) { setStatusErr("Select a combiner with 'target <n>'"); render(ctx); return; }
        var comb = cachedCombiners.get(selectedCombiner);
        // count free input slots
        int free = 0;
        for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) if (comb.getItem(i).isEmpty()) free++;
        if (free == 0) { setStatusWarn("No free input slots."); render(ctx); return; }

        // fill from first available gene vials in adjacent fridges
        int placed = 0;
        for (var fridge : cachedFridges) {
            for (int s = 0; s < SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                if (placed >= free) break;
                ItemStack st = fridge.getItem(s);
                if (st.isEmpty() || !isGeneVial(st)) continue;
                // find target slot
                int tgt = -1;
                for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) if (comb.getItem(i).isEmpty()) { tgt = i; break; }
                if (tgt < 0) break;
                // move
                fridge.setItem(s, ItemStack.EMPTY);
                comb.setItem(tgt, st);
                placed++;
            }
        }
        setStatusInfo("Loaded " + placed + " vial(s).");
        render(ctx);
    }

    private void doCombine(ConsoleContext ctx) {
        if (running) { setStatusWarn("Already running."); render(ctx); return; }
        if (selectedCombiner < 0 || selectedCombiner >= cachedCombiners.size()) { setStatusErr("Select a combiner with 'target <n>'"); render(ctx); return; }
        var comb = cachedCombiners.get(selectedCombiner);
        if (!comb.getItem(GeneCombinerBlockEntity.SLOT_OUTPUT).isEmpty()) { setStatusErr("Output not empty."); render(ctx); return; }

        // Gather inputs
        List<CompoundTag> genes = new ArrayList<>();
        for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
            ItemStack st = comb.getItem(i);
            if (st.isEmpty()) continue;
            CompoundTag tag = st.getTag();
            if (tag != null && tag.contains("gene", 10)) genes.add(tag.getCompound("gene").copy());
        }
        if (genes.isEmpty()) { setStatusWarn("Insert up to 4 gene vials."); render(ctx); return; }

        // Simulate processing time
        running = true;
        progress = 0;
        maxProgress = Math.max(100, 60 * genes.size());
        setStatusInfo("Combining started");
        render(ctx);
    }

    @Override
    public void onTick(ConsoleContext ctx) {
        if (!running) { render(ctx); return; }
        progress = Math.min(progress + 10, maxProgress);
        if (progress >= maxProgress) {
            completeCombine(ctx);
            running = false;
        }
        render(ctx);
    }

    private void completeCombine(ConsoleContext ctx) {
        try {
            if (selectedCombiner < 0 || selectedCombiner >= cachedCombiners.size()) { setStatusErr("Combiner missing"); return; }
            var comb = cachedCombiners.get(selectedCombiner);
            // Extract live inputs to reflect any changes during processing
            List<GeneCombinationService.GeneIngredient> provided = new ArrayList<>();
            for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
                ItemStack st = comb.getItem(i);
                if (st.isEmpty()) continue;
                CompoundTag tag = st.getTag();
                if (tag != null && tag.contains("gene", 10)) {
                    CompoundTag g = tag.getCompound("gene");
                    String id = g.getString("id");
                    int q = g.contains("quality", 3) ? g.getInt("quality") : 0;
                    try {
                        provided.add(new GeneCombinationService.GeneIngredient(new ResourceLocation(id), q));
                    } catch (Exception ignored) {}
                }
            }

            // Determine target gene(s): any combinable gene with a recipe that matches the provided inputs
            Gene result = null;
            for (Gene g : GeneRegistry.all()) {
                if (!g.isCombinable() || g.getCombinationRecipe() == null) continue;
                boolean ok = false;
                try {
                    var server = ((ServerLevel)ctx.getBlockEntity().getLevel()).getServer();
                    ok = GeneCombinationService.matchesRecipe(server, g, provided);
                } catch (Exception ignored) {}
                if (ok) { result = g; break; }
            }

            // Consume inputs
            for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) comb.setItem(i, ItemStack.EMPTY);

            ItemStack output;
            if (result != null) {
                // Build a gene vial based on category
                ItemStack vial;
                switch (result.getCategory()) {
                    case cosmetic -> vial = new ItemStack(ModItems.GENE_VIAL_COSMETIC.get());
                    case resistance -> vial = new ItemStack(ModItems.GENE_VIAL_RESISTANCE.get());
                    case builder -> vial = new ItemStack(ModItems.GENE_VIAL_BUILDER.get());
                    case quirk -> vial = new ItemStack(ModItems.GENE_VIAL_QUIRK.get());
                    default -> vial = new ItemStack(ModItems.GENE_VIAL_BUILDER.get());
                }
                CompoundTag gene = new CompoundTag();
                gene.putString("id", result.getId().toString());
                // For quality, take min of provided qualities meeting requirements
                int q = 0;
                for (var ing : provided) q = Math.max(q, ing.quality);
                gene.putInt("quality", Math.max(result.getQualityMin(), Math.min(result.getQualityMax(), q)));
                // Only show translated name if already known; otherwise hide to require Identify
                boolean known = false;
                try { known = ctx.getBlockEntity().hasDatabase() && ctx.getBlockEntity().isGeneKnown(result.getId()); } catch (Exception ignored) {}
                if (known) {
                    gene.putString("name", net.minecraft.network.chat.Component.translatable(result.getId().toString()).getString());
                } else {
                    gene.putString("name", "");
                }
                CompoundTag vtag = vial.getOrCreateTag();
                vtag.put("gene", gene);
                output = vial;
                setStatusOk("Combination complete: " + (known ? net.minecraft.network.chat.Component.translatable(result.getId().toString()).getString() : "unknown gene"));
            } else {
                // Failed sample
                output = new ItemStack(ModItems.FAILED_SAMPLE.get());
                setStatusWarn("Combination failed: produced a failed sample.");
            }

            // Place in output or drop into first input
            if (!comb.getItem(GeneCombinerBlockEntity.SLOT_OUTPUT).isEmpty()) {
                // fallback
                for (int i = 0; i < GeneCombinerBlockEntity.SLOT_INPUT_COUNT; i++) {
                    if (comb.getItem(i).isEmpty()) { comb.setItem(i, output); output = ItemStack.EMPTY; break; }
                }
            } else {
                comb.setItem(GeneCombinerBlockEntity.SLOT_OUTPUT, output);
            }
        } catch (Exception e) {
            setStatusErr("Error: " + e.getMessage());
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

    private int parseIndex(String token) { try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; } }
}


