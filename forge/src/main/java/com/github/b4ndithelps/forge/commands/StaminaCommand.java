package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.github.b4ndithelps.values.StaminaConstants.EXHAUSTION_LEVELS;
import static com.github.b4ndithelps.values.StaminaConstants.PLUS_ULTRA_TAG;

public class StaminaCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bql")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .then(Commands.literal("stamina")
                                                .executes(StaminaCommand::getStaminaStats))
                                        .then(Commands.literal("upgrade_points")
                                                .executes(StaminaCommand::getUpgradePoints))
                                )
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("max")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::setMaxStamina)))
                                        .then(Commands.literal("current")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::setCurrentStamina)))
                                        .then(Commands.literal("exhaust")
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::setExhaustionLevel)))
                                        .then(Commands.literal("plus_ultra")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(StaminaCommand::setPlusUltra)))
                                        .then(Commands.literal("upgrade_points")
                                                .then(Commands.argument("points", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::setUpgradePoints)))
                                )
                                .then(Commands.literal("use")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(StaminaCommand::useStamina))
                                )
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("max")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::addMaxStamina)))
                                        .then(Commands.literal("current")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(StaminaCommand::addCurrentStamina)))
                                )
                                .then(Commands.literal("debug")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(StaminaCommand::debugStamina))
                        )
        );
    }

    private static int getStaminaStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");

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
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            StaminaHelper.debugStamina(player);
            return 1;
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int getUpgradePoints(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            StaminaHelper.getUpgradePointsInfo(player);
            return 1;
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setUpgradePoints(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "points");

            StaminaHelper.setUpgradePoints(player, amount);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int addCurrentStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            StaminaHelper.addCurrentStamina(player, amount);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setCurrentStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            StaminaHelper.setCurrentStamina(player, amount);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int addMaxStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            StaminaHelper.addMaxStamina(player, amount);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setMaxStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            StaminaHelper.setMaxStamina(player, amount);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setExhaustionLevel(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int level = IntegerArgumentType.getInteger(context, "level");

            if (level > (EXHAUSTION_LEVELS.length - 1) || level < 0) {
                player.sendSystemMessage(Component.literal("Invalid Level: range: 0-" + (EXHAUSTION_LEVELS.length - 1)));
                return 0;
            }

            StaminaHelper.setExhaustionLevel(player, level);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int setPlusUltra(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            boolean val = BoolArgumentType.getBool(context, "value");

            if (!val) {
                player.removeTag(PLUS_ULTRA_TAG);
                player.sendSystemMessage(Component.literal("§lPLUS ULTRA DISABLED! §rYou can no longer push through your limits."));
            } else {
                player.addTag(PLUS_ULTRA_TAG);
                player.sendSystemMessage(Component.literal("§b§lPLUS ULTRA! §rYou can now push beyond your normal limits!"));
            }

            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }

    private static int useStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            StaminaHelper.useStamina(player, amount);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            BanditsQuirkLibForge.LOGGER.error("Command error: " + e.getMessage(), e);
            return 0;
        }
    }


}
