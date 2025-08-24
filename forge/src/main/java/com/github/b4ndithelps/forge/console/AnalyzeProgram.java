package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyze program: lists adjacent sequencers, allows start N, status [N], readout N, back, exit.
 */
public class AnalyzeProgram implements ConsoleProgram {
    private final List<GeneSequencerBlockEntity> cachedSequencers = new ArrayList<>();

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
        list(ctx);
    }

    private void list(ConsoleContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze Program\n");
        sb.append("Commands: list | start <n> | status [n] | readout <n> | back | exit\n\n");
        if (cachedSequencers.isEmpty()) {
            sb.append("No connected sequencers.\n");
        } else {
            sb.append("Connected Sequencers:\n");
            for (int i = 0; i < cachedSequencers.size(); i++) {
                var s = cachedSequencers.get(i);
                int pct = s.getMaxProgress() == 0 ? 0 : (s.getProgress() * 100 / s.getMaxProgress());
                sb.append(String.valueOf(i + 1)).append(") ");
                sb.append("[").append(bar(pct, 20)).append("] ").append(pct).append("% ");
                sb.append(s.isRunning() ? "(Running)" : "(Idle)").append("\n");
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
                cachedSequencers.get(idx).startProcessing();
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
                // For now, just print a mock genes list. Replace with real data extraction later.
                ctx.setScreenText("Readout for sequencer " + (ridx + 1) + ":\nGenes: ACTT, G7F, XXY, Z22");
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
        list(ctx);
    }

    private int parseIndex(String token) {
        try { return Integer.parseInt(token) - 1; } catch (Exception e) { return -1; }
    }

    private boolean validIndex(int idx) { return idx >= 0 && idx < cachedSequencers.size(); }
}


