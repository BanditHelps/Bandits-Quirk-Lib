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
                        .executes(ConfigCommand::reloadConfig))
                .then(Commands.literal("check")
                        .requires(source -> source.hasPermission(2))
                        .executes(ConfigCommand::checkConfig)));
    }
    
    /**
     * Creates config command branches that can be added to existing command structures
     */
    public static LiteralArgumentBuilder<CommandSourceStack> createConfigBranch() {
        return Commands.literal("config")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ConfigCommand::reloadConfig))
                .then(Commands.literal("check")
                        .requires(source -> source.hasPermission(2))
                        .executes(ConfigCommand::checkConfig));
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ConfigManager.forceReloadAll();
            source.sendSuccess(() -> Component.literal("§aConfig reloaded successfully!"), true);
            
            // Show updated values
            source.sendSuccess(() -> Component.literal("§7Max damage: " + BodyConstants.MAX_DAMAGE), false);
            source.sendSuccess(() -> Component.literal("§7Stamina gain chance: " + StaminaConstants.STAMINA_GAIN_CHANCE), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cFailed to reload config: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }

    private static int checkConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6=== BQL Config Values ==="), false);
        source.sendSuccess(() -> Component.literal("§7Max damage: §f" + BodyConstants.MAX_DAMAGE), false);
        source.sendSuccess(() -> Component.literal("§7Damage stages: §f" + java.util.Arrays.toString(BodyConstants.DAMAGE_STAGE_PERCENTAGES)), false);
        source.sendSuccess(() -> Component.literal("§7Stamina gain chance: §f" + StaminaConstants.STAMINA_GAIN_CHANCE), false);
        source.sendSuccess(() -> Component.literal("§7Stamina gain req: §f" + StaminaConstants.STAMINA_GAIN_REQ), false);
        source.sendSuccess(() -> Component.literal("§7Plus Ultra tag: §f" + StaminaConstants.PLUS_ULTRA_TAG), false);
        
        return 1;
    }
} 