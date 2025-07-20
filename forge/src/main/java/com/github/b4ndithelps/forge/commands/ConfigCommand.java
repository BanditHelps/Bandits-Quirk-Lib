package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.config.ConfigManager;
import com.github.b4ndithelps.values.BodyConstants;
import com.github.b4ndithelps.values.StaminaConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register as a separate command since /bql is already taken
        dispatcher.register(Commands.literal("bqlconfig")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ConfigCommand::reloadConfig)));
    }
    
    /**
     * Creates config command branches that can be added to existing command structures
     */
    public static LiteralArgumentBuilder<CommandSourceStack> createConfigBranch() {
        return Commands.literal("config")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ConfigCommand::reloadConfig));
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ConfigManager.forceReloadAll();
            source.sendSuccess(() -> Component.literal("§aConfig reloaded successfully!"), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cFailed to reload config: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
} 