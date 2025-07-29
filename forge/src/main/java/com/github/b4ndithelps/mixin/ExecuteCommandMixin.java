package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.commands.conditions.CommandPredicate;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

/**
 * Adds in a new conditional for the /execute command.
 * The "body" condition allows you to execute a command based on status from the body helper
 */
@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin {

    @Inject(method = "addConditionals", at = @At("RETURN"))
    private static void addBodyConditional(CommandNode<CommandSourceStack> parent,
                                           LiteralArgumentBuilder<CommandSourceStack> literal,
                                           boolean isIf,
                                           CommandBuildContext context,
                                           CallbackInfoReturnable<ArgumentBuilder<CommandSourceStack, ?>> cir) {

        // Add our custom "body" conditional
        literal.then(Commands.literal("body")
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.literal("get")
                                .then(Commands.literal("damage")
                                        .then(Commands.argument("body_part", StringArgumentType.word())
                                                        .then(Commands.literal("matches")
                                                                .then(addBodyConditional(parent, Commands.argument("range", RangeArgument.floatRange()), isIf,
                                                                        (commandContext) -> checkBodyDamage(commandContext))))))
                                .then(Commands.literal("status")
                                        .then(Commands.argument("body_part", StringArgumentType.word())
                                                .then(Commands.argument("key", StringArgumentType.word())
                                                        .then(Commands.literal("matches")
                                                                .then(addBodyConditional(parent, Commands.argument("value", RangeArgument.intRange()), isIf,
                                                                        (commandContext) -> checkBodyStatus(commandContext)))))))
                                .then(Commands.literal("float")
                                        .then(Commands.argument("body_part", StringArgumentType.word())
                                                .then(Commands.argument("key", StringArgumentType.word())
                                                        .then(Commands.literal("matches")
                                                                .then(addBodyConditional(parent, Commands.argument("range", RangeArgument.floatRange()), isIf,
                                                                        (commandContext) -> checkBodyFloat(commandContext)))))))
                                .then(Commands.literal("string")
                                        .then(Commands.argument("body_part", StringArgumentType.word())
                                                .then(Commands.argument("key", StringArgumentType.word())
                                                        .then(Commands.literal("matches")
                                                                .then(addBodyConditional(parent, Commands.argument("value", StringArgumentType.string()), isIf,
                                                                        (commandContext) -> checkBodyString(commandContext))))))))));
    }

    // Helper method to create conditional (similar to existing addConditional method)
    @Unique
    private static ArgumentBuilder<CommandSourceStack, ?> addBodyConditional(
            CommandNode<CommandSourceStack> commandNode,
            ArgumentBuilder<CommandSourceStack, ?> builder,
            boolean value,
            CommandPredicate test) {

        return builder.fork(commandNode, (commandContext) -> {
            try {
                boolean result = test.test(commandContext);
                return value == result ?
                        Collections.singleton((CommandSourceStack) commandContext.getSource()) :
                        Collections.emptyList();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }).executes((commandContext) -> {
            try {
                if (value == test.test(commandContext)) {
                    ((CommandSourceStack) commandContext.getSource()).sendSuccess(() -> {
                        return Component.translatable("commands.execute.conditional.pass");
                    }, false);
                    return 1;
                } else {
                    throw new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail")).create();
                }
            } catch (Exception e) {
                throw new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail")).create();
            }
        });
    }

    // Body check implementations
    @Unique
    private static boolean checkBodyDamage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if (!(target instanceof ServerPlayer player)) return false;

        String bodyPart = StringArgumentType.getString(context, "body_part");
        MinMaxBounds.Doubles range = RangeArgument.Floats.getRange(context, "range");

        return range.matches(BodyStatusHelper.getDamage(player, bodyPart));
    }

    @Unique
    private static boolean checkBodyStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if (!(target instanceof ServerPlayer player)) return false;

        String bodyPart = StringArgumentType.getString(context, "body_part");
        String key = StringArgumentType.getString(context, "key");
        MinMaxBounds.Ints expectedValue = RangeArgument.Ints.getRange(context, "value");

        int actualValue = BodyStatusHelper.getCustomStatus(player, bodyPart, key);
        return expectedValue.equals(actualValue);
    }

    @Unique
    private static boolean checkBodyFloat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if (!(target instanceof ServerPlayer player)) return false;
        String bodyPart = StringArgumentType.getString(context, "body_part");
        String key = StringArgumentType.getString(context, "key");
        MinMaxBounds.Doubles range = RangeArgument.Floats.getRange(context, "range");

        double floatValue = BodyStatusHelper.getCustomFloat(player, bodyPart, key);
        return range.matches(floatValue);
    }

    @Unique
    private static boolean checkBodyString(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        if (!(target instanceof ServerPlayer player)) return false;

        String bodyPart = StringArgumentType.getString(context, "body_part");
        String key = StringArgumentType.getString(context, "key");
        String expectedValue = StringArgumentType.getString(context, "value");

        String actualValue = BodyStatusHelper.getCustomString(player, bodyPart, key);
        return expectedValue.equals(actualValue);
    }
}
