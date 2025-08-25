package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyze program: lists adjacent sequencers, allows start N, status [N], readout N, back, exit.
 */
public class AnalyzeProgram extends AbstractConsoleProgram {
    private final List<GeneSequencerBlockEntity> cachedSequencers = new ArrayList<>();
    private String statusLine = ""; // shown at top of program screen
    private enum ViewMode { LIST, READOUT }
    private ViewMode viewMode = ViewMode.LIST;
    private int readoutIndex = -1;
    // Selection state for READOUT
    private int selectedSlotIndex = -1; // 0..(visibleSlots-1) over actual gene entries
    private List<Integer> visibleSlotRowIndices = new ArrayList<>(); // rows that have a gene label
    // Animation state for READOUT view (character-by-character)
    private boolean readoutAnimating = false;
    private List<String> readoutAnimLines = new ArrayList<>();
    private int readoutAnimLineIndex = 0;
    private int readoutAnimCharIndex = 0;
    private int readoutAnimSpeed = 80; // chars per tick (even faster)
    private boolean readoutHasAnimatedOnce = false;
    private boolean inDetailsView = false;
    // Current readout data (mapped 1:1 with visible slots order)
    private final List<String> currentGeneIds = new ArrayList<>();
    private final List<Integer> currentGeneQualities = new ArrayList<>();

    @Override
    public String getName() { return "analyze"; }

    @Override
    public void onEnter(ConsoleContext ctx) {
        refresh(ctx);
    }

