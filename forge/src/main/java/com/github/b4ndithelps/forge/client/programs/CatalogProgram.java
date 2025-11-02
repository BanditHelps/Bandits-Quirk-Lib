package com.github.b4ndithelps.forge.client.programs;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import com.github.b4ndithelps.forge.client.BioTerminalScreen;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.github.b4ndithelps.genetics.GeneRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side catalog program for the ref screen.
 * Left: list all gene vials in connected sample containers and sequenced samples.
 * Right: details of the currently selected entry (all fields shown as unknown for now).
 */
public class CatalogProgram {
    private final BioTerminalScreen screen;
    private final BlockPos terminalPos;

    private final List<Entry> entries = new ArrayList<>();
    private int selectedIndex = 0;

    private static final class Tip { final int x, y, w, h; final Component text; Tip(int x,int y,int w,int h, Component text){ this.x=x; this.y=y; this.w=w; this.h=h; this.text=text; } }
    private final List<Tip> tooltips = new ArrayList<>();

    private enum EntryType { VIAL, SEQUENCED_SAMPLE, SEQUENCED_GENE, SECTION }

    private static final class Entry {
        final EntryType type;
        final String label; // display label in list
        final ItemStack stack; // optional for VIAL/SAMPLE
        final String geneId; // empty for SECTION/SAMPLE rows without specific gene
        final int quality; // -1 if not applicable
        final boolean known;
        final int progress; // 0..max if identifying
        final int max;
        final int sourceIndex; // fridge or sequencer index (0-based), -1 if N/A
        final int slotIndex;   // slot within fridge if applicable, -1 otherwise

        Entry(EntryType type, String label, ItemStack stack, String geneId, int quality, boolean known, int progress, int max, int sourceIndex, int slotIndex) {
            this.type = type;
            this.label = label;
            this.stack = stack;
            this.geneId = geneId == null ? "" : geneId;
            this.quality = quality;
            this.known = known;
            this.progress = progress;
            this.max = max;
            this.sourceIndex = sourceIndex;
            this.slotIndex = slotIndex;
        }
    }

    public CatalogProgram(BioTerminalScreen screen, BlockPos terminalPos) {
        this.screen = screen;
        this.terminalPos = terminalPos;
        requestSync();
        refresh();
    }

    public void refresh() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        entries.clear();

        // Prefer server-authoritative cache so we can see container contents client-side
        var cached = ClientCatalogCache.get(terminalPos);
        if (!cached.isEmpty()) {
            for (var dto : cached) {
                EntryType t = switch (dto.type) {
                    case "VIAL" -> EntryType.VIAL;
                    case "SEQUENCED_SAMPLE" -> EntryType.SEQUENCED_SAMPLE;
                    case "SEQUENCED_GENE" -> EntryType.SEQUENCED_GENE;
                    default -> EntryType.SECTION;
                };
                entries.add(new Entry(t, dto.label, ItemStack.EMPTY, dto.geneId, dto.quality, dto.known, dto.progress, dto.max, dto.sourceIndex, dto.slotIndex));
            }
        } else {
            // Fallback to local scan (may miss inventory data without block entity sync)
            List<SampleRefrigeratorBlockEntity> fridges = new ArrayList<>();
            var connectedFridges = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof SampleRefrigeratorBlockEntity);
            for (var be : connectedFridges) if (be instanceof SampleRefrigeratorBlockEntity f) fridges.add(f);

            if (!fridges.isEmpty()) {
                entries.add(new Entry(EntryType.SECTION, "[VIALS]", ItemStack.EMPTY, "", -1, false, 0, 0, -1, -1));
                for (int f = 0; f < fridges.size(); f++) {
                    var fridge = fridges.get(f);
                    for (int s = 0; s < SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                        var st = fridge.getItem(s);
                        if (st == null || st.isEmpty()) continue;
                        if (!isGeneVial(st)) continue;
                        String name = getVialGeneLabel(st);
                        String line = String.format("%s", name);
                        entries.add(new Entry(EntryType.VIAL, line, st.copy(), getVialGeneId(st), getVialQuality(st), false, 0, 0, f, s));
                    }
                }
            }

