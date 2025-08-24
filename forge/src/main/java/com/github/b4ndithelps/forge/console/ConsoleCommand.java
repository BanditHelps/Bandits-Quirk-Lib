package com.github.b4ndithelps.forge.console;

import java.util.List;

/**
 * Represents a single console command that can be executed by the DNA Sequencer console.
 * Implementations should be stateless and side-effect free except via the provided context.
 */
public interface ConsoleCommand {
    /**
     * @return the primary name used to invoke this command (e.g. "echo").
     */
    String getName();

    /**
     * @return optional aliases that can also invoke this command (e.g. ["print"]).
     */
    default List<String> getAliases() { return List.of(); }

    /**
     * @return a short one-line description for use in help output.
     */
    String getDescription();

    /**
     * Execute this command.
     *
     * @param context execution context bound to the current block entity instance
     * @param args    arguments passed to the command (does not include the command name)
     */
    void execute(ConsoleContext context, List<String> args);
}


