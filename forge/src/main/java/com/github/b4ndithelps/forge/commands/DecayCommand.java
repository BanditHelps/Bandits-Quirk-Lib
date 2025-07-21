package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DecayCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("decay_test")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("quirk")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.literal("get")
                            .executes(DecayCommand::getQuirkFactor))
                        .then(Commands.literal("set")
                            .then(Commands.argument("factor", IntegerArgumentType.integer(0, 50))
                                .executes(DecayCommand::setQuirkFactor)))))
        );
    }

    private static int getQuirkFactor(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            double quirkFactor = QuirkFactorHelper.getQuirkFactor(target);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§b" + target.getGameProfile().getName() + "'s Quirk Factor: §6" + 
                String.format("%.1f", quirkFactor * 100) + "%"
            ), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Quirk factor command error: ", e);
            return 0;
        }
    }

    private static int setQuirkFactor(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            int factorScore = IntegerArgumentType.getInteger(context, "factor");
            double quirkFactor = factorScore * 0.1; // Convert score to decimal
            
            QuirkFactorHelper.setQuirkFactor(target, quirkFactor);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§bSet §6" + target.getGameProfile().getName() + "'s §bQuirk Factor to: §6" + 
                String.format("%.1f", quirkFactor * 100) + "%"
            ), false);
            
            target.sendSystemMessage(Component.literal(
                "§bYour Quirk Factor has been set to: §6" + 
                String.format("%.1f", quirkFactor * 100) + "%"
            ));
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Set quirk factor command error: ", e);
            return 0;
        }
    }
} 