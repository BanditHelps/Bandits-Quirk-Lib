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
    }
}


