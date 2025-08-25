package com.github.b4ndithelps.forge.console;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers a few built-in commands: help, echo, status.
 */
public final class BasicConsoleCommands {
    private BasicConsoleCommands() {}

    public static void registerDefaults(ConsoleCommandRegistry registry) {
        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "help"; }
            @Override public String getDescription() { return "List available commands"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                var cmds = ConsoleCommandRegistry.getInstance().all().stream()
                        .map(c -> c.getName() + " - " + c.getDescription())
                        .sorted()
                        .collect(Collectors.toList());
                if (cmds.isEmpty()) ctx.println("No commands registered.");
                else {
                    ctx.println("Available commands:");
                    cmds.forEach(ctx::println);
                }
            }
        });

        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "echo"; }
            @Override public String getDescription() { return "Echo back the provided text"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                ctx.println(String.join(" ", args));
            }
        });

        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "status"; }
            @Override public String getDescription() { return "Show current processing status"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                var be = ctx.getBlockEntity();
                ctx.println("Progress: " + be.getProgress() + "/" + be.getMaxProgress());
            }
        });

        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "clear"; }
            @Override public String getDescription() { return "Clears the current console screen"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                ctx.clearConsole();
            }
        });

        registry.register(new ConsoleCommand() {
            @Override
            public String getName() {
                return "testAnim";
            }

            @Override
            public String getDescription() {
                return "Just to test the fanxy line";
            }

            @Override
            public void execute(ConsoleContext ctx, List<String> args) {
                List<String> lines = new ArrayList<>();
                int speed = Integer.parseInt(String.valueOf(args.get(0)));
                lines.add("Loading...");
                lines.add("Doing something fancy...");
                lines.add("Super stuff....");
                lines.add("Complete!");
                ctx.enqueueLines(lines, speed);
            }
        });

        registry.register(new ConsoleCommand() {
            @Override
            public String getName() {
                return "testAnimChar";
            }

            @Override
            public String getDescription() {
                return "Just to test the fancy animation";
            }

            @Override
            public void execute(ConsoleContext ctx, List<String> args) {
                int speed = Integer.parseInt(String.valueOf(args.get(0)));
                ctx.enqueueCharacters("Loading: |||||||||||||||||||", speed);
            }
        });

        // Control adjacent GeneSequencer
        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "seqstart"; }
            @Override public String getDescription() { return "Start sequencing on adjacent GeneSequencer"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                var be = ctx.getBlockEntity();
                var level = be.getLevel();
                var pos = be.getBlockPos();
                boolean found = false;
                for (var dir : net.minecraft.core.Direction.values()) {
                    var neighbor = level.getBlockEntity(pos.relative(dir));
                    if (neighbor instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity seq) {
                        found = true;
                        // Validate input sample
                        var input = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_INPUT);
                        var output = seq.getItem(com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity.SLOT_OUTPUT);
                        if (!output.isEmpty()) {
                            ctx.println("Sequencer error: Output slot not empty. Remove product first.");
                            break;
                        }
                        if (input.isEmpty()) {
                            ctx.println("Sequencer error: Insert a tissue_sample in the input slot.");
                            break;
                        }
                        if (input.getItem() != com.github.b4ndithelps.forge.item.ModItems.TISSUE_SAMPLE.get()) {
                            ctx.println("Sequencer error: Invalid input item. Requires tissue_sample.");
                            break;
                        }
                        if (input.getTag() == null || (!input.getTag().contains("GenomeSeed") && !input.getTag().contains("Traits"))) {
                            ctx.println("Sequencer error: Sample missing genetic NBT. Re-extract a valid sample.");
                            break;
                        }
                        seq.startProcessing();
                        ctx.println("Sequencer started");
                        break;
                    }
                }
                if (!found) ctx.println("No GeneSequencer adjacent");
            }
        });

        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "seqstop"; }
            @Override public String getDescription() { return "Stop sequencing on adjacent GeneSequencer"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                var be = ctx.getBlockEntity();
                var level = be.getLevel();
                var pos = be.getBlockPos();
                boolean found = false;
                for (var dir : net.minecraft.core.Direction.values()) {
                    var neighbor = level.getBlockEntity(pos.relative(dir));
                    if (neighbor instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity seq) {
                        seq.stopProcessing();
                        ctx.println("Sequencer stopped");
                        found = true;
                        break;
                    }
                }
                if (!found) ctx.println("No GeneSequencer adjacent");
            }
        });

        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "seqstatus"; }
            @Override public String getDescription() { return "Show status of adjacent GeneSequencer"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                var be = ctx.getBlockEntity();
                var level = be.getLevel();
                var pos = be.getBlockPos();
                boolean found = false;
                for (var dir : net.minecraft.core.Direction.values()) {
                    var neighbor = level.getBlockEntity(pos.relative(dir));
                    if (neighbor instanceof com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity seq) {
                        ctx.println("Running: " + seq.isRunning() + ", Progress: " + seq.getProgress() + "/" + seq.getMaxProgress());
                        found = true;
                        break;
                    }
                }
                if (!found) ctx.println("No GeneSequencer adjacent");
            }
        });

        // Enter Analyze Program
        registry.register(new ConsoleCommand() {
            @Override public String getName() { return "analyze"; }
            @Override public String getDescription() { return "Open Analyze Program for connected sequencers"; }
            @Override public void execute(ConsoleContext ctx, List<String> args) {
                ctx.getBlockEntity().pushProgram(new AnalyzeProgram());
            }
        });
    }
}


