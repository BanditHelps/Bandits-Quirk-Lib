package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Random;

/**
 * Analyze program: lists adjacent sequencers, allows start N, status [N], readout N, back, exit.
 */
@SuppressWarnings("removal")
public class AnalyzeProgram extends AbstractConsoleProgram {
    private final List<GeneSequencerBlockEntity> cachedSequencers = new ArrayList<>();
    private String statusLine = ""; // shown at top of program screen
    private enum ViewMode { LIST, READOUT }
    private ViewMode viewMode = ViewMode.LIST;
    private int readoutIndex = -1;

    // New list-view model, similar to python: combined list of samples
    private enum SampleStatus { UNANALYZED, ANALYZED }
    private static final class SampleEntry {
        final int sequencerIndex;
        final SampleStatus status;
        final CompoundTag tag; // sample or readout tag for DNA display
        final String name;
        SampleEntry(int sequencerIndex, SampleStatus status, CompoundTag tag, String name) {
            this.sequencerIndex = sequencerIndex; this.status = status; this.tag = tag; this.name = name;
        }
    }
    private final List<SampleEntry> entries = new ArrayList<>();
    private int listSelectedIndex = 0;

    // Selection state for READOUT (details) view
    private int selectedSlotIndex = -1; // 0..(visibleSlots-1) over actual gene entries
    private List<Integer> visibleSlotRowIndices = new ArrayList<>(); // rows that have a gene label
    // Animation state for READOUT view (character-by-character)
    private boolean readoutAnimating = false;
    private List<String> readoutAnimLines = new ArrayList<>();
    private int readoutAnimLineIndex = 0;
    private int readoutAnimCharIndex = 0;
    private int readoutAnimSpeed = 80; // chars per tick (even faster)
    // Animation state: track which unique samples have already animated
    private final java.util.Set<String> animatedReadoutKeys = new java.util.HashSet<>();
    private String currentReadoutKey = "";
    private boolean inDetailsView = false;
    // Current readout data (mapped 1:1 with visible slots order)
    private final List<String> currentGeneIds = new ArrayList<>();
    private final List<Integer> currentGeneQualities = new ArrayList<>();
    private final List<String> currentGeneLabels = new ArrayList<>(); // cryptic names used in readout (no translation)
    // Rows (0..11) used for each gene label in current readout; parallels labels order
    private final List<Integer> currentGeneRows = new ArrayList<>();
    private final List<Integer> displayGeneIndices = new ArrayList<>();

