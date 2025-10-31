package com.github.b4ndithelps.forge.client.refprog;

import com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import com.github.b4ndithelps.forge.client.BioTerminalRefScreen;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.RefProgramActionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side analyze program for the ref screen: left-side list of connected sequencers,
 * right-side DNA visualization for the selected sequencer (if analyzed output is present).
 */
public class RefAnalyzeProgram {
    private final BioTerminalRefScreen screen;
    private final BlockPos terminalPos;

    private final List<GeneSequencerBlockEntity> sequencers = new ArrayList<>();
    private int selectedIndex = 0;
    private long lastSyncRequestGameTime = Long.MIN_VALUE;
    private long lastCatalogSyncGameTime = Long.MIN_VALUE;

    public RefAnalyzeProgram(BioTerminalRefScreen screen, BlockPos terminalPos) {
        this.screen = screen;
        this.terminalPos = terminalPos;
        refreshSequencers();
        requestStatusSync();
    }

    public void refreshSequencers() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        sequencers.clear();
        var connected = CableNetworkUtil.findConnected(mc.level, terminalPos, be -> be instanceof GeneSequencerBlockEntity);
        for (var be : connected) if (be instanceof GeneSequencerBlockEntity g) sequencers.add(g);
        if (selectedIndex >= sequencers.size()) selectedIndex = Math.max(0, sequencers.size() - 1);
        // Periodically request authoritative status from server so UI reflects pre-existing states
        long gt = mc.level.getGameTime();
        if (gt - lastSyncRequestGameTime >= 20L) {
            requestStatusSync();
        }
    }

    public void moveSelection(int delta) {
        if (sequencers.isEmpty()) return;
        selectedIndex = (selectedIndex + delta + sequencers.size()) % sequencers.size();
    }

    public void startSelected() {
        if (sequencers.isEmpty()) return;
        var target = sequencers.get(selectedIndex);
        // Prevent starting if already running
        var cache = com.github.b4ndithelps.forge.client.refprog.ClientSequencerStatusCache.get(target.getBlockPos());
        boolean runningNow = (cache != null) ? cache.running : target.isRunning();
        if (runningNow) return;
        // Prevent starting without a sample in input
        var in = target.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
        if (in.isEmpty() || in.getItem() != com.github.b4ndithelps.forge.item.ModItems.TISSUE_SAMPLE.get()) return;
        // Prevent starting if output is occupied (already analyzed result present)
        var out = target.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
        if (!out.isEmpty()) return;
        BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, "analyze.start", target.getBlockPos()));
    }

    public void render(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h, net.minecraft.client.gui.Font font) {
        // Periodically refresh connections and request sync so UI reflects latest server state
        refreshSequencers();
        // Split area: 60% left list, 40% right DNA panel
        // Split area: left list and right DNA panel
        int leftW = (int)Math.floor(w * 0.58);
        int rightW = w - leftW - 2;
        int listX = x;
        int listY = y;
        int dnaX = x + leftW + 2;
        int dnaY = y;

        // Header
        g.drawString(font, Component.literal("[ANALYZE]"), listX, listY, 0x55FF55, false);
        int curY = listY + font.lineHeight + 2;

        if (sequencers.isEmpty()) {
            g.drawString(font, Component.literal("No connected sequencers."), listX, curY, 0xAAAAAA, false);
            return;
        }

        // List items with explicit running/analyzed indicators from client cache fallback
        for (int i = 0; i < sequencers.size(); i++) {
            var seq = sequencers.get(i);
            boolean sel = (i == selectedIndex);
            String name = String.format("Sequencer %d", i + 1);
            String status;
            var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
            boolean analyzed = !out.isEmpty() && out.getTag() != null;
            // Check cache override for both running and analyzed
            var cache = com.github.b4ndithelps.forge.client.refprog.ClientSequencerStatusCache.get(seq.getBlockPos());
            boolean runningNow = (cache != null) ? cache.running : seq.isRunning();
            if (cache != null) analyzed = analyzed || cache.analyzed;
            if (runningNow) status = "[RUNNING]";
            else if (analyzed) status = "[ANALYZED]";
            else status = "[READY]";
            String line = (sel ? ">> " : "   ") + name + " " + status;
            int color = sel ? 0x55FF55 : 0xFFFFFF;
            g.drawString(font, Component.literal(line), listX, curY, color, false);
            curY += font.lineHeight;
            if (curY > y + h - font.lineHeight) break;
        }

        // DNA panel on the right side
        if (!sequencers.isEmpty()) {
            // Vertical separator between list and DNA
            int sepX = dnaX - 2;
            g.fill(sepX, y, sepX + 1, y + h, 0x33555555);
            drawDnaPanel(g, sequencers.get(selectedIndex), dnaX, dnaY, rightW, h, font);
        }
    }

    private void drawDnaPanel(net.minecraft.client.gui.GuiGraphics g, GeneSequencerBlockEntity seq, int x, int y, int w, int h, net.minecraft.client.gui.Font font) {
        int curY = y;
        g.drawString(font, Component.literal("DNA"), x, curY, 0xA0FFFFFF, false);
        curY += font.lineHeight + 2;

        // Two-frame animation with fixed-width strings across all rows to avoid jitter
        long t = (Minecraft.getInstance().level == null) ? 0L : Minecraft.getInstance().level.getGameTime();
        boolean phase = ((t / 8L) % 2L) == 0L;
        String[] dna = phase ?
                new String[]{
                        " A-=-T",
                        " \\    /",
                        " T-=-A",
                        " /    \\",
                        " C-=-G",
                        " \\    /",
                        " G-=-C",
                        " /    \\",
                        " T-=-A",
                        " \\    /",
                        " A-=-T"
                }
                :
                new String[]{
                        " A===T",
                        " /    \\",
                        " T===A",
                        " \\    /",
                        " C===G",
                        " /    \\",
                        " G===C",
                        " \\    /",
                        " T===A",
                        " /    \\",
                        " A===T"
                };

        // Try to show up to 4 gene labels if analyzed; prefer live BE output, fallback to catalog cache
        String[] left = new String[]{"", "", "", "", "", ""};
        String[] right = new String[]{"", "", "", "", "", ""};
        boolean haveLabels = false;
        var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
        if (!out.isEmpty() && out.getTag() != null) {
            var tag = out.getTag();
            if (tag.contains("genes", 9)) {
                var list = tag.getList("genes", 10);
                int max = Math.min(4, list.size());
                for (int i = 0; i < max; i++) {
                    var gTag = list.getCompound(i);
                    String label = gTag.getString("name");
                    if (label == null || label.isEmpty()) label = gTag.getString("id");
                    left[i] = compact(label);
                }
                haveLabels = true;
            }
        }
        if (!haveLabels) {
            // If server told us it's analyzed, request a catalog sync and read labels from catalog cache
            var cache = com.github.b4ndithelps.forge.client.refprog.ClientSequencerStatusCache.get(seq.getBlockPos());
            var mcLocal = Minecraft.getInstance();
            long gt = (mcLocal.level == null) ? 0L : mcLocal.level.getGameTime();
            if (cache != null && cache.analyzed && gt - lastCatalogSyncGameTime >= 10L) {
                lastCatalogSyncGameTime = gt;
                BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, "catalog.sync", null));
            }
            var entries = com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache.get(terminalPos);
            if (entries != null && !entries.isEmpty()) {
                int placed = 0;
                for (var e : entries) {
                    if ("SEQUENCED_GENE".equals(e.type) && e.sourcePos != null && e.sourcePos.equals(seq.getBlockPos())) {
                        if (placed < 4) left[placed++] = compact(e.label);
                        if (placed >= 4) break;
                    }
                }
            }
        }

        int[] leftRows = new int[]{0, 2, 4, 6, 8, 10};
        int[] rightRows = new int[]{}; // no right placements
        int labelWidth = 10;
        // Reserve fixed pixel width for label column so DNA stays aligned regardless of label glyph widths
        int reservedLabelPx = font.width("W".repeat(labelWidth));
        int labelGapPx = 4;

        for (int row = 0; row < dna.length; row++) {
            String leftLab = "";
            String rightLab = "";
            for (int idx = 0; idx < leftRows.length; idx++) if (leftRows[idx] == row) { leftLab = left[idx] == null ? "" : left[idx]; break; }
            for (int idx = 0; idx < rightRows.length; idx++) if (rightRows[idx] == row) { rightLab = right[idx] == null ? "" : right[idx]; break; }

            // Draw left label at fixed column, then DNA starting at a constant pixel X
            if (!leftLab.isEmpty()) {
                // Compact already limits char count; draw label dimmer
                g.drawString(font, Component.literal(leftLab), x, curY, 0xA0FFFFFF, false);
            }
            int dnaXStart = x + reservedLabelPx + labelGapPx;
            g.drawString(font, Component.literal(dna[row]), dnaXStart, curY, 0xFFFFFF, false);
            curY += font.lineHeight;
            if (curY > y + h - font.lineHeight) break;
        }

        // Optional: show a tiny running indicator
        {
            var cache = com.github.b4ndithelps.forge.client.refprog.ClientSequencerStatusCache.get(seq.getBlockPos());
            boolean runningNow = (cache != null) ? cache.running : seq.isRunning();
            if (runningNow) {
            String running = "[RUNNING]";
            reservedLabelPx = font.width("W".repeat(labelWidth));
            int dnaXStart = x + reservedLabelPx + labelGapPx;
            g.drawString(font, Component.literal(running), dnaXStart, Math.min(curY + 2, y + h - font.lineHeight), 0x55FF55, false);
            }
        }
    }

    private void requestStatusSync() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        lastSyncRequestGameTime = mc.level.getGameTime();
        BQLNetwork.CHANNEL.sendToServer(new RefProgramActionC2SPacket(terminalPos, "analyze.sync", null));
    }

    private static String compact(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[^A-Za-z0-9_()]+", "");
        if (t.length() > 10) return t.substring(0, 10);
        return t;
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder b = new StringBuilder(s);
        while (b.length() < width) b.append(' ');
        return b.toString();
    }

    private static String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder b = new StringBuilder();
        while (b.length() + s.length() < width) b.append(' ');
        b.append(s);
        return b.toString();
    }

    private static String spaces(int n) { return " ".repeat(Math.max(0, n)); }

    public int getSelectedIndex() { return selectedIndex; }
    public int size() { return sequencers.size(); }
}


