package com.github.b4ndithelps.forge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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

    private static int setSlotItem(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int slot = IntegerArgumentType.getInteger(context, "slot");
            String item = StringArgumentType.getString(context, "item");

            String slotKey = "MineHa.Slot." + slot;
            player.getPersistentData().putString(slotKey, item);

            context.getSource().sendSuccess(
                    () -> Component.literal("Set Slot " + slot + " to: " + item),
                    false
            );

            return 1;
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