    private void refresh(ConsoleContext ctx) {
        cachedSequencers.clear();
        var term = ctx.getBlockEntity();
        var level = term.getLevel();
        var pos = term.getBlockPos();
        for (var dir : Direction.values()) {
            var be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof GeneSequencerBlockEntity seq) cachedSequencers.add(seq);
        }
        viewMode = ViewMode.LIST;
        readoutIndex = -1;
        list(ctx);
    }

    private void list(ConsoleContext ctx) {
        var b = screen()
                .header("Analyze Program")
                .line("Commands: list | start <n> | status [n] | readout <n> | back | exit", ConsoleText.ColorTag.GRAY)
                .blank();
        if (!statusLine.isEmpty()) {
            b.line(statusLine);
            b.blank();
        }
        if (cachedSequencers.isEmpty()) {
            b.line("No connected sequencers.", ConsoleText.ColorTag.GRAY);
        } else {
            b.line("Connected Sequencers:", ConsoleText.ColorTag.WHITE);
            for (int i = 0; i < cachedSequencers.size(); i++) {
                var s = cachedSequencers.get(i);
                int pct = s.getMaxProgress() == 0 ? 0 : (s.getProgress() * 100 / s.getMaxProgress());
                boolean hasOutput = !s.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT).isEmpty();
                var input = s.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
                boolean hasValidInput = !input.isEmpty()
                        && input.getItem() == ModItems.TISSUE_SAMPLE.get()
                        && input.getTag() != null
                        && (input.getTag().contains("genes", 9) || input.getTag().contains("GenomeSeed") || input.getTag().contains("Traits"));
                boolean hasInvalidSample = !input.isEmpty() && !hasValidInput;

                ConsoleText.ColorTag color;
                if (hasOutput) color = ConsoleText.ColorTag.GREEN;
                else if (s.isRunning()) color = ConsoleText.ColorTag.YELLOW;
                else if (hasInvalidSample) color = ConsoleText.ColorTag.RED;
                else if (hasValidInput) color = ConsoleText.ColorTag.AQUA;
                else color = ConsoleText.ColorTag.GRAY;

                b.line(i + 1 + ") [" + bar(pct, 20) + "] " + pct + "% " + (s.isRunning() ? "(Running)" : "(Idle)") + (hasOutput ? " - Output Ready" : ""), color);
            }
        }
        render(ctx, b.build());
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
                    // Return to last DNA readout screen from details view or selection
                    viewMode = ViewMode.LIST;
                    selectedSlotIndex = -1;
                    ctx.setScreenText("");
                    list(ctx);
                    return true;
                }
                return false;
            case "list":
                refresh(ctx);
                return true;
            case "start":
                if (args.isEmpty()) { ctx.println("Usage: start <n>"); return true; }
                int idx = parseIndex(args.get(0));
                if (!validIndex(idx)) { ctx.println("Invalid index"); return true; }
                var seq = cachedSequencers.get(idx);
                var input = seq.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
                var output = seq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                if (!output.isEmpty()) {
                    statusLine = "[RED]Error: Output slot not empty. Remove product first.";
                } else if (input.isEmpty()) {
                    statusLine = "[RED]Error: Insert a tissue_sample in the input slot.";
                } else if (input.getItem() != ModItems.TISSUE_SAMPLE.get()) {
                    statusLine = "[RED]Error: Invalid input item. Requires tissue_sample.";
                } else if (input.getTag() == null || (!input.getTag().contains("genes", 9) && !input.getTag().contains("GenomeSeed") && !input.getTag().contains("Traits"))) {
                    statusLine = "[RED]Error: Sample missing genetic NBT. Re-extract a valid sample.";
                } else {
                    seq.startProcessing();
                    statusLine = "[GREEN]Sequencer started.";
                }
                refresh(ctx);
                return true;
            case "status":
                if (args.isEmpty()) { refresh(ctx); return true; }
                int sidx = parseIndex(args.get(0));
                if (!validIndex(sidx)) { ctx.println("Invalid index"); return true; }
                var s = cachedSequencers.get(sidx);
                int pct = s.getMaxProgress() == 0 ? 0 : (s.getProgress() * 100 / s.getMaxProgress());
                ctx.setScreenText("Sequencer " + (sidx + 1) + ":\n[" + bar(pct, 20) + "] " + pct + "%\n" + (s.isRunning() ? "Running" : "Idle"));
                return true;
            case "readout":
                if (args.isEmpty()) { ctx.println("Usage: readout <n>"); return true; }
                int ridx = parseIndex(args.get(0));
                if (!validIndex(ridx)) { ctx.println("Invalid index"); return true; }
                var selectedSeq = cachedSequencers.get(ridx);
                var out = selectedSeq.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT);
                if (out.isEmpty()) {
                    render(ctx, ConsoleText.color("Readout for sequencer " + (ridx + 1) + ":\n<no output>", ConsoleText.ColorTag.GRAY));
                } else {
                    var tag = out.getTag();
                    List<String> labels = new ArrayList<>();
                    currentGeneIds.clear();
                    currentGeneQualities.clear();
                    if (tag != null) {
                        if (tag.contains("genes", 9)) {
                            var list = tag.getList("genes", 10);
                            for (int i = 0; i < list.size() && labels.size() < 4; i++) {
                                var g = list.getCompound(i);
                                String id = g.getString("id");
                                int q = g.getInt("quality");
                                name = g.getString("name");
                                if (name.isEmpty()) name = compactLabelFromId(id, q);
                                currentGeneIds.add(id);
                                currentGeneQualities.add(q);
                                labels.add(name);
                            }
                        } else if (tag.contains("Traits", 9)) {
                            var list = tag.getList("Traits", 8);
                            int q = tag.getInt("Quality");
                            for (int i = 0; i < list.size() && labels.size() < 4; i++) {
                                String raw = list.getString(i);
                                String id = "bandits_quirk_lib:legacy." + raw.toLowerCase();
                                currentGeneIds.add(id);
                                currentGeneQualities.add(q);
                                // legacy fallback: synthesize a name pattern
                                labels.add("gene_" + String.format("%04x", Math.abs(id.hashCode()) & 0xFFFF));
                            }
                        }
                    }
                    while (labels.size() < 4) {
                        if (labels.isEmpty()) labels.add("Regulator");
                        else if (labels.size() == 1) labels.add("Repair Prot");
                        else if (labels.size() == 2) labels.add("Enhancer");
                        else labels.add("Immunity");
                    }
                    // Build centered DNA readout with left/right gene placements and animate char-by-char
                    readoutAnimLines = buildCenteredDnaReadoutLines(ridx + 1, labels);
                    // build index of selectable slots from current labels
                    rebuildVisibleSlots(labels);
                    selectedSlotIndex = visibleSlotRowIndices.isEmpty() ? -1 : 0;
                    // Insert non-rendering marker line at very top to flag readout on client
                    readoutAnimLines.add(0, "[READOUT]");
                    inDetailsView = false;
                    if (!readoutHasAnimatedOnce) {
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
            list(ctx);
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
                        readoutHasAnimatedOnce = true;
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
        if (viewMode != ViewMode.READOUT || visibleSlotRowIndices.isEmpty()) return false;
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
                // go back from details to readout if in details view, otherwise back to list
                if (inDetailsView) {
                    inDetailsView = false;
                    redrawReadoutWithSelection(ctx);
                } else {
                    viewMode = ViewMode.LIST;
                    selectedSlotIndex = -1;
                    ctx.setScreenText("");
                    list(ctx);
                }
                return true;
            default:
                return false;
        }
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
        try { g = GeneRegistry.get(new ResourceLocation(id)); } catch (Exception ignored) {}

        ProgramScreenBuilder b = screen()
                .header("Gene Details")
                .separator()
                .twoColumn("ID", id, 10)
                .twoColumn("Quality", String.valueOf(q), 10);
        if (g != null) {
            b.twoColumn("Category", g.getCategory().name(), 10)
             .twoColumn("Rarity", g.getRarity().name(), 10)
             .twoColumn("Combinable", String.valueOf(g.isCombinable()), 10);
            String desc = g.getDescription();
            if (desc != null && !desc.isEmpty()) b.blank().line(desc);
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


