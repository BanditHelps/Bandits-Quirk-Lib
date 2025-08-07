package com.github.b4ndithelps.forge.commands.conditions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

// Functional interface for command predicates
@FunctionalInterface
public interface CommandPredicate {
    boolean test(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException;
}
