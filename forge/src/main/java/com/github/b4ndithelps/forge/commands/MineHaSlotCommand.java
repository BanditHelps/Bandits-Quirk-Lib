package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.values.CreationShopConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class MineHaSlotCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mineha_slot_item")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1))
                                        .executes(context -> getSlotItem(context))
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                                .executes(context -> setSlotItem(context))
                                        )
                                )
                        )
                        .then(Commands.literal("test")
                                .executes(context -> testSlots(context))
                        )
        );
    }

    private static int getSlotItem(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int slot = IntegerArgumentType.getInteger(context, "slot");

            String slotKey = "MineHa.Slot." + slot;
            String itemName = player.getPersistentData().getString(slotKey);

            if (itemName.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("empty"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal(itemName), false);
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * This method will attempt to slot an item into the creation hot bar.
     * It will detect if the item is learned before putting it in there,
     * because it makes my life so much easier maintaining the gui.
     * @param context
     * @return
     */
    private static int setSlotItem(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int slot = IntegerArgumentType.getInteger(context, "slot");
            String item = StringArgumentType.getString(context, "item");

            boolean isSlottable = false;

            // Check to see if the material for the slot is unlocked

            try {
                Scoreboard scoreboard = player.level().getScoreboard();
                Objective objective = null;
                int selected = -1;

                // Check what bitmap the item is in
                if (CreationShopConstants.BIT_MAP_1_TABLE.containsKey(item)) {
                    objective = scoreboard.getObjective("MineHa.Creation.BitMap1");
                    selected = 1;
                } else if (CreationShopConstants.BIT_MAP_2_TABLE.containsKey(item)) {
                    objective = scoreboard.getObjective("MineHa.Creation.BitMap2");
                    selected = 2;
                } else if (CreationShopConstants.BIT_MAP_3_TABLE.containsKey(item)) {
                    objective = scoreboard.getObjective("MineHa.Creation.BitMap3");
                    selected = 3;
                }

                if (objective != null) {
                    // Check if the value is inside the bitmap scoreboard
                    Score score = scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), objective);
                    int scoreValue = score.getScore();

                    int bitMask = -1;

                    switch(selected) {
                        case 1:
                            bitMask = CreationShopConstants.BIT_MAP_1_TABLE.get(item);
                            break;
                        case 2:
                            bitMask = CreationShopConstants.BIT_MAP_2_TABLE.get(item);
                            break;
                        case 3:
                            bitMask = CreationShopConstants.BIT_MAP_3_TABLE.get(item);
                            break;
                        default:
                            context.getSource().sendFailure(Component.literal("Error: Key not found"));
                            return 0;
                    }

                    isSlottable = (scoreValue & bitMask) != 0;


                }

            } catch (Exception e) {
                BanditsQuirkLibForge.LOGGER.warn("Could not get appropriate score board. Ignoring");
            }


            if (isSlottable) {
                String slotKey = "MineHa.Slot." + slot;
                player.getPersistentData().putString(slotKey, item);

                context.getSource().sendSuccess(
                        () -> Component.literal("Set Slot " + slot + " to: " + item),
                        false
                );

                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Error: IsSlottable false"));
                return 0;
            }


        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int testSlots(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            StringBuilder result = new StringBuilder("Slots: ");

            for (int i = 0; i <= 3; i++) {
                String slotKey = "MineHa.Slot." + i;
                String value = player.getPersistentData().getString(slotKey);
                if (i > 1) result.append(", ");
                result.append(i).append(":");
                result.append(value.isEmpty() ? "empty" : value);
            }

            context.getSource().sendSuccess(() -> Component.literal(result.toString()), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
