package com.github.b4ndithelps.forge.console;

import java.util.*;

/**
 * Simple in-memory registry for console commands.
 * Commands are stored by lowercase name and any aliases will also be mapped.
 */
public final class ConsoleCommandRegistry {
    private static final ConsoleCommandRegistry INSTANCE = new ConsoleCommandRegistry();

    public static ConsoleCommandRegistry getInstance() { return INSTANCE; }

    private final Map<String, ConsoleCommand> nameToCommand = new HashMap<>();

    private ConsoleCommandRegistry() {}

    public synchronized void register(ConsoleCommand command) {
        Objects.requireNonNull(command, "command");
        nameToCommand.put(command.getName().toLowerCase(Locale.ROOT), command);
        for (String alias : command.getAliases()) {
            if (alias != null && !alias.isBlank()) {
                nameToCommand.put(alias.toLowerCase(Locale.ROOT), command);
            }
        }
    }

    public Optional<ConsoleCommand> find(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(nameToCommand.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<ConsoleCommand> all() {
        // Return unique commands (aliases map to same instance)
        return new LinkedHashSet<>(nameToCommand.values());
    }
}


