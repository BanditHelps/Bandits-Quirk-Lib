package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyze program: lists adjacent sequencers, allows start N, status [N], readout N, back, exit.
 */
public class AnalyzeProgram implements ConsoleProgram {
    private final List<GeneSequencerBlockEntity> cachedSequencers = new ArrayList<>();
    private String statusLine = ""; // shown at top of program screen
    private enum ViewMode { LIST, READOUT }
    private ViewMode viewMode = ViewMode.LIST;
    private int readoutIndex = -1;

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
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze Program\n");
        sb.append("Commands: list | start <n> | status [n] | readout <n> | back | exit\n");
        if (!statusLine.isEmpty()) {
            sb.append(statusLine).append('\n');
        }
        sb.append('\n');
        if (cachedSequencers.isEmpty()) {
            sb.append("No connected sequencers.\n");
        } else {
            sb.append("Connected Sequencers:\n");
            for (int i = 0; i < cachedSequencers.size(); i++) {
                var s = cachedSequencers.get(i);
                int pct = s.getMaxProgress() == 0 ? 0 : (s.getProgress() * 100 / s.getMaxProgress());
                boolean hasOutput = !s.getItem(GeneSequencerBlockEntity.SLOT_OUTPUT).isEmpty();
                var input = s.getItem(GeneSequencerBlockEntity.SLOT_INPUT);
                boolean hasValidInput = !input.isEmpty()
                        && input.getItem() == ModItems.TISSUE_SAMPLE.get()
                        && input.getTag() != null
                        && (input.getTag().contains("GenomeSeed") || input.getTag().contains("Traits"));
                boolean hasInvalidSample = !input.isEmpty() && !hasValidInput;

                String colorTag;
                if (hasOutput) colorTag = "[GREEN]"; // sequenced sample ready
                else if (s.isRunning()) colorTag = "[YELLOW]";
                else if (hasInvalidSample) colorTag = "[RED]"; // invalid sample present
                else if (hasValidInput) colorTag = "[AQUA]"; // valid sample present, idle
                else colorTag = "[GRAY]"; // empty

                sb.append(colorTag)
                  .append(String.valueOf(i + 1)).append(") ")
                  .append("[").append(bar(pct, 20)).append("] ").append(pct).append("% ");
                sb.append(s.isRunning() ? "(Running)" : "(Idle)");
                if (hasOutput) sb.append(" - Output Ready");
                sb.append('\n');
            }
        }
        ctx.setScreenText(sb.toString());
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
                } else if (input.getTag() == null || (!input.getTag().contains("GenomeSeed") && !input.getTag().contains("Traits"))) {
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
                    ctx.setScreenText("[GRAY]Readout for sequencer " + (ridx + 1) + ":\n<no output>");
                } else {
                    var tag = out.getTag();
                    List<String> labels = new ArrayList<>();
                    if (tag != null && tag.contains("Traits", 9)) {
                        var list = tag.getList("Traits", 8);
                        for (int i = 0; i < list.size() && labels.size() < 4; i++) {
                            String raw = list.getString(i);
                            labels.add(humanizeTrait(raw));
                        }
                    }
                    while (labels.size() < 4) {
                        if (labels.isEmpty()) labels.add("Regulator");
                        else if (labels.size() == 1) labels.add("Repair Prot");
                        else if (labels.size() == 2) labels.add("Enhancer");
                        else labels.add("Immunity");
                    }
                    String diagram = buildDnaDisplay(labels);
                    String header = buildDnaHeader(ridx + 1);
                    ctx.setScreenText(header + ":\n" + diagram);
                }
                viewMode = ViewMode.READOUT;
                readoutIndex = ridx;
                return true;
            case "back":
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
        }
    }

    private int parseIndex(String token) {
        try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; }
    }

    private boolean validIndex(int idx) { return idx >= 0 && idx < cachedSequencers.size(); }

    // Makes a nice consistent header for the DNA Readout
    private String buildDnaHeader(int sequencer) {
        String outputTitle = "[AQUA]DNA Results - Sequencer " + sequencer + "\n";
        String outputBar = "=================================";
        return outputTitle + outputBar;
    }

    // Build a side-by-side DNA-like diagram with labels to the right
    private String buildDnaDisplay(List<String> labels) {
        String[] left = new String[]{
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
        String[] right = new String[]{
                "GENE_A: " + labels.get(0),
                "",
                "GENE_B: " + labels.get(1),
                "",
                "GENE_C: " + labels.get(2),
                "",
                "",
                "",
                "GENE_D: " + labels.get(3),
                "",
                ""
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left.length; i++) {
            String l = left[i];
            String r = right[i];
            // pad left to fixed width for alignment
            sb.append(String.format("%-12s  %s", l, r)).append('\n');
        }
        return sb.toString();
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