            List<GeneSequencerBlockEntity> sequencers = new ArrayList<>();
            var connectedSeq = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof GeneSequencerBlockEntity);
            for (var be : connectedSeq) if (be instanceof GeneSequencerBlockEntity g) sequencers.add(g);

            if (!sequencers.isEmpty()) {
                entries.add(new Entry(EntryType.SECTION, "[SAMPLES]", ItemStack.EMPTY, "", -1, false, 0, 0, -1, -1));
                for (int i = 0; i < sequencers.size(); i++) {
                    var seq = sequencers.get(i);
                    var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                    if (!out.isEmpty() && out.getTag() != null && out.getTag().contains("genes", 9)) {
                        var list = out.getTag().getList("genes", 10);
                        for (int gi = 0; gi < list.size(); gi++) {
                            var gTag = list.getCompound(gi);
                            String label = gTag.contains("name", 8) ? gTag.getString("name") : gTag.getString("id");
                            int q = gTag.contains("quality", 3) ? gTag.getInt("quality") : -1;
                            entries.add(new Entry(EntryType.SEQUENCED_GENE, label, ItemStack.EMPTY, gTag.getString("id"), q, false, 0, 0, i, gi));
                        }
                    }
                }
            }
        }

        // Ensure selection stays within bounds and not on a SECTION row
        if (entries.isEmpty()) {
            selectedIndex = 0;
        } else {
            selectedIndex = Math.min(selectedIndex, entries.size() - 1);
            if (selectedIndex < 0) selectedIndex = 0;
            snapToSelectable(0);
        }
    }

    public void moveSelection(int delta) {
        if (entries.isEmpty()) return;
        int idx = selectedIndex;
        int attempts = 0;
        do {
            idx = (idx + delta + entries.size()) % entries.size();
            attempts++;
            if (entries.get(idx).type != EntryType.SECTION) {
                selectedIndex = idx;
                return;
            }
        } while (attempts <= entries.size());
    }

    public void startSelected() {
        if (entries.isEmpty()) return;
        var sel = entries.get(selectedIndex);
        if (sel.type == EntryType.SECTION || sel.geneId == null || sel.geneId.isEmpty()) return;
        if (sel.known) return;
        // Client debug log
        try { net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(Component.literal("[Catalog] Request identify: " + sel.geneId)); } catch (Throwable ignored) {}
        int q = sel.quality >= 0 ? sel.quality : 50;
        String action = "catalog.identify:" + sel.geneId + ":" + q;
        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.sendToServer(
                new com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket(terminalPos, action, null)
        );
        // Immediately request a sync so progress appears quickly
        requestSync();
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, Font font) {
        tooltips.clear();
        int leftW = (int)Math.floor(w * 0.58);
        int rightW = w - leftW - 2;
        int listX = x;
        int listY = y;
        int infoX = x + leftW + 2;
        int infoY = y;

        // Header and DB indicator
        g.drawString(font, Component.literal("[CATALOG]"), listX, listY, 0x55FF55, false);
        // Show database status on the right of header
        boolean hasDb = false;
        var mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            var be = mc.level.getBlockEntity(terminalPos);
            if (be instanceof BioTerminalBlockEntity term) {
                hasDb = term.hasDatabase();
            }
        }
        String dbText = hasDb ? "[DB: inserted]" : "[DB: missing]";
        int dbX = listX + Math.max(0, leftW - font.width(dbText));
        g.drawString(font, Component.literal(dbText), dbX, listY, hasDb ? 0x55FF55 : 0xFF5555, false);
        int curY = listY + font.lineHeight + 2;

        if (entries.isEmpty()) {
            g.drawString(font, Component.literal("No vials or samples found."), listX, curY, 0xAAAAAA, false);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                var e = entries.get(i);
                if (e.type == EntryType.SECTION) {
                    g.drawString(font, Component.literal(e.label), listX, curY, 0xA0FFFFFF, false);
                } else {
                    boolean sel = (i == selectedIndex);
                    String prefix = sel ? ">> " : "   ";
                    int color = sel ? 0x55FF55 : 0xFFFFFF;
                    String suffix = "";
                    if (e.known) suffix = " [KNOWN]";
                    else if (e.max > 0) {
                        int pct = Math.max(0, Math.min(100, (int)Math.round(e.progress * 100.0 / Math.max(1, e.max))));
                        suffix = " [ID " + pct + "%]";
                    }
                    g.drawString(font, Component.literal(prefix + e.label + suffix), listX, curY, color, false);
                }
                curY += font.lineHeight;
                if (curY > y + h - font.lineHeight) break;
            }
        }

        // Vertical separator and details panel
        if (!entries.isEmpty()) {
            int sepX = infoX - 2;
            g.fill(sepX, y, sepX + 1, y + h, 0x33555555);
            drawDetails(g, infoX, infoY, rightW, h, font);
        }
    }

    private void drawDetails(GuiGraphics g, int x, int y, int w, int h, Font font) {
        int curY = y;
        g.drawString(font, Component.literal("Details"), x, curY, 0xA0FFFFFF, false);
        curY += font.lineHeight + 2;

        if (entries.isEmpty()) return;
        var sel = entries.get(selectedIndex);
        if (sel.type == EntryType.SECTION) return;

        boolean known = sel.known;
        String translatedName = (known && sel.geneId != null && !sel.geneId.isEmpty()) ? Component.translatable(sel.geneId).getString() : "unknown";
        drawClipped(g, font, "Name: " + translatedName, x, curY, 0xFFFFFF, w); curY += font.lineHeight;
        if (known && sel.geneId != null && !sel.geneId.isEmpty()) { drawClipped(g, font, "Gene ID: " + sel.geneId, x, curY, 0xAAAAAA, w); curY += font.lineHeight; }
        Component cat = getCategoryComponent(sel);
        drawClipped(g, font, "Category: " + (cat != null ? cat.getString() : "Unknown"), x, curY, 0xAAAAAA, w); curY += font.lineHeight;
        drawClipped(g, font, "Quality: " + ((known && sel.quality >= 0) ? (sel.quality + "%") : "unknown"), x, curY, 0xFFFF55, w); curY += font.lineHeight;
        if (!known && sel.max > 0) {
            int pct = Math.max(0, Math.min(100, (int)Math.round(sel.progress * 100.0 / Math.max(1, sel.max))));
            drawClipped(g, font, "Identifying: " + pct + "%", x, curY, 0x55FF55, w); curY += font.lineHeight;
        }
    }

    private void drawClipped(GuiGraphics g, Font font, String text, int x, int y, int color, int maxWidth) {
        int available = Math.max(0, maxWidth - 2);
        if (font.width(text) <= available) {
            g.drawString(font, Component.literal(text), x, y, color, false);
            return;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        int target = Math.max(0, available - ellipsisWidth);
        String clipped = text;
        while (!clipped.isEmpty() && font.width(clipped) > target) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        g.drawString(font, Component.literal(clipped + ellipsis), x, y, color, false);
        tooltips.add(new Tip(x, y, available, font.lineHeight, Component.literal(text)));
    }

    public void renderTooltips(GuiGraphics g, int mouseX, int mouseY, Font font) {
        for (var tip : tooltips) {
            boolean inside = mouseX >= tip.x && mouseX <= tip.x + tip.w && mouseY >= tip.y && mouseY <= tip.y + tip.h;
            if (inside) {
                g.renderTooltip(font, tip.text, mouseX, mouseY);
                return;
            }
        }
    }

    private void snapToSelectable(int directionIfOnSection) {
        if (entries.isEmpty()) return;
        if (entries.get(selectedIndex).type != EntryType.SECTION) return;
        // Move in provided direction until non-section
        int idx = selectedIndex;
        int delta = directionIfOnSection >= 0 ? 1 : -1;
        for (int attempts = 0; attempts < entries.size(); attempts++) {
            idx = (idx + delta + entries.size()) % entries.size();
            if (entries.get(idx).type != EntryType.SECTION) { selectedIndex = idx; return; }
        }
        // fallback: first non-section
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).type != EntryType.SECTION) { selectedIndex = i; return; }
    }

    private static boolean isGeneVial(ItemStack stack) {
        var it = stack.getItem();
        return it instanceof GeneVialItem
                || it == ModItems.GENE_VIAL_COSMETIC.get()
                || it == ModItems.GENE_VIAL_RESISTANCE.get()
                || it == ModItems.GENE_VIAL_BUILDER.get()
                || it == ModItems.GENE_VIAL_QUIRK.get();
    }

    private static String getStackDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        if (stack.hasCustomHoverName()) return stack.getHoverName().getString();
        return stack.getItem().getDescription().getString();
    }

    private static String getVialGeneLabel(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return "";
        var tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            var gene = tag.getCompound("gene");
            if (gene.contains("name", 8)) {
                String name = gene.getString("name");
                if (name != null && !name.isEmpty()) return name;
            }
            if (gene.contains("id", 8)) {
                String id = gene.getString("id");
                if (id != null && !id.isEmpty()) return id;
            }
        }
        return getStackDisplayName(vial);
    }

    private static String getVialGeneId(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return "";
        var tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            var gene = tag.getCompound("gene");
            if (gene.contains("id", 8)) return gene.getString("id");
        }
        return "";
    }

    private static int getVialQuality(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return -1;
        var tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            var gene = tag.getCompound("gene");
            if (gene.contains("quality", 3)) return gene.getInt("quality");
        }
        return -1;
    }

    public int getSelectedIndex() { return selectedIndex; }
    public int size() { return entries.size(); }

    public void requestSync() {
        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.sendToServer(
                new com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket(terminalPos, "catalog.sync", null)
        );
    }

    private Component getCategoryComponent(Entry sel) {
        // Prefer gene registry by id (works for sequenced entries and vials with id)
        if (sel.geneId != null && !sel.geneId.isEmpty()) {
            try {
                var gene = GeneRegistry.get(new ResourceLocation(sel.geneId));
                if (gene != null) {
                    String key = switch (gene.getCategory()) {
                        case cosmetic -> "category.bandits_quirk_lib.cosmetic";
                        case resistance -> "category.bandits_quirk_lib.resistance";
                        case builder -> "category.bandits_quirk_lib.builder";
                        case lowend -> "category.bandits_quirk_lib.lowend";
                        case quirk -> "category.bandits_quirk_lib.quirk";
                    };
                    return Component.translatable(key);
                }
            } catch (Exception ignored) {}
        }

        // Fallback: for vials, derive from the vial item type category
        if (sel.type == EntryType.VIAL) {
            ItemStack st = sel.stack;
            if (st == null || st.isEmpty()) {
                var mc = Minecraft.getInstance();
                if (mc != null && mc.level != null && sel.sourceIndex >= 0 && sel.slotIndex >= 0) {
                    List<SampleRefrigeratorBlockEntity> fridges = new ArrayList<>();
                    var connectedFridges = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof SampleRefrigeratorBlockEntity);
                    for (var be : connectedFridges) if (be instanceof SampleRefrigeratorBlockEntity f) fridges.add(f);
                    if (sel.sourceIndex >= 0 && sel.sourceIndex < fridges.size()) {
                        var fridge = fridges.get(sel.sourceIndex);
                        if (fridge != null) st = fridge.getItem(sel.slotIndex);
                    }
                }
            }
            if (st != null && !st.isEmpty() && st.getItem() instanceof GeneVialItem vialItem) {
                String key = switch (vialItem.getCategory()) {
                    case COSMETIC -> "category.bandits_quirk_lib.cosmetic";
                    case RESISTANCE -> "category.bandits_quirk_lib.resistance";
                    case BUILDER -> "category.bandits_quirk_lib.builder";
                    case LOWEND -> "category.bandits_quirk_lib.lowend";
                    case QUIRK -> "category.bandits_quirk_lib.quirk";
                };
                return Component.translatable(key);
            }
        }
        return null;
    }
}


