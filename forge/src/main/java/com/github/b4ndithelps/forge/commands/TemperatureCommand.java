package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.systems.TempHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TemperatureCommand {

    private static final String DEBUG_KEY = "Bql.TempDebug";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("temperature")
                        .then(Commands.literal("on").executes(ctx -> setTemperatureHud(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setTemperatureHud(ctx, false)))
                        .then(Commands.literal("toggle").executes(TemperatureCommand::toggleTemperatureHud))
        );
    }

    private static int toggleTemperatureHud(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean current = player.getPersistentData().getBoolean(DEBUG_KEY);
        return setTemperatureHud(context, !current);
    }

    private static int setTemperatureHud(CommandContext<CommandSourceStack> context, boolean value) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.getPersistentData().putBoolean(DEBUG_KEY, value);
        float temp = TempHelper.getInnerTemp(player);
        if (value) {
            Component enabled = Component.literal(String.format("Temperature HUD enabled (%.1f)", temp));
            context.getSource().sendSuccess(() -> enabled, true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Temperature HUD disabled"), true);
        }
        return 1;
    }
}

