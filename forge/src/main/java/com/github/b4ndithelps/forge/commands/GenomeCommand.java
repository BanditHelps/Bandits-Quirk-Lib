package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.OpenGeneGraphS2CPacket;
import net.minecraftforge.network.PacketDistributor;
import com.github.b4ndithelps.genetics.GeneCombinationService;
import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.values.GeneticsConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GenomeCommand {
    private static final SuggestionProvider<CommandSourceStack> GENE_ID_SUGGESTIONS = (context, builder) -> {
        try {
            return SharedSuggestionProvider.suggestResource(
                GeneRegistry.all().stream().map(Gene::getId).toList(),
                builder
            );
        } catch (Throwable t) {
            return SharedSuggestionProvider.suggestResource(java.util.List.<ResourceLocation>of(), builder);
        }
    };
    private static final SuggestionProvider<CommandSourceStack> PLAYER_GENE_ID_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            ListTag genome = GenomeHelper.getGenome(player);
            java.util.List<ResourceLocation> ids = new java.util.ArrayList<>();
            for (int i = 0; i < genome.size(); i++) {
                CompoundTag g = genome.getCompound(i);
                if (g.contains("id", 8)) {
                    try { ids.add(new ResourceLocation(g.getString("id"))); } catch (Exception ignored) {}
                }
            }
            return SharedSuggestionProvider.suggestResource(ids, builder);
        } catch (Throwable t) {
            return SharedSuggestionProvider.suggestResource(java.util.List.<ResourceLocation>of(), builder);
        }
    };
    

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("genome")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("graph")
                    .executes(ctx -> {
                        var src = ctx.getSource();
                        var player = src.getPlayerOrException();
                        var server = src.getServer();
                        java.util.List<net.minecraft.resources.ResourceLocation> builderOrder = com.github.b4ndithelps.genetics.GeneCombinationService.getOrCreateBuilderGenes(server);
                        BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenGeneGraphS2CPacket(builderOrder));
                        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Opening gene graph..."), false);
                        return 1;
                    }))
                .then(Commands.literal("list")
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
                        .then(Commands.argument("gene_id", ResourceLocationArgument.id()).suggests(GENE_ID_SUGGESTIONS)
                            .then(Commands.argument("quality", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "gene_id");
                                    String qRaw = StringArgumentType.getString(ctx, "quality");
                                    int q;
                                    try { q = parseQuality(qRaw); } catch (NumberFormatException nfe) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid quality: " + qRaw));
                                        return 0;
                                    }
                                    // If the player already has this gene, overwrite its quality instead of adding a duplicate
                                    ListTag genome = GenomeHelper.getGenome(player);
                                    for (int i = 0; i < genome.size(); i++) {
                                        CompoundTag existing = genome.getCompound(i);
                                        if (id.toString().equals(existing.getString("id"))) {
                                            existing.putInt("quality", q);
                                            GenomeHelper.syncToClient(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Updated gene quality"), false);
                                            return 1;
                                        }
                                    }
                                    CompoundTag g = new CompoundTag();
                                    g.putString("id", id.toString());
                                    g.putInt("quality", q);
                                    boolean ok = GenomeHelper.addGene(player, g);
                                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Added gene" : "Failed to add (max reached)"), false);
                                    return ok ? 1 : 0;
                                })))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("*")
                            .executes(ctx -> {
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                GenomeHelper.clear(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Cleared genome"), false);
                                return 1;
                            }))
                        .then(Commands.argument("gene_id", ResourceLocationArgument.id()).suggests(PLAYER_GENE_ID_SUGGESTIONS)
                            .executes(ctx -> {
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                ResourceLocation id = ResourceLocationArgument.getId(ctx, "gene_id");
                                boolean ok = GenomeHelper.removeGeneById(player, id.toString());
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
                .then(Commands.literal("vial")
                    .then(Commands.argument("gene_id", ResourceLocationArgument.id()).suggests(GENE_ID_SUGGESTIONS)
                        .then(Commands.argument("quality", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer target = ctx.getSource().getPlayerOrException();
                                String qRaw = StringArgumentType.getString(ctx, "quality");
                                int q;
                                try { q = parseQuality(qRaw); } catch (NumberFormatException nfe) {
                                    ctx.getSource().sendFailure(Component.literal("Invalid quality: " + qRaw));
                                    return 0;
                                }
                                return giveVial(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "gene_id").toString(), q);
                            })
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    String qRaw = StringArgumentType.getString(ctx, "quality");
                                    int q;
                                    try { q = parseQuality(qRaw); } catch (NumberFormatException nfe) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid quality: " + qRaw));
                                        return 0;
                                    }
                                    return giveVial(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "gene_id").toString(), q);
                                })))))
        );
    }

    private static int parseQuality(String raw) throws NumberFormatException {
        if (raw == null) throw new NumberFormatException("null");
        String s = raw.trim();
        if (s.endsWith("%")) s = s.substring(0, s.length() - 1);
        int v = Integer.parseInt(s);
        if (v < 0) v = 0; if (v > 100) v = 100;
        return v;
    }

    private static int giveVial(CommandSourceStack source, ServerPlayer target, String geneIdStr, int quality) {
        ResourceLocation id;
        try { id = new ResourceLocation(geneIdStr); } catch (Exception e) {
            source.sendFailure(Component.literal("Invalid gene id: " + geneIdStr));
            return 0;
        }
        Gene gene = GeneRegistry.get(id);
        if (gene == null) {
            source.sendFailure(Component.literal("Unknown gene: " + geneIdStr));
            return 0;
        }

        // Pick vial item by gene category
        Item vialItem = switch (gene.getCategory()) {
            case resistance -> ModItems.GENE_VIAL_RESISTANCE.get();
            case builder -> ModItems.GENE_VIAL_BUILDER.get();
            case cosmetic -> ModItems.GENE_VIAL_COSMETIC.get();
            case lowend -> ModItems.GENE_VIAL_LOWEND.get();
            case quirk -> ModItems.GENE_VIAL_QUIRK.get();
        };
        ItemStack vial = new ItemStack(vialItem);
        CompoundTag tag = vial.getOrCreateTag();
        CompoundTag g = new CompoundTag();
        g.putString("id", id.toString());
        g.putInt("quality", Math.max(0, Math.min(100, quality)));
        tag.put("gene", g);

        boolean added = target.addItem(vial);
        if (!added) {
            target.drop(vial, false);
        }
        source.sendSuccess(() -> Component.literal("Given gene vial: " + id + " (" + quality + "%) to " + target.getName().getString()), false);
        return 1;
    }
}


