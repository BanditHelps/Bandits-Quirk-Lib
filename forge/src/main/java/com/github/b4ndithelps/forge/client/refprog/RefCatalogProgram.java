package com.github.b4ndithelps.forge.client.refprog;

import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import com.github.b4ndithelps.forge.client.BioTerminalRefScreen;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side catalog program for the ref screen.
 * Left: list all gene vials in connected sample containers and sequenced samples.
 * Right: details of the currently selected entry (all fields shown as unknown for now).
 */
public class RefCatalogProgram {
    private final BioTerminalRefScreen screen;
    private final BlockPos terminalPos;

    private final List<Entry> entries = new ArrayList<>();
    private int selectedIndex = 0;

    private enum EntryType { VIAL, SEQUENCED_SAMPLE, SECTION }

    private static final class Entry {
        final EntryType type;
        final String label; // display label in list
        final ItemStack stack; // optional for VIAL/SAMPLE
        final int sourceIndex; // fridge or sequencer index (0-based), -1 if N/A
        final int slotIndex;   // slot within fridge if applicable, -1 otherwise

        Entry(EntryType type, String label, ItemStack stack, int sourceIndex, int slotIndex) {
            this.type = type;
            this.label = label;
            this.stack = stack;
            this.sourceIndex = sourceIndex;
            this.slotIndex = slotIndex;
        }
    }

    public RefCatalogProgram(BioTerminalRefScreen screen, BlockPos terminalPos) {
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
                    default -> EntryType.SECTION;
                };
                entries.add(new Entry(t, dto.label, ItemStack.EMPTY, dto.sourceIndex, dto.slotIndex));
            }
        } else {
            // Fallback to local scan (may miss inventory data without block entity sync)
            List<SampleRefrigeratorBlockEntity> fridges = new ArrayList<>();
            var connectedFridges = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof SampleRefrigeratorBlockEntity);
            for (var be : connectedFridges) if (be instanceof SampleRefrigeratorBlockEntity f) fridges.add(f);

            if (!fridges.isEmpty()) {
                entries.add(new Entry(EntryType.SECTION, "[VIALS]", ItemStack.EMPTY, -1, -1));
                for (int f = 0; f < fridges.size(); f++) {
                    var fridge = fridges.get(f);
                    for (int s = 0; s < SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                        var st = fridge.getItem(s);
                        if (st == null || st.isEmpty()) continue;
                        if (!isGeneVial(st)) continue;
                        String name = getVialGeneLabel(st);
                        String line = String.format("%s", name);
                        entries.add(new Entry(EntryType.VIAL, line, st.copy(), f, s));
                    }
                }
            }

            List<GeneSequencerBlockEntity> sequencers = new ArrayList<>();
            var connectedSeq = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof GeneSequencerBlockEntity);
            for (var be : connectedSeq) if (be instanceof GeneSequencerBlockEntity g) sequencers.add(g);

            if (!sequencers.isEmpty()) {
                entries.add(new Entry(EntryType.SECTION, "[SAMPLES]", ItemStack.EMPTY, -1, -1));
                for (int i = 0; i < sequencers.size(); i++) {
                    var seq = sequencers.get(i);
                    var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                    if (!out.isEmpty()) {
                        String line = String.format("Sequenced Sample %d", i + 1);
                        entries.add(new Entry(EntryType.SEQUENCED_SAMPLE, line, out.copy(), i, -1));
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
        // No action for now; reserved for future interactions
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, Font font) {
        int leftW = (int)Math.floor(w * 0.58);
        int rightW = w - leftW - 2;
        int listX = x;
        int listY = y;
        int infoX = x + leftW + 2;
        int infoY = y;

        // Header
        g.drawString(font, Component.literal("[CATALOG]"), listX, listY, 0x55FF55, false);
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
                    g.drawString(font, Component.literal(prefix + e.label), listX, curY, color, false);
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

        // All information is unknown for now, per requirements
        g.drawString(font, Component.literal("Name: unknown"), x, curY, 0xFFFFFF, false); curY += font.lineHeight;
        g.drawString(font, Component.literal("Gene ID: unknown"), x, curY, 0xAAAAAA, false); curY += font.lineHeight;
        g.drawString(font, Component.literal("Category: Unknown"), x, curY, 0xAAAAAA, false); curY += font.lineHeight;
        g.drawString(font, Component.literal("Quality: unknown"), x, curY, 0xFFFF55, false); curY += font.lineHeight;
        curY += 2;

        // Show source location basics for context
        if (sel.type == EntryType.VIAL) {
            String src = String.format("Source: Fridge %d, Slot %d", sel.sourceIndex + 1, sel.slotIndex + 1);
            g.drawString(font, Component.literal(src), x, curY, 0xAAAAAA, false);
        } else if (sel.type == EntryType.SEQUENCED_SAMPLE) {
            String src = String.format("Source: Sequencer %d", sel.sourceIndex + 1);
            g.drawString(font, Component.literal(src), x, curY, 0xAAAAAA, false);
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

    public int getSelectedIndex() { return selectedIndex; }
    public int size() { return entries.size(); }

    private void requestSync() {
        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.sendToServer(
                new com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket(terminalPos, "catalog.sync", null)
        );
    }
}


