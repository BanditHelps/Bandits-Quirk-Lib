package com.github.b4ndithelps.forge.client.refprog;

import com.github.b4ndithelps.forge.blocks.GeneSlicerBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import com.github.b4ndithelps.forge.client.BioTerminalRefScreen;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side slicer program for the ref screen.
 * Left: list of connected Gene Slicers.
 * Right: genes in the selected slicer's input sequenced sample with selection and a start action.
 */
public class RefSlicerProgram {
    private final BioTerminalRefScreen screen;
    private final BlockPos terminalPos;

    private final List<GeneSlicerBlockEntity> slicers = new ArrayList<>();
    private int selectedSlicerIndex = 0;

    private enum Pane { LEFT, RIGHT }
    private Pane activePane = Pane.LEFT;

    // Right pane state
    private final List<String> geneLabels = new ArrayList<>();
    private int rightCursorIndex = 0; // 0..genes + (start+back rows)
    private final Set<Integer> selectedGeneIndices = new HashSet<>(); // 0-based indices in gene list

    public RefSlicerProgram(BioTerminalRefScreen screen, BlockPos terminalPos) {
        this.screen = screen;
        this.terminalPos = terminalPos;
        refresh();
    }

    public void refresh() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        slicers.clear();
        var connected = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof GeneSlicerBlockEntity);
        for (var be : connected) if (be instanceof GeneSlicerBlockEntity g) slicers.add(g);
        if (selectedSlicerIndex >= slicers.size()) selectedSlicerIndex = Math.max(0, slicers.size() - 1);
        refreshRightPane();
    }

    private void refreshRightPane() {
        // Preserve current selection and cursor where possible
        int prevCursor = rightCursorIndex;
        java.util.Set<Integer> prevSelected = new java.util.HashSet<>(selectedGeneIndices);

        java.util.List<String> newLabels = new java.util.ArrayList<>();
        if (slicers.isEmpty()) {
            geneLabels.clear();
            selectedGeneIndices.clear();
            rightCursorIndex = Math.min(prevCursor, Math.max(0, getRightRowCount() - 1));
            return;
        }
        var slicer = slicers.get(selectedSlicerIndex);
        ItemStack input = slicer.getItem(GeneSlicerBlockEntity.SLOT_INPUT);
        if (!input.isEmpty() && input.getItem() == ModItems.SEQUENCED_SAMPLE.get()) {
            CompoundTag tag = input.getTag();
            if (tag != null && tag.contains("genes", 9)) {
                var genes = tag.getList("genes", 10);
                for (int i = 0; i < genes.size(); i++) {
                    CompoundTag g = genes.getCompound(i);
                    String label = g.getString("name");
                    if (label == null || label.isEmpty()) label = g.getString("id");
                    if (label == null || label.isEmpty()) label = "gene_" + Integer.toString(i + 1);
                    newLabels.add(label);
                }
            }
        }
        // Swap in new labels
        geneLabels.clear();
        geneLabels.addAll(newLabels);

        // Restore selection within bounds
        selectedGeneIndices.clear();
        for (Integer idx : prevSelected) {
            if (idx != null && idx >= 0 && idx < geneLabels.size()) selectedGeneIndices.add(idx);
        }
        // Restore cursor; clamp if necessary
        int rows = getRightRowCount();
        rightCursorIndex = Math.min(prevCursor, Math.max(0, rows - 1));
    }

    public void moveSelection(int delta) {
        if (activePane == Pane.LEFT) {
            if (slicers.isEmpty()) return;
            selectedSlicerIndex = (selectedSlicerIndex + delta + slicers.size()) % slicers.size();
            // Changing slicer resets right pane selection
            selectedGeneIndices.clear();
            rightCursorIndex = 0;
            refreshRightPane();
        } else {
            int rows = getRightRowCount();
            if (rows <= 0) return;
            rightCursorIndex = (rightCursorIndex + delta + rows) % rows;
        }
    }

    private int getRightRowCount() {
        // genes + [Start] + [Back]
        return geneLabels.size() + 2;
    }

    public void startSelected() {
        // Enter key behaviour per pane
        if (activePane == Pane.LEFT) {
            // Enter the gene selection pane
            activePane = Pane.RIGHT;
            rightCursorIndex = 0;
            return;
        }
        // RIGHT pane
        int genes = geneLabels.size();
        if (rightCursorIndex < genes) {
            // Toggle selection
            if (selectedGeneIndices.contains(rightCursorIndex)) selectedGeneIndices.remove(rightCursorIndex);
            else selectedGeneIndices.add(rightCursorIndex);
            return;
        }
        if (rightCursorIndex == genes) {
            // Start slicing if there is at least one selection
            if (selectedGeneIndices.isEmpty() || slicers.isEmpty()) return;
            var target = slicers.get(selectedSlicerIndex);
            // Build comma-separated 0-based indices
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Integer idx : selectedGeneIndices) {
                if (!first) sb.append(',');
                sb.append(idx);
                first = false;
            }
            String action = "slice.start:" + sb;
            BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, action, target.getBlockPos()));
            // Optimistically clear selection and refresh UI (server will update inventories)
            selectedGeneIndices.clear();
            refreshRightPane();
            return;
        }
        if (rightCursorIndex == genes + 1) {
            // Back to left pane
            activePane = Pane.LEFT;
            return;
        }
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, Font font) {
        int leftW = (int)Math.floor(w * 0.42);
        int rightW = w - leftW - 2;
        int leftX = x;
        int rightX = x + leftW + 2;
        int curY = y;

        // Header
        g.drawString(font, Component.literal("[SLICE]"), leftX, curY, 0x55FF55, false);
        curY += font.lineHeight + 2;

        // LEFT: slicers list
        if (slicers.isEmpty()) {
            g.drawString(font, Component.literal("No connected slicers."), leftX, curY, 0xAAAAAA, false);
        } else {
            for (int i = 0; i < slicers.size(); i++) {
                boolean sel = (activePane == Pane.LEFT && i == selectedSlicerIndex);
                var slicer = slicers.get(i);
                int pct = slicer.getMaxProgress() == 0 ? 0 : (slicer.getProgress() * 100 / slicer.getMaxProgress());
                String status = slicer.isRunning() ? ("[" + pct + "%]") : "[IDLE]";
                String line = (sel ? ">> " : "   ") + "Slicer " + (i + 1) + " " + status;
                g.drawString(font, Component.literal(line), leftX, curY, sel ? 0x55FF55 : 0xFFFFFF, false);
                curY += font.lineHeight;
                if (curY > y + h - font.lineHeight) break;
            }
        }

        // Vertical separator
        int sepX = rightX - 2;
        g.fill(sepX, y, sepX + 1, y + h, 0x33555555);

        // RIGHT: gene list and actions
        int ry = y;
        g.drawString(font, Component.literal("Genes"), rightX, ry, 0xA0FFFFFF, false);
        ry += font.lineHeight + 2;

        if (geneLabels.isEmpty()) {
            // Wrap instruction text within right pane width
            var wrapped = font.split(Component.literal("Insert a sequenced sample in selected slicer."), rightW);
            for (int i = 0; i < wrapped.size(); i++) {
                if (ry > y + h - font.lineHeight) break;
                g.drawString(font, wrapped.get(i), rightX, ry, 0xAAAAAA, false);
                ry += font.lineHeight;
            }
            return;
        }

        int genes = geneLabels.size();
        for (int i = 0; i < genes; i++) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == i);
            boolean picked = selectedGeneIndices.contains(i);
            String prefix = cursor ? ">> " : "   ";
            String mark = picked ? "[*] " : "[ ] ";
            String label = prefix + mark + (i + 1) + ") " + compact(geneLabels.get(i));
            g.drawString(font, Component.literal(label), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
            ry += font.lineHeight;
            if (ry > y + h - font.lineHeight) break;
        }

        // Action rows
        if (ry <= y + h - font.lineHeight) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == genes);
            String action = ">> Start slicing (" + selectedGeneIndices.size() + ")";
            String idle = "   Start slicing (" + selectedGeneIndices.size() + ")";
            g.drawString(font, Component.literal(cursor ? action : idle), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
            ry += font.lineHeight;
        }
        if (ry <= y + h - font.lineHeight) {
            boolean cursor = (activePane == Pane.RIGHT && rightCursorIndex == genes + 1);
            String back = cursor ? ">> Back" : "   Back";
            g.drawString(font, Component.literal(back), rightX, ry, cursor ? 0x55FF55 : 0xFFFFFF, false);
        }
    }

    private static String compact(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[^A-Za-z0-9_()]+", "");
        if (t.length() > 16) return t.substring(0, 16);
        return t;
    }
}


