package com.github.b4ndithelps.forge.client.programs;

import com.github.b4ndithelps.forge.blocks.BioPrinterBlockEntity;
import com.github.b4ndithelps.forge.blocks.SampleRefrigeratorBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import com.github.b4ndithelps.forge.client.BioTerminalScreen;
import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side printer program for the ref screen.
 * Left: list of connected Bio Printers.
 * Right: list of available gene vials from connected refrigerators with selection and a start action.
 */
public class PrinterProgram {
    private final BioTerminalScreen screen;
    private final BlockPos terminalPos;

    private final List<BioPrinterBlockEntity> printers = new ArrayList<>();
    private int selectedPrinterIndex = 0;

    private static final class VialEntry {
        final String label;
        final int fridgeIndex;
        final int slotIndex;
        VialEntry(String label, int fridgeIndex, int slotIndex) {
            this.label = label;
            this.fridgeIndex = fridgeIndex;
            this.slotIndex = slotIndex;
        }
    }
    private final List<VialEntry> vialEntries = new ArrayList<>();
    private final Set<Integer> selectedVialIndices = new HashSet<>();
    private int rightCursorIndex = 0; // rows: entries + start + back

    private enum Pane { LEFT, RIGHT }
    private Pane activePane = Pane.LEFT;

    private long lastCatalogSyncRequestGameTime = Long.MIN_VALUE;

    public PrinterProgram(BioTerminalScreen screen, BlockPos terminalPos) {
        this.screen = screen;
        this.terminalPos = terminalPos;
        refresh();
    }