    @Override
    public String getName() { return "analyze"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
        listSelectedIndex = Math.min(listSelectedIndex, Math.max(0, entries.size() - 1));
        renderList(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        cachedSequencers.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();
        java.util.Set<net.minecraft.world.level.block.entity.BlockEntity> connected = CableNetworkUtil.findConnected(level, pos, be -> be instanceof GeneSequencerBlockEntity);
        for (var be : connected) if (be instanceof GeneSequencerBlockEntity seq) cachedSequencers.add(seq);
        buildSampleEntries();
        viewMode = ViewMode.LIST;
        readoutIndex = -1;
    }

    private void renderList(ConsoleContext ctx) {
        ProgramScreenBuilder b = screen()
                .header("Analyze - Genetic Sample Processor")
                .separator();

        if (!statusLine.isEmpty()) {
            b.line(statusLine);
            b.blank();
        }

        List<String> sampleLines = new ArrayList<>();
        sampleLines.add(ConsoleText.color("SAMPLES:", ConsoleText.ColorTag.AQUA));
        sampleLines.add("");
        if (entries.isEmpty()) {
            sampleLines.add("  NO SAMPLES AVAILABLE");
            sampleLines.add("  INSERT SAMPLES TO BEGIN ANALYSIS");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                SampleEntry e = entries.get(i);
                String prefix = (i == listSelectedIndex) ? ">> " : "   ";
                String statusRaw = e.status == SampleStatus.UNANALYZED ? "UNANALYZED" : "ANALYZED";
                String statusCol = ConsoleText.color(statusRaw, e.status == SampleStatus.UNANALYZED ? ConsoleText.ColorTag.YELLOW : ConsoleText.ColorTag.AQUA);
                sampleLines.add(prefix + String.format("%-15s ", e.name) + " [" + statusCol + "]");
            }
        }
        while (sampleLines.size() < 15) sampleLines.add("");

        // Right column DNA visualization if current selection is analyzed
        List<String> dnaLines = new ArrayList<>();
        SampleEntry current = (entries.isEmpty() || listSelectedIndex < 0 || listSelectedIndex >= entries.size()) ? null : entries.get(listSelectedIndex);
        if (current != null && current.status == SampleStatus.ANALYZED) {
            dnaLines = buildDnaReadoutPlain(current.tag);
        } else {
            for (int i = 0; i < 15; i++) dnaLines.add("");
        }

        int rows = Math.max(sampleLines.size(), dnaLines.size());
        for (int i = 0; i < rows; i++) {
            String left = i < sampleLines.size() ? sampleLines.get(i) : "";
            String right = i < dnaLines.size() ? dnaLines.get(i) : "";
            // Respect color/alignment tags on left by not stripping tags: use twoColumn for spacing
            b.line(left + "  " + right);
        }

        b.blank();
        // Processing bar if selected item belongs to a running sequencer
        if (current != null) {
            var seq = cachedSequencers.get(current.sequencerIndex);
            if (seq.isRunning()) {
                int pct = seq.getMaxProgress() == 0 ? 0 : (seq.getProgress() * 100 / seq.getMaxProgress());
                b.line("Processing sample...", ConsoleText.ColorTag.AQUA);
                b.progressBar(pct, 30);
            } else if (current.status == SampleStatus.UNANALYZED) {
                b.line("Ready.", ConsoleText.ColorTag.GRAY);
            }
        }

        b.blank();
        b.line("Controls:", ConsoleText.ColorTag.GRAY);
        if (entries.isEmpty()) {
            b.line("  A/D - Change Tab    Q/Ctrl+C - Exit", ConsoleText.ColorTag.GRAY);
        } else {
            b.line("  W/S - Navigate    ENTER - Analyze Sample", ConsoleText.ColorTag.GRAY);
            b.line("  A/D - Change Tab  Q/Ctrl+C - Exit", ConsoleText.ColorTag.GRAY);
        }

        render(ctx, b.build());
    }

