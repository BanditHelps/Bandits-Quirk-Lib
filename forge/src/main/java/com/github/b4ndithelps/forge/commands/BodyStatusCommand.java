package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.github.b4ndithelps.values.BodyConstants.MAX_DAMAGE;

public class BodyStatusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("body")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .executes(BodyStatusCommand::getDamage)))
                                        .then(Commands.literal("stage")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .executes(BodyStatusCommand::getDamageStage)))
                                        .then(Commands.literal("status")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("statusName", StringArgumentType.string())
                                                                .executes(BodyStatusCommand::getCustomStatus))))
                                        .then(Commands.literal("float")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("key", StringArgumentType.string())
                                                                .executes(BodyStatusCommand::getCustomFloat))))
                                        .then(Commands.literal("string")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("key", StringArgumentType.string())
                                                                .executes(BodyStatusCommand::getCustomString))))
                                        .then(Commands.literal("all")
                                                .executes(BodyStatusCommand::getAllBodyStatus))
                                        .then(Commands.literal("custom_statuses")
                                                .executes(BodyStatusCommand::getAllCustomStatuses))
                                )
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                                                .executes(BodyStatusCommand::setDamage))))
                                        .then(Commands.literal("status")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("statusName", StringArgumentType.string())
                                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                                        .executes(BodyStatusCommand::setCustomStatus)))))
                                        .then(Commands.literal("float")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("key", StringArgumentType.string())
                                                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                                        .executes(BodyStatusCommand::setCustomFloat)))))
                                        .then(Commands.literal("string")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("key", StringArgumentType.string())
                                                                .then(Commands.argument("value", StringArgumentType.string())
                                                                        .executes(BodyStatusCommand::setCustomString)))))
                                )
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                                                .executes(BodyStatusCommand::addDamage))))
                                        .then(Commands.literal("float")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("key", StringArgumentType.string())
                                                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                                        .executes(BodyStatusCommand::addToCustomFloat)))))
                                )
                                .then(Commands.literal("heal")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("bodypart", StringArgumentType.string())
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f))
                                                        .executes(BodyStatusCommand::healDamage)))
                                        .then(Commands.literal("all")
                                                .executes(BodyStatusCommand::healAll))
                                )
                                .then(Commands.literal("reset")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("bodypart", StringArgumentType.string())
                                                .executes(BodyStatusCommand::resetBodyPart))
                                        .then(Commands.literal("all")
                                                .executes(BodyStatusCommand::resetAll))
                                )
                                .then(Commands.literal("debug")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(BodyStatusCommand::debugBodyStatus))
                                .then(Commands.literal("test")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(BodyStatusCommand::runTestScenarios))
                                .then(Commands.literal("init")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.literal("status")
                                                .then(Commands.argument("bodypart", StringArgumentType.string())
                                                        .then(Commands.argument("statusName", StringArgumentType.string())
                                                                .then(Commands.argument("defaultLevel", IntegerArgumentType.integer(0))
                                                                        .executes(BodyStatusCommand::initializeStatus)))))
                                        .then(Commands.literal("status_all")
                                                .then(Commands.argument("statusName", StringArgumentType.string())
                                                        .then(Commands.argument("defaultLevel", IntegerArgumentType.integer(0))
                                                                .executes(BodyStatusCommand::initializeStatusAllParts)))))
                        )
        );
    }

    private static int getDamage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            
            float damage = BodyStatusHelper.getDamage(player, bodyPartName);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6%s's %s damage: §e%.2f", 
                            player.getName().getString(), bodyPartName, damage)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getDamageStage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            
            String stage = BodyStatusHelper.getDamageStage(player, bodyPartName);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6%s's %s stage: §e%s", 
                            player.getName().getString(), bodyPartName, stage)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getCustomStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String statusName = StringArgumentType.getString(context, "statusName");
            
            int level = BodyStatusHelper.getCustomStatus(player, bodyPartName, statusName);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6%s's %s %s status: §e%d", 
                            player.getName().getString(), bodyPartName, statusName, level)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getCustomFloat(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String key = StringArgumentType.getString(context, "key");
            
            float value = BodyStatusHelper.getCustomFloat(player, bodyPartName, key);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6%s's %s %s: §e%.2f", 
                            player.getName().getString(), bodyPartName, key, value)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getCustomString(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String key = StringArgumentType.getString(context, "key");
            
            String value = BodyStatusHelper.getCustomString(player, bodyPartName, key);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6%s's %s %s: §e%s", 
                            player.getName().getString(), bodyPartName, key, value)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getAllBodyStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6=== Body Status for " + player.getName().getString() + " ==="), false);
            
            for (BodyPart part : BodyPart.values()) {
                String partName = part.getName();
                float damage = BodyStatusHelper.getDamage(player, partName);
                String stage = BodyStatusHelper.getDamageStage(player, partName);
                
                context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§e%s: §7%.2f damage §8(§7%s§8)", 
                                partName, damage, stage)), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int getAllCustomStatuses(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6=== Custom Statuses for " + player.getName().getString() + " ==="), false);
            
            boolean hasAnyStatuses = false;
            
            for (BodyPart part : BodyPart.values()) {
                String partName = part.getName();
                var bodyStatus = BodyStatusHelper.getBodyStatus(player);
                var partData = bodyStatus.getBodyPartData(part);
                var activeStatuses = partData.getActiveCustomStatuses();
                
                if (!activeStatuses.isEmpty()) {
                    hasAnyStatuses = true;
                    StringBuilder statusInfo = new StringBuilder();
                    statusInfo.append("§e").append(partName).append("§7: ");
                    
                    boolean first = true;
                    for (String statusName : activeStatuses) {
                        if (!first) {
                            statusInfo.append("§7, ");
                        }
                        int level = bodyStatus.getCustomStatus(part, statusName);
                        statusInfo.append("§a").append(statusName).append("§8(§a").append(level).append("§8)");
                        first = false;
                    }
                    
                    String finalMessage = statusInfo.toString();
                    context.getSource().sendSuccess(() -> Component.literal(finalMessage), false);
                }
            }
            
            if (!hasAnyStatuses) {
                context.getSource().sendSuccess(() -> Component.literal(
                        "§7No custom statuses found for " + player.getName().getString()), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setDamage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            float amount = FloatArgumentType.getFloat(context, "amount");
            
            BodyStatusHelper.setDamage(player, bodyPartName, amount);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Set %s's %s damage to §e%.2f", 
                            player.getName().getString(), bodyPartName, amount)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCustomStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String statusName = StringArgumentType.getString(context, "statusName");
            int level = IntegerArgumentType.getInteger(context, "level");
            
            BodyStatusHelper.setCustomStatus(player, bodyPartName, statusName, level);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Set %s's %s %s status to §e%d", 
                            player.getName().getString(), bodyPartName, statusName, level)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCustomFloat(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String key = StringArgumentType.getString(context, "key");
            float value = FloatArgumentType.getFloat(context, "value");
            
            BodyStatusHelper.setCustomFloat(player, bodyPartName, key, value);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Set %s's %s %s to §e%.2f", 
                            player.getName().getString(), bodyPartName, key, value)), false);
            BodyStatusHelper.syncToTrackingPlayers(player);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addToCustomFloat(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String key = StringArgumentType.getString(context, "key");
            float value = FloatArgumentType.getFloat(context, "value");

            BodyStatusHelper.addToCustomFloat(player, bodyPartName, key, value);

            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Added %.2f to %s's %s: %s",
                            value, player.getName().getString(), bodyPartName, key)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setCustomString(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String key = StringArgumentType.getString(context, "key");
            String value = StringArgumentType.getString(context, "value");
            
            BodyStatusHelper.setCustomString(player, bodyPartName, key, value);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Set %s's %s %s to §e%s", 
                            player.getName().getString(), bodyPartName, key, value)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addDamage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            float amount = FloatArgumentType.getFloat(context, "amount");
            
            float oldDamage = BodyStatusHelper.getDamage(player, bodyPartName);
            BodyStatusHelper.addDamage(player, bodyPartName, amount);
            float newDamage = BodyStatusHelper.getDamage(player, bodyPartName);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Added §e%.2f§6 damage to %s's %s (§e%.2f§6 → §e%.2f§6)", 
                            amount, player.getName().getString(), bodyPartName, oldDamage, newDamage)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int healDamage(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            float amount = FloatArgumentType.getFloat(context, "amount");
            
            float oldDamage = BodyStatusHelper.getDamage(player, bodyPartName);
            BodyStatusHelper.setDamage(player, bodyPartName, Math.max(0, oldDamage - amount));
            float newDamage = BodyStatusHelper.getDamage(player, bodyPartName);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Healed §e%.2f§6 damage from %s's %s (§e%.2f§6 → §e%.2f§6)", 
                            amount, player.getName().getString(), bodyPartName, oldDamage, newDamage)), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int healAll(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            for (BodyPart part : BodyPart.values()) {
                BodyStatusHelper.setDamageNoSync(player, part.getName(), 0.0f);
            }
            // Sync once after all changes
            BodyStatusHelper.syncToClient(player);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Healed all body parts for %s", player.getName().getString())), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetBodyPart(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            BodyStatusHelper.getBodyStatus(player).resetPart(part);
            BodyStatusHelper.syncToClient(player);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Reset %s for %s", bodyPartName, player.getName().getString())), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            BodyStatusHelper.getBodyStatus(player).resetAll();
            BodyStatusHelper.syncToClient(player);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§6Reset all body parts for %s", player.getName().getString())), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int debugBodyStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6=== DEBUG: Body Status for " + player.getName().getString() + " ==="), false);
            
            for (BodyPart part : BodyPart.values()) {
                String partName = part.getName();
                float damage = BodyStatusHelper.getDamage(player, partName);
                String stage = BodyStatusHelper.getDamageStage(player, partName);
                boolean broken = BodyStatusHelper.isPartBroken(player, partName);
                boolean destroyed = BodyStatusHelper.isPartDestroyed(player, partName);
                boolean sprained = BodyStatusHelper.isPartSprained(player, partName);
                
                context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§e%s: §7%.2f/%.2f §8(§7%s§8) §8[§7B:%s D:%s S:%s§8]",
                                partName, damage, MAX_DAMAGE, stage, broken, destroyed, sprained)), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int runTestScenarios(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            
            context.getSource().sendSuccess(() -> Component.literal(
                    "§6=== Running Body Status Test Scenarios ==="), false);
            
            // Test 1: Basic damage tests
            context.getSource().sendSuccess(() -> Component.literal("§eTest 1: Basic Damage"), false);
            BodyStatusHelper.setDamageNoSync(player, "head", 25.0f);
            BodyStatusHelper.setDamageNoSync(player, "left_arm", 50.0f);
            BodyStatusHelper.setDamageNoSync(player, "right_leg", 85.0f);
            BodyStatusHelper.setDamageNoSync(player, "chest", 100.0f);
            
            // Test 2: Custom status tests
            context.getSource().sendSuccess(() -> Component.literal("§eTest 2: Custom Status"), false);
            BodyStatusHelper.setCustomStatusNoSync(player, "left_hand", "frostbite", 3);
            BodyStatusHelper.setCustomStatusNoSync(player, "right_hand", "burn", 2);
            
            // Test 3: Custom data tests
            context.getSource().sendSuccess(() -> Component.literal("§eTest 3: Custom Data"), false);
            BodyStatusHelper.setCustomFloatNoSync(player, "head", "temperature", 101.5f);
            BodyStatusHelper.setCustomStringNoSync(player, "chest", "condition", "bruised");
            
            // Sync all changes to client
            BodyStatusHelper.syncToClient(player);
            
            // Show results
            context.getSource().sendSuccess(() -> Component.literal("§eResults:"), false);
            for (BodyPart part : BodyPart.values()) {
                String partName = part.getName();
                float damage = BodyStatusHelper.getDamage(player, partName);
                String stage = BodyStatusHelper.getDamageStage(player, partName);
                
                if (damage > 0) {
                    context.getSource().sendSuccess(() -> Component.literal(
                            String.format("§7%s: %.2f damage (%s)", partName, damage, stage)), false);
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§aTest scenarios completed!"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int initializeStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String bodyPartName = StringArgumentType.getString(context, "bodypart");
            String statusName = StringArgumentType.getString(context, "statusName");
            int defaultLevel = IntegerArgumentType.getInteger(context, "defaultLevel");
            
            boolean initialized = BodyStatusHelper.initializeNewStatus(player, bodyPartName, statusName, defaultLevel);
            
            if (initialized) {
                context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§aInitialized %s's %s %s status to §e%d", 
                                player.getName().getString(), bodyPartName, statusName, defaultLevel)), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§7%s's %s already has %s status (not overwritten)", 
                                player.getName().getString(), bodyPartName, statusName)), false);
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int initializeStatusAllParts(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            String statusName = StringArgumentType.getString(context, "statusName");
            int defaultLevel = IntegerArgumentType.getInteger(context, "defaultLevel");
            
            int initializedCount = BodyStatusHelper.initializeNewStatusForAllParts(player, statusName, defaultLevel);
            
            context.getSource().sendSuccess(() -> Component.literal(
                    String.format("§aInitialized %s status to §e%d§a on §e%d§a body parts for %s", 
                            statusName, defaultLevel, initializedCount, player.getName().getString())), false);
            
            if (initializedCount < BodyPart.values().length) {
                int existingCount = BodyPart.values().length - initializedCount;
                context.getSource().sendSuccess(() -> Component.literal(
                        String.format("§7(§e%d§7 parts already had this status)", existingCount)), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
} 