    public void refresh() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        printers.clear();
        var connected = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof BioPrinterBlockEntity);
        for (var be : connected) if (be instanceof BioPrinterBlockEntity p) printers.add(p);
        if (selectedPrinterIndex >= printers.size()) selectedPrinterIndex = Math.max(0, printers.size() - 1);

        rebuildVialEntries();

        long gt = mc.level.getGameTime();
        if (gt - lastCatalogSyncRequestGameTime >= 20L) {
            lastCatalogSyncRequestGameTime = gt;
            BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, "catalog.sync", null));
        }
    }

    private void rebuildVialEntries() {
        int prevCursor = rightCursorIndex;
        java.util.Set<Integer> prevSelected = new java.util.HashSet<>(selectedVialIndices);
        List<VialEntry> list = new ArrayList<>();
        var cached = ClientCatalogCache.get(terminalPos);
        if (!cached.isEmpty()) {
            for (var dto : cached) if ("VIAL".equals(dto.type)) list.add(new VialEntry(dto.label, dto.sourceIndex, dto.slotIndex));
        } else {
            var mc = Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                List<SampleRefrigeratorBlockEntity> fridges = new ArrayList<>();
                var connectedFridges = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof SampleRefrigeratorBlockEntity);
                for (var be : connectedFridges) if (be instanceof SampleRefrigeratorBlockEntity f) fridges.add(f);
                for (int f = 0; f < fridges.size(); f++) {
                    var fridge = fridges.get(f);
                    for (int s = 0; s < SampleRefrigeratorBlockEntity.SLOT_COUNT; s++) {
                        ItemStack st = fridge.getItem(s);
                        if (st == null || st.isEmpty()) continue;
                        if (!isGeneVial(st)) continue;
                        list.add(new VialEntry(labelFromVial(st), f, s));
                    }
                }
            }
        }
        vialEntries.clear();
        vialEntries.addAll(list);
        selectedVialIndices.clear();
        for (Integer idx : prevSelected) if (idx != null && idx >= 0 && idx < vialEntries.size()) selectedVialIndices.add(idx);
        int rows = getRightRowCount();
        rightCursorIndex = Math.min(prevCursor, Math.max(0, rows - 1));
    }

    public void moveSelection(int delta) {
        if (activePane == Pane.LEFT) {
            if (printers.isEmpty()) return;
            selectedPrinterIndex = (selectedPrinterIndex + delta + printers.size()) % printers.size();
            selectedVialIndices.clear();
            rightCursorIndex = 0;
        } else {
            int rows = getRightRowCount();
            if (rows <= 0) return;
            rightCursorIndex = (rightCursorIndex + delta + rows) % rows;
        }
    }

    private int getRightRowCount() { return vialEntries.size() + 2; }

    public void startSelected() {
        if (activePane == Pane.LEFT) {
            if (printers.isEmpty()) return;
            activePane = Pane.RIGHT;
            rightCursorIndex = 0;
            return;
        }
        int entries = vialEntries.size();
        if (rightCursorIndex < entries) {
            int maxPick = BioPrinterBlockEntity.SLOT_INPUT_COUNT;
            if (selectedVialIndices.contains(rightCursorIndex)) selectedVialIndices.remove(rightCursorIndex);
            else if (selectedVialIndices.size() < maxPick) selectedVialIndices.add(rightCursorIndex);
            return;
        }
        if (rightCursorIndex == entries) {
            if (printers.isEmpty() || selectedVialIndices.isEmpty()) return;
            var target = printers.get(selectedPrinterIndex);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Integer idx : selectedVialIndices) {
                if (idx == null || idx < 0 || idx >= vialEntries.size()) continue;
                var v = vialEntries.get(idx);
                if (!first) sb.append(',');
                sb.append(v.fridgeIndex).append('-').append(v.slotIndex);
                first = false;
            }
            if (sb.length() == 0) return;
            String action = "print.start:" + sb;
            BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, action, target.getBlockPos()));
            // Request updated catalog and optimistically remove the used vials
            BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, "catalog.sync", null));
            java.util.ArrayList<Integer> sorted = new java.util.ArrayList<>(selectedVialIndices);
            sorted.sort(java.util.Comparator.reverseOrder());
            for (int idx : sorted) if (idx >= 0 && idx < vialEntries.size()) vialEntries.remove(idx);
            selectedVialIndices.clear();
            rightCursorIndex = Math.min(rightCursorIndex, Math.max(0, getRightRowCount() - 1));
            return;
        }
        if (rightCursorIndex == entries + 1) { activePane = Pane.LEFT; }
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, Font font) {
        int leftW = (int)Math.floor(w * 0.48);
        int rightW = w - leftW - 2;
        int leftX = x;
        int rightX = x + leftW + 2;
        int curY = y;

        g.drawString(font, Component.literal("[PRINT]"), leftX, curY, 0x55FF55, false);
        curY += font.lineHeight + 2;

        // Left list: printers
        if (printers.isEmpty()) {
            g.drawString(font, Component.literal("No detected printers."), leftX, curY, 0xAAAAAA, false);
        } else {
            for (int i = 0; i < printers.size(); i++) {
                boolean sel = (activePane == Pane.LEFT && i == selectedPrinterIndex);
                String line = (sel ? ">> " : "   ") + "Printer " + (i + 1);
                g.drawString(font, Component.literal(line), leftX, curY, sel ? 0x55FF55 : 0xFFFFFF, false);
                curY += font.lineHeight;
                if (curY > y + h - font.lineHeight) break;
            }
        }

        int sepX = rightX - 2; g.fill(sepX, y, sepX + 1, y + h, 0x33555555);

        int ry = y;
        g.drawString(font, Component.literal("Available Vials"), rightX, ry, 0xA0FFFFFF, false);
        ry += font.lineHeight + 2;

        // Feedback (success/failure)
        if (!printers.isEmpty() && selectedPrinterIndex >= 0 && selectedPrinterIndex < printers.size()) {
            var printer = printers.get(selectedPrinterIndex);
            var entry = com.github.b4ndithelps.forge.client.programs.ClientPrinterStateCache.get(printer.getBlockPos());
            if (entry != null && entry.message != null && !entry.message.isEmpty()) {
                int color = entry.success ? 0x55FF55 : 0xFFAA00;
                g.drawString(font, Component.literal(entry.message), rightX, ry, color, false);
                ry += font.lineHeight + 2;
            }
        }

        if (vialEntries.isEmpty()) {
            var wrapped = font.split(Component.literal("Insert gene vials into connected refrigerators."), rightW);
            for (int i = 0; i < wrapped.size(); i++) {
                if (ry > y + h - font.lineHeight) break;
                g.drawString(font, wrapped.get(i), rightX, ry, 0xAAAAAA, false);
                ry += font.lineHeight;
            }
            return;
        }

        int cap = BioPrinterBlockEntity.SLOT_INPUT_COUNT;
        for (int i = 0; i < vialEntries.size(); i++) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == i);
            boolean picked = selectedVialIndices.contains(i);
            String prefix = cursor ? ">> " : "   ";
            String mark = picked ? "[*] " : "[ ] ";
            String label = prefix + mark + (i + 1) + ") " + compact(vialEntries.get(i).label);
            g.drawString(font, Component.literal(label), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
            ry += font.lineHeight;
            if (ry > y + h - font.lineHeight) break;
        }

        if (ry <= y + h - font.lineHeight) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == vialEntries.size());
            String action = ">> Start printing (" + selectedVialIndices.size() + "/" + cap + ")";
            String idle = "   Start printing (" + selectedVialIndices.size() + "/" + cap + ")";
            g.drawString(font, Component.literal(cursor ? action : idle), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
            ry += font.lineHeight;
        }
        if (ry <= y + h - font.lineHeight) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == vialEntries.size() + 1);
            String back = cursor ? ">> Back" : "   Back";
            g.drawString(font, Component.literal(back), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
        }
    }

    private static boolean isGeneVial(ItemStack stack) {
        var it = stack.getItem();
        return it instanceof GeneVialItem
                || it == ModItems.GENE_VIAL_COSMETIC.get()
                || it == ModItems.GENE_VIAL_RESISTANCE.get()
                || it == ModItems.GENE_VIAL_BUILDER.get()
                || it == ModItems.GENE_VIAL_QUIRK.get();
    }

    private static String labelFromVial(ItemStack vial) {
        if (vial == null || vial.isEmpty()) return "";
        CompoundTag tag = vial.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            CompoundTag g = tag.getCompound("gene");
            String name = g.contains("name", 8) ? g.getString("name") : null;
            String id = g.contains("id", 8) ? g.getString("id") : null;
            if (name != null && !name.isEmpty()) return name;
            if (id != null && !id.isEmpty()) return id;
        }
        return vial.getItem().getDescription().getString();
    }

    private static String compact(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[^A-Za-z0-9_()]+", "");
        if (t.length() > 16) return t.substring(0, 16);
        return t;
    }
}


