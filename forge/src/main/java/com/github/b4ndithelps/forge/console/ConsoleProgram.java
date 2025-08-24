package com.github.b4ndithelps.forge.console;

import java.util.List;

/**
 * Represents a stateful console "program" that can handle commands while active.
 */
public interface ConsoleProgram {
    String getName();

    default void onEnter(ConsoleContext ctx) {}

    default void onExit(ConsoleContext ctx) {}

    default void onTick(ConsoleContext ctx) {}

    /**
     * @return true if the command was handled by this program.
     */
    boolean handle(ConsoleContext ctx, String name, List<String> args);
}