    private void buildSampleEntries() {
        entries.clear();
        int count = 1;
        for (int i = 0; i < cachedSequencers.size(); i++) {
            var seq = cachedSequencers.get(i);
            var out = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
            if (!out.isEmpty() && out.getTag() != null) {
                entries.add(new SampleEntry(i, SampleStatus.ANALYZED, out.getTag().copy(), String.format("TISSUE-%03d", count++)));
            }
        }
        for (int i = 0; i < cachedSequencers.size(); i++) {
            var seq = cachedSequencers.get(i);
            var in = seq.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
            boolean hasValidInput = !in.isEmpty() && in.getItem() == ModItems.TISSUE_SAMPLE.get() && in.getTag() != null;
            if (hasValidInput && seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT).isEmpty()) {
                entries.add(new SampleEntry(i, SampleStatus.UNANALYZED, in.getTag().copy(), String.format("TISSUE-%03d", count++)));
            }
        }
        if (listSelectedIndex >= entries.size()) listSelectedIndex = Math.max(0, entries.size() - 1);
    }

    private String bar(int pct, int width) {
        int filled = Math.max(0, Math.min(width, (pct * width) / 100));
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < width; i++) r.append(i < filled ? '|' : '.');
        return r.toString();
    }

    @Override
    public boolean handle(ConsoleContext ctx, String name, List<String> args) {
        switch (name) {
            case "back":
                if (viewMode == ViewMode.READOUT) {
                    viewMode = ViewMode.LIST;
                    selectedSlotIndex = -1;
                    ctx.setScreenText("");
                    renderList(ctx);
                    return true;
                }
                return false;
            case "list":
                refresh(ctx);
                renderList(ctx);
                return true;
            case "readout":
                if (args.isEmpty()) { return false; }
                int ridx = parseIndex(args.get(0));
                if (!validIndex(ridx)) { return false; }
                var selectedSeq = cachedSequencers.get(ridx);
                var out = selectedSeq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                if (out.isEmpty()) {
                    render(ctx, ConsoleText.color("Readout for sequencer " + (ridx + 1) + ":\n<no output>", ConsoleText.ColorTag.GRAY));
                } else {
                    var tag = out.getTag();
                    List<String> labels = new ArrayList<>();
                    currentGeneIds.clear();
                    currentGeneQualities.clear();
                    currentGeneLabels.clear();
                    if (tag != null) {
                        if (tag.contains("genes", 9)) {
                            var list = tag.getList("genes", 10);
                            for (int i = 0; i < list.size() && labels.size() < 4; i++) {
                                var g = list.getCompound(i);
                                String id = g.getString("id");
                                int q = g.getInt("quality");
                                String dispName = g.getString("name");
                                if (dispName.isEmpty()) dispName = compactLabelFromId(id, q);
                                currentGeneIds.add(id);
                                currentGeneQualities.add(q);
                                currentGeneLabels.add(dispName);
                                labels.add(dispName);
                            }
                        } else if (tag.contains("Traits", 9)) {
                            var list = tag.getList("Traits", 8);
                            int q = tag.getInt("Quality");
                            for (int i = 0; i < list.size() && labels.size() < 4; i++) {
                                String raw = list.getString(i);
                                String id = "bandits_quirk_lib:legacy." + raw.toLowerCase();
                                currentGeneIds.add(id);
                                currentGeneQualities.add(q);
                                currentGeneLabels.add("gene_" + String.format("%04x", Math.abs(id.hashCode()) & 0xFFFF));
                                // legacy fallback: synthesize a name pattern
                                labels.add("gene_" + String.format("%04x", Math.abs(id.hashCode()) & 0xFFFF));
                            }
                        }
                    }

                    // Build deterministic layout for this sample and render
                    Layout layout = computeDeterministicLayout(tag, labels);
                    readoutAnimLines = buildCenteredDnaReadoutLines(ridx + 1, layout.leftTexts, layout.rightTexts);
                    // Build index of selectable slots from computed display order
                    visibleSlotRowIndices.clear();
                    visibleSlotRowIndices.addAll(layout.displayRows);
                    displayGeneIndices.clear();
                    displayGeneIndices.addAll(layout.displayGeneIndices);
                    selectedSlotIndex = visibleSlotRowIndices.isEmpty() ? -1 : 0;
                    // Insert non-rendering marker line at very top to flag readout on client
                    readoutAnimLines.add(0, "[READOUT]");
                    inDetailsView = false;
                    String key = computeReadoutKey(tag);
                    currentReadoutKey = key;
                    if (!animatedReadoutKeys.contains(key)) {
                        // Pre-highlight first selectable during animation
                        applySelectionToAnimLines();
                        readoutAnimating = true;
                        readoutAnimLineIndex = 0;
                        readoutAnimCharIndex = 0;
                        render(ctx, "");
                    } else {
                        // Immediately render full readout with current selection
                        redrawReadoutWithSelection(ctx);
                    }
                }
                viewMode = ViewMode.READOUT;
                readoutIndex = ridx;
                return true;
            case "start":
                // Not used in new UI
                return false;
            case "status":
                // Not used in new UI
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
        // Periodically refresh the list/progress to auto-update the screen
        if (viewMode == ViewMode.LIST) {
            refresh(ctx);
            renderList(ctx);
        } else if (viewMode == ViewMode.READOUT) {
            if (readoutAnimating && !readoutAnimLines.isEmpty()) {
                StringBuilder out = new StringBuilder();
                // Fully emit lines before the current one
                for (int i = 0; i < readoutAnimLineIndex; i++) {
                    out.append(readoutAnimLines.get(i)).append('\n');
                }
                // Advance characters on current line
                String current = readoutAnimLines.get(readoutAnimLineIndex);
                int nextIdx = Math.min(current.length(), readoutAnimCharIndex + readoutAnimSpeed);
                // Ensure leading tags (e.g., [CENTER][AQUA]) reveal atomically
                int tagsEnd = leadingTagsEndIndex(current);
                if (nextIdx > 0 && nextIdx < tagsEnd) nextIdx = tagsEnd;
                readoutAnimCharIndex = nextIdx;
                out.append(current, 0, readoutAnimCharIndex);
                // Render current progress
                render(ctx, out.toString());
                // If current line finished, move to next
                if (readoutAnimCharIndex >= current.length()) {
                    readoutAnimLineIndex++;
                    readoutAnimCharIndex = 0;
                    if (readoutAnimLineIndex >= readoutAnimLines.size()) {
                        readoutAnimating = false;
                        if (currentReadoutKey != null && !currentReadoutKey.isEmpty()) {
                            animatedReadoutKeys.add(currentReadoutKey);
                        }
                        // Ensure selection is green after animation completes
                        redrawReadoutWithSelection(ctx);
                    }
                }
            }
        }
    }

    private int parseIndex(String token) {
        try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; }
    }

    private boolean validIndex(int idx) { return idx >= 0 && idx < cachedSequencers.size(); }

    // Makes a nice consistent header for the DNA Readout
    private String buildDnaHeader(int sequencer) {
        String outputTitle = "[CENTER][AQUA]DNA Results - Sequencer " + sequencer + "\n";
        String outputBar = "[CENTER]==================================";
        return outputTitle + outputBar;
    }

    // Build centered DNA with left/right compact gene labels around it; returns lines to render
    private List<String> buildCenteredDnaReadoutLines(int sequencer, List<String> labels) {
        List<String> lines = new ArrayList<>();
        // Header (already includes [CENTER])
        String header = buildDnaHeader(sequencer);
        for (String h : header.split("\\r?\\n", -1)) lines.add(h);

        // DNA art, 11 lines total
        String[] dna = new String[]{
                "A-=-T",
                "\\    /",
                " T==A",
                "/    \\",
                "C-=-G",
                "\\    /",
                " G==C",
                "/    \\",
                "T-=-A",
                "\\    /",
                " A==T"
        };

        // Define 12 potential slots mapped to DNA rows (indices in dna[]): 6 left, 6 right
        int[] leftRows = new int[]{0, 2, 4, 6, 8, 10};
        int[] rightRows = new int[]{1, 3, 5, 7, 9, 11};

        // Generate compact gene ids for up to 4 labels (still render 4 for now): L,R,L,R
        String[] leftTexts = new String[leftRows.length];
        String[] rightTexts = new String[rightRows.length];
        int maxGenes = Math.min(4, labels.size());
        for (int i = 0; i < maxGenes; i++) {
            String tag = compactTag(labels.get(i));
            if (i % 2 == 0) leftTexts[i / 2] = tag; else rightTexts[i / 2] = tag;
        }

        // Columns and connectors around centered DNA
        int labelWidth = 10; // compact label region fits "gene_0000"
        String connector = "----"; // 4 chars
        for (int row = 0; row < dna.length; row++) {
            String leftLab = "";
            String rightLab = "";
            for (int idx = 0; idx < leftRows.length; idx++) if (leftRows[idx] == row) { leftLab = leftTexts[idx] == null ? "" : leftTexts[idx]; break; }
            for (int idx = 0; idx < rightRows.length; idx++) if (rightRows[idx] == row) { rightLab = rightTexts[idx] == null ? "" : rightTexts[idx]; break; }

            String leftCol = padRight(leftLab, labelWidth);
            String rightCol = padLeft(rightLab, labelWidth);
            String leftConn = leftLab.isEmpty() ? " ".repeat(connector.length()) : connector;
            String rightConn = rightLab.isEmpty() ? " ".repeat(connector.length()) : connector;

            String composite = leftCol + leftConn + "[CORE]" + dna[row] + "[/CORE]" + rightConn + rightCol;
            lines.add("[CENTER]" + composite);
        }

        return lines;
    }

    private static final class Layout {
        final String[] leftTexts;
        final String[] rightTexts;
        final List<Integer> geneRows; // placement rows in insertion order
        final List<Integer> displayRows; // rows sorted top->bottom for navigation/highlight
        final List<Integer> displayGeneIndices; // mapping from display index -> gene index
        Layout(String[] leftTexts, String[] rightTexts, List<Integer> geneRows, List<Integer> displayRows, List<Integer> displayGeneIndices) {
            this.leftTexts = leftTexts;
            this.rightTexts = rightTexts;
            this.geneRows = geneRows;
            this.displayRows = displayRows;
            this.displayGeneIndices = displayGeneIndices;
        }
    }

    private Layout computeDeterministicLayout(CompoundTag sampleTag, List<String> labels) {
        // Define 12 potential rows; we will choose up to 4 unique rows based on seed
        int[] leftRows = new int[]{0, 2, 4, 6, 8, 10};
        int[] rightRows = new int[]{1, 3, 5, 7, 9};
        long seed = 0L;
        if (sampleTag != null) {
            if (sampleTag.contains("entity_uuid", 8)) seed ^= sampleTag.getString("entity_uuid").hashCode();
            if (sampleTag.contains("layout_salt", 4)) seed ^= sampleTag.getLong("layout_salt");
            if (sampleTag.contains("genes", 9)) {
                var list = sampleTag.getList("genes", 10);
                for (int i = 0; i < list.size(); i++) seed ^= list.getCompound(i).getString("id").hashCode();
            }
        }
        Random rng = new Random(seed ^ 0xC2B2AE3D27D4EB4FL);

        boolean[] usedLeft = new boolean[leftRows.length];
        boolean[] usedRight = new boolean[rightRows.length];
        String[] leftTexts = new String[leftRows.length];
        String[] rightTexts = new String[rightRows.length];
        List<Integer> rowsUsed = new ArrayList<>();
        List<Integer> geneIndicesUsed = new ArrayList<>();

        int max = Math.min(4, labels.size());
        for (int i = 0; i < max; i++) {
            boolean placeLeft = (rng.nextBoolean());
            if (placeLeft) {
                // pick an unused left row
                int attempts = 0;
                int idx;
                do { idx = rng.nextInt(leftRows.length); attempts++; } while (usedLeft[idx] && attempts < 16);
                // fallback to first free
                if (usedLeft[idx]) {
                    for (int j = 0; j < usedLeft.length; j++) if (!usedLeft[j]) { idx = j; break; }
                }
                usedLeft[idx] = true;
                leftTexts[idx] = compactTag(labels.get(i));
                rowsUsed.add(leftRows[idx]);
            } else {
                int attempts = 0;
                int idx;
                do { idx = rng.nextInt(rightRows.length); attempts++; } while (usedRight[idx] && attempts < 16);
                if (usedRight[idx]) {
                    for (int j = 0; j < usedRight.length; j++) if (!usedRight[j]) { idx = j; break; }
                }
                usedRight[idx] = true;
                rightTexts[idx] = compactTag(labels.get(i));
                rowsUsed.add(rightRows[idx]);
            }
            geneIndicesUsed.add(i);
        }
        // Sort display order top->bottom and map to gene indices
        java.util.List<int[]> pairs = new java.util.ArrayList<>(); // [row, geneIdx]
        for (int i = 0; i < rowsUsed.size(); i++) pairs.add(new int[]{rowsUsed.get(i), geneIndicesUsed.get(i)});
        pairs.sort(java.util.Comparator.comparingInt(a -> a[0]));
        List<Integer> displayRows = new ArrayList<>();
        List<Integer> displayGeneIdxs = new ArrayList<>();
        for (int[] p : pairs) { displayRows.add(p[0]); displayGeneIdxs.add(p[1]); }
        // Default fill rowsUsed if fewer than labels count (should not happen)
        while (rowsUsed.size() < max) rowsUsed.add(rowsUsed.isEmpty() ? 0 : rowsUsed.get(rowsUsed.size()-1));
        if (displayRows.isEmpty()) { displayRows.addAll(rowsUsed); displayGeneIdxs.addAll(geneIndicesUsed); }
        return new Layout(leftTexts, rightTexts, rowsUsed, displayRows, displayGeneIdxs);
    }

    private List<String> buildCenteredDnaReadoutLines(int sequencer, String[] leftTexts, String[] rightTexts) {
        List<String> lines = new ArrayList<>();
        String header = buildDnaHeader(sequencer);
        for (String h : header.split("\\r?\\n", -1)) lines.add(h);

        String[] dna = new String[]{
                "A-=-T",
                "\\    /",
                " T==A",
                "/    \\",
                "C-=-G",
                "\\    /",
                " G==C",
                "/    \\",
                "T-=-A",
                "\\    /",
                " A==T"
        };
        int[] leftRows = new int[]{0, 2, 4, 6, 8, 10};
        int[] rightRows = new int[]{1, 3, 5, 7, 9};

        int labelWidth = 10;
        String connector = "----";
        for (int row = 0; row < dna.length; row++) {
            String leftLab = "";
            String rightLab = "";
            for (int idx = 0; idx < leftRows.length; idx++) if (leftRows[idx] == row) { leftLab = leftTexts[idx] == null ? "" : leftTexts[idx]; break; }
            for (int idx = 0; idx < rightRows.length; idx++) if (rightRows[idx] == row) { rightLab = rightTexts[idx] == null ? "" : rightTexts[idx]; break; }

            String leftCol = padRight(leftLab, labelWidth);
            String rightCol = padLeft(rightLab, labelWidth);
            String leftConn = leftLab.isEmpty() ? " ".repeat(connector.length()) : connector;
            String rightConn = rightLab.isEmpty() ? " ".repeat(connector.length()) : connector;

            String composite = leftCol + leftConn + "[CORE]" + dna[row] + "[/CORE]" + rightConn + rightCol;
            lines.add("[CENTER]" + composite);
        }
        return lines;
    }

    private String compactLabelFromId(String id, int quality) {
        // Fallback generator for a consistent short tag
        return "gene_" + String.format("%04x", Math.abs((id + "_" + quality).hashCode()) & 0xFFFF);
    }

    private String compactTag(String label) {
        // Trim and sanitize to fit in label column (10 chars)
        String s = label == null ? "" : label.replaceAll("[^A-Za-z0-9_()]+", "");
        if (s.length() > 10) return s.substring(0, 10);
        return s;
    }

    private void rebuildVisibleSlots(List<String> labels) {
        visibleSlotRowIndices.clear();
        // Mirror assignment used above for 4 labels: L rows at 0,2; R rows at 1,3.
        int[] rows = new int[]{0, 1, 2, 3};
        int max = Math.min(4, labels.size());
        for (int i = 0; i < max; i++) visibleSlotRowIndices.add(rows[i]);
    }

    @Override
    public boolean onKey(ConsoleContext ctx, String action) {
        if (viewMode == ViewMode.LIST) {
            if (entries.isEmpty()) return false;
            switch (action) {
                case "up":
                    if (listSelectedIndex > 0) listSelectedIndex--;
                    renderList(ctx);
                    return true;
                case "down":
                    if (listSelectedIndex < entries.size() - 1) listSelectedIndex++;
                    renderList(ctx);
                    return true;
                case "enter":
                    SampleEntry e = entries.get(listSelectedIndex);
                    var seq = cachedSequencers.get(e.sequencerIndex);
                    if (e.status == SampleStatus.UNANALYZED) {
                        var input = seq.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
                        var output = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!output.isEmpty()) {
                            statusLine = "[RED]Output not empty.";
                        } else if (input.isEmpty() || input.getItem() != ModItems.TISSUE_SAMPLE.get() || input.getTag() == null) {
                            statusLine = "[RED]Insert a valid tissue_sample.";
                        } else {
                            seq.startProcessing();
                            statusLine = "[GREEN]Analysis started.";
                        }
                        renderList(ctx);
                    }
                    return true;
                case "interrupt":
                    ctx.exitProgram();
                    return true;
                default:
                    return false;
            }
        } else if (viewMode == ViewMode.READOUT) {
            if (visibleSlotRowIndices.isEmpty()) return false;
            switch (action) {
                case "up":
                    if (selectedSlotIndex > 0) selectedSlotIndex--;
                    redrawReadoutWithSelection(ctx);
                    return true;
                case "down":
                    if (selectedSlotIndex < visibleSlotRowIndices.size() - 1) selectedSlotIndex++;
                    redrawReadoutWithSelection(ctx);
                    return true;
                case "enter":
                    openSelectedGeneDetails(ctx);
                    return true;
                case "interrupt":
                    if (inDetailsView) {
                        inDetailsView = false;
                        redrawReadoutWithSelection(ctx);
                    } else {
                        viewMode = ViewMode.LIST;
                        selectedSlotIndex = -1;
                        ctx.setScreenText("");
                        renderList(ctx);
                    }
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private List<String> buildDnaReadoutPlain(CompoundTag tag) {
        List<String> labels = new ArrayList<>();
        if (tag != null) {
            if (tag.contains("genes", 9)) {
                var list = tag.getList("genes", 10);
                for (int i = 0; i < list.size() && labels.size() < 6; i++) {
                    var g = list.getCompound(i);
                    String id = g.getString("id");
                    int q = g.getInt("quality");
                    String dispName = g.contains("name", 8) ? g.getString("name") : compactLabelFromId(id, q);
                    labels.add(compactTag(dispName));
                }
            } else if (tag.contains("Traits", 9)) {
                var list = tag.getList("Traits", 8);
                int q = tag.contains("Quality", 3) ? tag.getInt("Quality") : 0;
                for (int i = 0; i < list.size() && labels.size() < 6; i++) {
                    String raw = list.getString(i);
                    String id = "bandits_quirk_lib:legacy." + raw.toLowerCase();
                    labels.add(compactTag("gene_" + String.format("%04x", Math.abs(id.hashCode()) & 0xFFFF)));
                }
            }
        }

        // Build side-by-side DNA with 6 label slots (3 left, 3 right) across 11 rows
        String[] dna = new String[]{
                "A-=-T",
                "\\    /",
                " T==A",
                "/    \\",
                "C-=-G",
                "\\    /",
                " G==C",
                "/    \\",
                "T-=-A",
                "\\    /",
                " A==T"
        };
        int[] leftRows = new int[]{0, 2, 4, 6, 8, 10};
        int[] rightRows = new int[]{1, 3, 5, 7, 9};
        String[] leftTexts = new String[leftRows.length];
        String[] rightTexts = new String[rightRows.length];
        for (int i = 0; i < Math.min(6, labels.size()); i++) {
            if (i % 2 == 0) leftTexts[i / 2] = labels.get(i); else rightTexts[i / 2] = labels.get(i);
        }
        int labelWidth = 8;
        String connector = "----";
        List<String> lines = new ArrayList<>();
        for (int row = 0; row < dna.length; row++) {
            String leftLab = "";
            String rightLab = "";
            for (int idx = 0; idx < leftRows.length; idx++) if (leftRows[idx] == row) { leftLab = leftTexts[idx] == null ? "" : leftTexts[idx]; break; }
            for (int idx = 0; idx < rightRows.length; idx++) if (rightRows[idx] == row) { rightLab = rightTexts[idx] == null ? "" : rightTexts[idx]; break; }
            String leftCol = padLeft(leftLab, labelWidth);
            String rightCol = padRight(rightLab, labelWidth);
            String leftConn = leftLab.isEmpty() ? "    " : connector;
            String rightConn = rightLab.isEmpty() ? "    " : connector;
            lines.add("  " + leftCol + leftConn + dna[row] + rightConn + rightCol);
        }
        return lines;
    }

    private void redrawReadoutWithSelection(ConsoleContext ctx) {
        // recolor the selected label line to green by inserting [GREEN]
        List<String> updated = new ArrayList<>(readoutAnimLines);
        // first two lines are header; subsequent lines correspond to dna rows
        int headerLines = 2;
        int offset = 1; // due to [READOUT] marker line
        for (int i = 0; i < visibleSlotRowIndices.size(); i++) {
            int row = visibleSlotRowIndices.get(i);
            int lineIdx = offset + headerLines + row;
            if (lineIdx < 0 || lineIdx >= updated.size()) continue;
            String line = updated.get(lineIdx);
            // Remove any existing color tag at start
            String stripped = line;
            if (stripped.startsWith("[GREEN]")) stripped = stripped.substring(7);
            if (i == selectedSlotIndex) updated.set(lineIdx, "[GREEN]" + stripped);
            else updated.set(lineIdx, stripped);
        }
        ctx.setScreenText(String.join("\n", updated));
    }

    private void openSelectedGeneDetails(ConsoleContext ctx) {
        if (selectedSlotIndex < 0 || selectedSlotIndex >= visibleSlotRowIndices.size()) return;
        int geneIdx = selectedSlotIndex; // matches readout order
        String id = geneIdx >= 0 && geneIdx < currentGeneIds.size() ? currentGeneIds.get(geneIdx) : "unknown";
        int q = geneIdx >= 0 && geneIdx < currentGeneQualities.size() ? currentGeneQualities.get(geneIdx) : 0;
        Gene g = null;
        boolean known = false;
        boolean hasDb = false;
        String cryptic = (geneIdx >= 0 && geneIdx < currentGeneLabels.size()) ? currentGeneLabels.get(geneIdx) : "";
        try {
            var rid = new ResourceLocation(id);
            g = GeneRegistry.get(rid);
            hasDb = ctx.getBlockEntity().hasDatabase();
            known = hasDb && ctx.getBlockEntity().isGeneKnown(rid);
        } catch (Exception ignored) {}

        ProgramScreenBuilder b = screen()
                .header("Gene Details")
                .separator()
                .twoColumn("Label", cryptic, 10)
                .twoColumn("Translated", (known ? net.minecraft.network.chat.Component.translatable(id).getString() : "unknown"), 10)
                .twoColumn("Quality", (known ? (q + "%") : "unknown"), 10);
        if (known && g != null) {
            b.twoColumn("Category", g.getCategory().name(), 10)
             .twoColumn("Rarity", g.getRarity().name(), 10)
             .twoColumn("Combinable", String.valueOf(g.isCombinable()), 10);
            String desc = g.getDescription();
            if (desc != null && !desc.isEmpty()) b.blank().line(desc);
        } else {
            if (!hasDb) {
                b.blank().line("Insert a Gene Database to reveal details.", ConsoleText.ColorTag.GRAY);
            } else {
                b.blank().line("Identify this gene to reveal details.", ConsoleText.ColorTag.GRAY);
            }
        }
        b.blank();
        ctx.setScreenText(b.build());
        inDetailsView = true;
    }

    private void applySelectionToAnimLines() {
        if (readoutAnimLines == null || readoutAnimLines.isEmpty()) return;
        int headerLines = 2;
        int offset = 1; // [READOUT]
        for (int i = 0; i < visibleSlotRowIndices.size(); i++) {
            int row = visibleSlotRowIndices.get(i);
            int lineIdx = offset + headerLines + row;
            if (lineIdx >= 0 && lineIdx < readoutAnimLines.size()) {
                String line = readoutAnimLines.get(lineIdx);
                if (line.startsWith("[GREEN]")) continue;
                if (i == selectedSlotIndex) readoutAnimLines.set(lineIdx, "[GREEN]" + line);
            }
        }
    }

    private int leadingTagsEndIndex(String line) {
        int i = 0;
        boolean cont = true;
        while (cont && i < line.length() && line.charAt(i) == '[') {
            int end = line.indexOf(']', i);
            if (end <= i) break;
            String tag = line.substring(i, end + 1);
            if ("[RED]".equals(tag) || "[GREEN]".equals(tag) || "[YELLOW]".equals(tag) ||
                    "[AQUA]".equals(tag) || "[GRAY]".equals(tag) || "[CENTER]".equals(tag) || "[RIGHT]".equals(tag)) {
                i = end + 1;
            } else {
                cont = false;
            }
        }
        return i;
    }

    private String computeReadoutKey(CompoundTag tag) {
        if (tag == null) return "";
        StringBuilder sb = new StringBuilder();
        if (tag.contains("entity_uuid", 8)) sb.append(tag.getString("entity_uuid"));
        if (tag.contains("layout_salt", 4)) sb.append('|').append(tag.getLong("layout_salt"));
        if (tag.contains("genes", 9)) {
            var list = tag.getList("genes", 10);
            for (int i = 0; i < Math.min(4, list.size()); i++) sb.append('|').append(list.getCompound(i).getString("id"));
        } else if (tag.contains("Traits", 9)) {
            var list = tag.getList("Traits", 8);
            for (int i = 0; i < Math.min(4, list.size()); i++) sb.append('|').append(list.getString(i));
        }
        return sb.toString();
    }

    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder b = new StringBuilder(s);
        while (b.length() < width) b.append(' ');
        return b.toString();
    }

    private String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder b = new StringBuilder();
        while (b.length() + s.length() < width) b.append(' ');
        b.append(s);
        return b.toString();
    }

    private String humanizeTrait(String raw) {
        if (raw == null || raw.isEmpty()) return "Unknown";
        // Simple mapping for nicer labels; fall back to spaced words
        switch (raw) {
            case "StrongBones": return "Regulator";
            case "RapidHealing": return "Repair Prot";
            case "DenseMuscle": return "Enhancer";
            case "NightVision": return "Immunity";
            default:
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (i > 0 && Character.isUpperCase(c)) sb.append(' ');
                    sb.append(c);
                }
                return sb.toString();
        }
    }
}


