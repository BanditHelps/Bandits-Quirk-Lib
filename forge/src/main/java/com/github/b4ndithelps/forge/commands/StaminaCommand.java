package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

public class StaminaCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bql_stamina")
                        .then(Commands.literal("get")
                                .executes(StaminaCommand::getStaminaStats)
                        )
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(StaminaCommand::setCurrentStamina)))
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(2))
                                .executes(StaminaCommand::debugStamina))
        );
    }

    private static int getStaminaStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            String staminaInfo = StaminaHelper.getStaminaInfo(player);

            player.sendSystemMessage(Component.literal("=== Stamina Info ==="));
            player.sendSystemMessage(Component.literal("Stamina: " + staminaInfo));

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Displays all information to the player about their current stamina setup.
     * @param context
     * @return
     */
    private static int debugStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            StaminaHelper.debugStamina(player);
            return 1;
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setCurrentStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int amount = IntegerArgumentType.getInteger(context, "amount");

            // Add this debug logging
            BanditsQuirkLibForge.LOGGER.info("Attempting to set stamina for player: " + player.getName().getString());

            LazyOptional<IStaminaData> staminaOpt = StaminaHelper.getStaminaData(player);
            if (!staminaOpt.isPresent()) {
                BanditsQuirkLibForge.LOGGER.error("No stamina data found for player: " + player.getName().getString());
                player.sendSystemMessage(Component.literal("Error: No stamina data found!"));
                return 0;
            }

            staminaOpt.ifPresent(staminaData -> {
                BanditsQuirkLibForge.LOGGER.info("Setting stamina from " + staminaData.getCurrentStamina() + " to " + amount);

                int maxStamina = staminaData.getMaxStamina();
                int newStamina = Math.min(amount, maxStamina);
                staminaData.setCurrentStamina(newStamina);

                BanditsQuirkLibForge.LOGGER.info("Stamina set successfully to: " + newStamina);

                player.sendSystemMessage(Component.literal(
                        String.format("Stamina set to %d/%d", newStamina, maxStamina)
                ));
            });

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }


}
