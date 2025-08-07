package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.values.CreationShopConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static com.github.b4ndithelps.values.CreationShopConstants.*;

public class MineHaCreationCommand {

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(new String[]{"buy", "shop", "learn", "enchant"}, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mineha_creation")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.string())
                        .suggests(MODE_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                                .then(Commands.argument("cost", IntegerArgumentType.integer())
                                        .executes(MineHaCreationCommand::execute)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String mode = StringArgumentType.getString(context, "mode");

            // Check if player has creation superpower
            if (!hasCreationSuperpower(player)) {
                player.sendSystemMessage(Component.literal("You don't have the Creation Quirk!"));
                return 0;
            }

            switch (mode.toLowerCase()) {
                case "buy":
                    return executeBuyMode(context, player);
                case "shop":
                    return executeShopMode(player);
                case "learn":
                    return executeLearnMode(context, player);
                case "enchant":
                    return executeEnchantMode(context, player);
                default:
                    player.sendSystemMessage(Component.literal("Invalid mode! Use: buy, shop, or learn"));
                    return 0;
            }
        } catch (Exception e) {
            System.err.println("Error in mineha_creation command: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    private static int executeBuyMode(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String itemId = StringArgumentType.getString(context, "item");
        String slotInfo = "";

        if (itemId.startsWith("slot")) {
            String slotId = itemId.split("\\.")[1];
            String slotKey = "MineHa.Slot." + slotId;

            slotInfo = player.getPersistentData().getString(slotKey);
        }

        // If something is in the slot, it will show up here and can use it
        if (!slotInfo.isEmpty()) {
            itemId = slotInfo;
        }

        Integer cost = ITEM_BUY_COST_TABLE.get(itemId);

        if (cost == null) {
            player.sendSystemMessage(Component.literal("Unknown item: " + itemId));
            return 0;
        }

        // Check lipids
        int lipids = getPlayerScore(player, "MineHa.Creation.Lipids");
        if (lipids < cost) {
            player.sendSystemMessage(Component.literal(
                    String.format("You don't have enough lipids! You need %d but you only have %d", cost, lipids)));
            return 0;
        }

        // Deduct lipids
        setPlayerScore(player, "MineHa.Creation.Lipids", lipids - cost);

        // Create and spawn item
        spawnCreatedItem(player, itemId);

        return 1;
    }

    private static int executeShopMode(ServerPlayer player) {
        // You'll need to implement your GUI opening logic here
        // This depends on how your mod handles GUIs
        player.sendSystemMessage(Component.literal("Opening creation shop... (GUI implementation needed)"));
        return 1;
    }

    @SuppressWarnings("removal")
    private static int executeLearnMode(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String itemId = StringArgumentType.getString(context, "item");
        int cost = IntegerArgumentType.getInteger(context, "cost");

        // If the player is in creative mode, make the cost 0 so they can insta-learn it
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            cost = 0;
        }

        // Determine which bitmap table contains the item
        Map<String, Integer> valueTable = null;
        int foundBitMap = 0;

        if (BIT_MAP_1_TABLE.containsKey(itemId)) {
            valueTable = BIT_MAP_1_TABLE;
            foundBitMap = 1;
        } else if (BIT_MAP_2_TABLE.containsKey(itemId)) {
            valueTable = BIT_MAP_2_TABLE;
            foundBitMap = 2;
        } else if (BIT_MAP_3_TABLE.containsKey(itemId)) {
            valueTable = BIT_MAP_3_TABLE;
            foundBitMap = 3;
        } else if (BIT_MAP_4_TABLE.containsKey(itemId)) {
            valueTable = BIT_MAP_4_TABLE;
            foundBitMap = 4;
        } else if (BIT_MAP_5_TABLE.containsKey(itemId)) {
            valueTable = BIT_MAP_5_TABLE;
            foundBitMap = 5;
        } else {
            player.sendSystemMessage(Component.literal("Something is wrong with that item, please check the config"));
            return 0;
        }

        // Check if already learned
        int bitMapScore = getPlayerScore(player, "MineHa.Creation.BitMap" + foundBitMap);
        int itemValue = valueTable.get(itemId);

        if ((bitMapScore & itemValue) == itemValue) {
            player.sendSystemMessage(Component.literal("You already know this recipe!"));
            return 1;
        }

        // Count items in inventory
        int totalItemCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId))) {
                totalItemCount += stack.getCount();
            }
        }

        if (totalItemCount < cost) {
            player.sendSystemMessage(Component.literal(
                    String.format("You don't have enough of that item! You need %d but you only have %d", cost, totalItemCount)));
            return 0;
        }

        // Remove items from inventory
        int remainingToRemove = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId))) {
                int toRemove = Math.min(stack.getCount(), remainingToRemove);
                stack.shrink(toRemove);
                remainingToRemove -= toRemove;
            }
        }

        // Learn the recipe (update bitmap)
        int newScore = bitMapScore + itemValue;
        setPlayerScore(player, "MineHa.Creation.BitMap" + foundBitMap, newScore);

        player.sendSystemMessage(Component.literal(
                String.format("Successfully learned %s! Removed %d items from inventory.", itemId, cost)));

        return 1;
    }

    @SuppressWarnings("removal")
    private static int executeEnchantMode(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String enchantmentId = StringArgumentType.getString(context, "item");
        int cost = IntegerArgumentType.getInteger(context, "cost");

        // Determine which bitmap table contains the item
        Map<String, Integer> valueTable = null;
        int foundBitMap = 0;

        if (BIT_MAP_4_TABLE.containsKey(enchantmentId)) {
            valueTable = BIT_MAP_4_TABLE;
            foundBitMap = 4;
        } else if (BIT_MAP_5_TABLE.containsKey(enchantmentId)) {
            valueTable = BIT_MAP_5_TABLE;
            foundBitMap = 5;
        } else {
            player.sendSystemMessage(Component.literal("Something is wrong with that enchantment, please check the config"));
            return 0;
        }

        // Check if already learned
        int bitMapScore = getPlayerScore(player, "MineHa.Creation.BitMap" + foundBitMap);
        int itemValue = valueTable.get(enchantmentId);

        if ((bitMapScore & itemValue) == itemValue) {
            player.sendSystemMessage(Component.literal("You already know this enchantment!"));
            return 1;
        }

        // Get the enchantment we're looking for
        ResourceLocation enchantLoc = new ResourceLocation(enchantmentId);
        Enchantment targetEnchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantLoc);

        if (targetEnchantment == null) {
            player.sendSystemMessage(Component.literal("Invalid enchantment ID: " + enchantmentId));
            return 0;
        }

        // Count enchanted books with the target enchantment in inventory
        int totalBookCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            // Check if it's an enchanted book
            if (stack.getItem() == Items.ENCHANTED_BOOK) {
                // Get the enchantments on the book
                ListTag enchantmentList = EnchantedBookItem.getEnchantments(stack);

                // Check if this book has our target enchantment
                for (int j = 0; j < enchantmentList.size(); j++) {
                    CompoundTag enchantmentTag = enchantmentList.getCompound(j);
                    String bookEnchantId = enchantmentTag.getString("id");

                    if (bookEnchantId.equals(enchantmentId)) {
                        totalBookCount += stack.getCount();
                        break; // Found the enchantment on this book, no need to check other enchantments
                    }
                }
            }
        }

        if (totalBookCount < cost) {
            player.sendSystemMessage(Component.literal(
                    String.format("You don't have enough enchanted books with %s! You need %d but you only have %d",
                            enchantmentId, cost, totalBookCount)));
            return 0;
        }

        // Remove enchanted books with the target enchantment from inventory
        int remainingToRemove = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);

            // Check if it's an enchanted book with our target enchantment
            if (stack.getItem() == Items.ENCHANTED_BOOK) {
                ListTag enchantmentList = EnchantedBookItem.getEnchantments(stack);

                // Check if this book has our target enchantment
                boolean hasTargetEnchantment = false;
                for (int j = 0; j < enchantmentList.size(); j++) {
                    CompoundTag enchantmentTag = enchantmentList.getCompound(j);
                    String bookEnchantId = enchantmentTag.getString("id");

                    if (bookEnchantId.equals(enchantmentId)) {
                        hasTargetEnchantment = true;
                        break;
                    }
                }

                if (hasTargetEnchantment) {
                    int toRemove = Math.min(stack.getCount(), remainingToRemove);
                    stack.shrink(toRemove);
                    remainingToRemove -= toRemove;
                }
            }
        }

        // Learn the recipe (update bitmap)
        int newScore = bitMapScore + itemValue;
        setPlayerScore(player, "MineHa.Creation.BitMap" + foundBitMap, newScore);

        player.sendSystemMessage(Component.literal(
                String.format("Successfully learned %s! Removed %d enchanted books from inventory.", enchantmentId, cost)));

        return 1;
    }

    @SuppressWarnings("removal")
    private static void spawnCreatedItem(ServerPlayer player, String itemId) {
        try {
            ServerLevel level = player.serverLevel();

            // Get player position
            Vec3 playerPos = player.position();
            double spawnX = playerPos.x + (Math.random() - 0.5) * 1.0;
            double spawnY = playerPos.y + 1.0 + (Math.random() - 0.5) * 0.5;
            double spawnZ = playerPos.z + (Math.random() - 0.5) * 1.0;

            // Create item stack
            Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId));
            if (item == null) {
                player.sendSystemMessage(Component.literal("Invalid item: " + itemId));
                return;
            }

            ItemStack itemStack = new ItemStack(item, 1);

            // Create item entity
            ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, itemStack);

            // Set motion
            Random random = new Random();
            double motionX = (random.nextDouble() - 0.5) * 0.3;
            double motionY = 0.2 + random.nextDouble() * 0.1;
            double motionZ = (random.nextDouble() - 0.5) * 0.3;
            itemEntity.setDeltaMovement(motionX, motionY, motionZ);

            // Set pickup delay
            itemEntity.setPickUpDelay(20);

            // Spawn entity
            level.addFreshEntity(itemEntity);

            // Add particle effects
            level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.8f, 0.0f), 1.0f),
                    spawnX, spawnY, spawnZ, 15, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(ParticleTypes.ENCHANT,
                    spawnX, spawnY, spawnZ, 10, 0.2, 0.2, 0.2, 0.05);

        } catch (Exception e) {
            System.err.println("Error spawning created item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean hasCreationSuperpower(ServerPlayer player) {
        return true; // Replace with actual superpower check
    }

    private static int getPlayerScore(ServerPlayer player, String objectiveName) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        }

        Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static void setPlayerScore(ServerPlayer player, String objectiveName, int value) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(objectiveName, net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,
                    Component.literal(objectiveName), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        }

        Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        score.setScore(value);
    }
}
