package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.github.b4ndithelps.values.GeneticsConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class GenomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("genome")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("get")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            ListTag genome = GenomeHelper.getGenome(player);
                            int n = genome.size();
                            ctx.getSource().sendSuccess(() -> Component.literal("Genome[" + n + "/" + GeneticsConstants.PLAYER_MAX_GENES + "]"), false);
                            for (int i = 0; i < n; i++) {
                                CompoundTag g = genome.getCompound(i);
                                String id = g.getString("id");
                                String name = g.contains("name", 8) ? g.getString("name") : id;
                                Integer q = g.contains("quality", 3) ? g.getInt("quality") : null;
                                String line = " - " + (name == null ? id : name) + (q != null ? (" (" + q + "%)") : "");
                                ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                            }
                            return 1;
                        })))
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("gene_id", StringArgumentType.string())
                            .then(Commands.argument("quality", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                    String id = StringArgumentType.getString(ctx, "gene_id");
                                    int q = IntegerArgumentType.getInteger(ctx, "quality");
                                    CompoundTag g = new CompoundTag();
                                    g.putString("id", id);
                                    g.putInt("quality", q);
                                    boolean ok = GenomeHelper.addGene(player, g);
                                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Added gene" : "Failed to add (max reached)"), false);
                                    return ok ? 1 : 0;
                                })))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("gene_id", StringArgumentType.string())
                            .executes(ctx -> {
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                String id = StringArgumentType.getString(ctx, "gene_id");
                                boolean ok = GenomeHelper.removeGeneById(player, id);
                                ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Removed" : "Not found"), false);
                                return ok ? 1 : 0;
                            }))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            GenomeHelper.clear(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared genome"), false);
                            return 1;
                        })))
        );
    }
}


