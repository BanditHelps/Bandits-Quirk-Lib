package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.config.ConfigHelper;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import com.google.gson.JsonObject;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.github.b4ndithelps.values.CreationShopConstants.*;
import static com.github.b4ndithelps.values.CreationShopConstants.BIT_MAP_3_TABLE;

public class MineHaEnchantCommand {

    private static final SimpleCommandExceptionType INVALID_ITEM = new SimpleCommandExceptionType(
            Component.literal("Invalid item ID")
    );

    private static final SimpleCommandExceptionType INVALID_JSON = new SimpleCommandExceptionType(
            Component.literal("Invalid JSON format for enchantment values")
    );

    private static final SimpleCommandExceptionType INVALID_TYPE = new SimpleCommandExceptionType(
            Component.literal("Invalid enchantment type")
    );

    // Define enchantment mappings for different types
    private static final Map<String, Map<String, Enchantment>> ENCHANTMENT_MAPPINGS = new HashMap<>();

    static {
        // Tool type enchantments
        Map<String, Enchantment> toolEnchants = new HashMap<>();
        toolEnchants.put("creation_e1", Enchantments.UNBREAKING);
        toolEnchants.put("creation_e2", Enchantments.SILK_TOUCH);
        toolEnchants.put("creation_e3", Enchantments.BLOCK_EFFICIENCY);
        toolEnchants.put("creation_e4", Enchantments.SHARPNESS);
        toolEnchants.put("creation_e5", Enchantments.MENDING);
        toolEnchants.put("creation_e6", Enchantments.BLOCK_FORTUNE);
        ENCHANTMENT_MAPPINGS.put("tool_type", toolEnchants);

        // Sword type enchantments
        Map<String, Enchantment> swordEnchants = new HashMap<>();
        swordEnchants.put("creation_e1", Enchantments.UNBREAKING);
        swordEnchants.put("creation_e2", Enchantments.SMITE);
        swordEnchants.put("creation_e3", Enchantments.KNOCKBACK);
        swordEnchants.put("creation_e4", Enchantments.SWEEPING_EDGE);
        swordEnchants.put("creation_e5", Enchantments.FIRE_ASPECT);
        swordEnchants.put("creation_e6", Enchantments.SHARPNESS);
        swordEnchants.put("creation_e7", Enchantments.MENDING);
        swordEnchants.put("creation_e8", Enchantments.MOB_LOOTING);
        ENCHANTMENT_MAPPINGS.put("sword_type", swordEnchants);

        // Utility type enchantments
        Map<String, Enchantment> utilityEnchants = new HashMap<>();
        utilityEnchants.put("creation_e1", Enchantments.UNBREAKING);
        utilityEnchants.put("creation_e2", Enchantments.MENDING);
        ENCHANTMENT_MAPPINGS.put("utility_type", utilityEnchants);

        // Ranged type enchantments
        Map<String, Enchantment> rangedEnchants = new HashMap<>();
        rangedEnchants.put("creation_e1", Enchantments.UNBREAKING);
        rangedEnchants.put("creation_e2", Enchantments.PUNCH_ARROWS);
        rangedEnchants.put("creation_e3", Enchantments.PIERCING);
        rangedEnchants.put("creation_e4", Enchantments.FLAMING_ARROWS);
        rangedEnchants.put("creation_e5", Enchantments.POWER_ARROWS);
        rangedEnchants.put("creation_e6", Enchantments.MULTISHOT);
        rangedEnchants.put("creation_e7", Enchantments.QUICK_CHARGE);
        rangedEnchants.put("creation_e8", Enchantments.MENDING);
        ENCHANTMENT_MAPPINGS.put("ranged_type", rangedEnchants);

        // Helmet type enchantments -
        Map<String, Enchantment> helmetEnchants = new HashMap<>();
        helmetEnchants.put("creation_e1", Enchantments.MENDING);
        helmetEnchants.put("creation_e2", Enchantments.UNBREAKING);
        helmetEnchants.put("creation_e3", Enchantments.ALL_DAMAGE_PROTECTION);
        helmetEnchants.put("creation_e4", Enchantments.THORNS);
        helmetEnchants.put("creation_e5", Enchantments.PROJECTILE_PROTECTION);
        helmetEnchants.put("creation_e7", Enchantments.RESPIRATION);
        helmetEnchants.put("creation_e6", Enchantments.FIRE_PROTECTION);
        helmetEnchants.put("creation_e8", Enchantments.AQUA_AFFINITY);
        helmetEnchants.put("creation_e9", Enchantments.BLAST_PROTECTION);
        ENCHANTMENT_MAPPINGS.put("helmet_type", helmetEnchants);

        // Chestplate type enchantments - verified
        Map<String, Enchantment> chestplateEnchants = new HashMap<>();
        chestplateEnchants.put("creation_e1", Enchantments.MENDING);
        chestplateEnchants.put("creation_e2", Enchantments.UNBREAKING);
        chestplateEnchants.put("creation_e3", Enchantments.ALL_DAMAGE_PROTECTION);
        chestplateEnchants.put("creation_e4", Enchantments.THORNS);
        chestplateEnchants.put("creation_e5", Enchantments.PROJECTILE_PROTECTION);
        chestplateEnchants.put("creation_e6", Enchantments.FIRE_PROTECTION);
        chestplateEnchants.put("creation_e9", Enchantments.BLAST_PROTECTION);
        ENCHANTMENT_MAPPINGS.put("chestplate_type", chestplateEnchants);

        // Leggings type enchantments - Verified
        Map<String, Enchantment> leggingsEnchants = new HashMap<>();
        leggingsEnchants.put("creation_e1", Enchantments.MENDING);
        leggingsEnchants.put("creation_e2", Enchantments.UNBREAKING);
        leggingsEnchants.put("creation_e3", Enchantments.ALL_DAMAGE_PROTECTION);
        leggingsEnchants.put("creation_e4", Enchantments.THORNS);
        leggingsEnchants.put("creation_e5", Enchantments.PROJECTILE_PROTECTION);
        leggingsEnchants.put("creation_e7", Enchantments.SWIFT_SNEAK);
        leggingsEnchants.put("creation_e6", Enchantments.FIRE_PROTECTION);
        leggingsEnchants.put("creation_e9", Enchantments.BLAST_PROTECTION);
        ENCHANTMENT_MAPPINGS.put("leggings_type", leggingsEnchants);

        // Boots type enchantments - Verified
        Map<String, Enchantment> bootsEnchants = new HashMap<>();
        bootsEnchants.put("creation_e1", Enchantments.MENDING);
        bootsEnchants.put("creation_e2", Enchantments.UNBREAKING);
        bootsEnchants.put("creation_e3", Enchantments.ALL_DAMAGE_PROTECTION);
        bootsEnchants.put("creation_e4", Enchantments.THORNS);
        bootsEnchants.put("creation_e5", Enchantments.PROJECTILE_PROTECTION);
        bootsEnchants.put("creation_e7", Enchantments.FALL_PROTECTION);
        bootsEnchants.put("creation_e6", Enchantments.FIRE_PROTECTION);
        bootsEnchants.put("creation_e8", Enchantments.DEPTH_STRIDER);
        bootsEnchants.put("creation_e9", Enchantments.BLAST_PROTECTION);
        bootsEnchants.put("creation_e10", Enchantments.SOUL_SPEED);
        bootsEnchants.put("creation_e11", Enchantments.FROST_WALKER);
        ENCHANTMENT_MAPPINGS.put("boots_type", bootsEnchants);

        // Weapon type enchantments
        Map<String, Enchantment> weaponEnchants = new HashMap<>();
        weaponEnchants.put("creation_e1", Enchantments.SHARPNESS);
        weaponEnchants.put("creation_e2", Enchantments.SMITE);
        weaponEnchants.put("creation_e3", Enchantments.BANE_OF_ARTHROPODS);
        weaponEnchants.put("creation_e4", Enchantments.KNOCKBACK);
        weaponEnchants.put("creation_e5", Enchantments.FIRE_ASPECT);
        weaponEnchants.put("creation_e6", Enchantments.MOB_LOOTING);
        weaponEnchants.put("creation_e7", Enchantments.SWEEPING_EDGE);
        weaponEnchants.put("creation_e8", Enchantments.MENDING);
        weaponEnchants.put("creation_e9", Enchantments.UNBREAKING);
        ENCHANTMENT_MAPPINGS.put("weapon_type", weaponEnchants);

        // Bow type enchantments
        Map<String, Enchantment> bowEnchants = new HashMap<>();
        bowEnchants.put("creation_e1", Enchantments.POWER_ARROWS);
        bowEnchants.put("creation_e2", Enchantments.PUNCH_ARROWS);
        bowEnchants.put("creation_e3", Enchantments.FLAMING_ARROWS);
        bowEnchants.put("creation_e4", Enchantments.INFINITY_ARROWS);
        bowEnchants.put("creation_e5", Enchantments.MENDING);
        bowEnchants.put("creation_e6", Enchantments.UNBREAKING);
        ENCHANTMENT_MAPPINGS.put("bow_type", bowEnchants);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mineha_enchant")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.string())
                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                .then(Commands.argument("custom_name", StringArgumentType.string())
                                .then(Commands.argument("enchant_values", StringArgumentType.greedyString())
                                                .executes(context -> executeCommand(context,
                                                        StringArgumentType.getString(context, "custom_name"))))))
        ));
    }

    private static int executeCommand(CommandContext<CommandSourceStack> context, String customName) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        String type = StringArgumentType.getString(context, "type");
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item_id");
        String enchantValuesJson = StringArgumentType.getString(context, "enchant_values");

        // Validate enchantment type
        if (!ENCHANTMENT_MAPPINGS.containsKey(type)) {
            throw INVALID_TYPE.create();
        }

        // Get item from registry
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            throw INVALID_ITEM.create();
        }

        // Check the scoreboard bitmap to see if the player has learned the item
        // Determine which bitmap table contains the item
        try {
            Map<String, Integer> valueTable = null;
            int foundBitMap = 0;
            String itemKey = itemId.toString();

            if (BIT_MAP_1_TABLE.containsKey(itemKey)) {
                valueTable = BIT_MAP_1_TABLE;
                foundBitMap = 1;
            } else if (BIT_MAP_2_TABLE.containsKey(itemKey)) {
                valueTable = BIT_MAP_2_TABLE;
                foundBitMap = 2;
            } else if (BIT_MAP_3_TABLE.containsKey(itemKey)) {
                valueTable = BIT_MAP_3_TABLE;
                foundBitMap = 3;
            } else {
                player.sendSystemMessage(Component.literal("Something is wrong with that item, please check the config"));
                return 0;
            }

            // Check if already learned
            int bitMapScore = getPlayerScore(player, "MineHa.Creation.BitMap" + foundBitMap);
            int itemValue = valueTable.get(itemKey);

            if ((bitMapScore & itemValue) != itemValue) {
                player.sendSystemMessage(Component.literal("You don't know this recipe!"));
                return 1;
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error with enchant command: " + e.getMessage());
        }


        //Parse the JSON object for enchant values
        JsonObject enchantValues;
        try {
            enchantValues = JsonParser.parseString(enchantValuesJson).getAsJsonObject();
        } catch (Exception e ) {
            throw INVALID_JSON.create();
        }

        // Calculate total cost: base item cost + sum of selected enchant costs
        int totalCost = 0;
        String itemKeyForCost = itemId.toString();
        totalCost += ConfigHelper.getItemBuyCost(itemKeyForCost);

        Map<String, Enchantment> typeEnchantments = ENCHANTMENT_MAPPINGS.get(type);
        for (String key : enchantValues.keySet()) {
            if (typeEnchantments.containsKey(key)) {
                int level = enchantValues.get(key).getAsInt();
                if (level > 0) {
                    Enchantment enchantment = typeEnchantments.get(key);
                    ResourceLocation enchantmentId = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
                    if (enchantmentId != null) {
                        int perLevelCost = ConfigHelper.getEnchantBuyCost(enchantmentId.toString());
                        totalCost += perLevelCost * level;
                    }
                }
            }
        }

        // Check lipids balance and deduct if sufficient
        int lipids = (int) BodyStatusHelper.getCustomFloat(player, "head", "creation_lipids");
        if (lipids < totalCost) {
            player.sendSystemMessage(Component.literal(
                    String.format("You don't have enough lipids! You need %d but you only have %d", totalCost, lipids)));
            return 0;
        }
        BodyStatusHelper.setCustomFloat(player, "head", "creation_lipids", lipids - totalCost);

        // Use stamina equal to the totalCost * (cost multiplier)
        StaminaHelper.useStamina(player, (int)(ConfigHelper.getCreationStaminaCost() * totalCost));

        // Create item stack and apply name/enchantments
        ItemStack itemStack = new ItemStack(item);
        if (customName != null && !customName.isEmpty() && !customName.equals("0")) {
            itemStack.setHoverName(Component.literal(customName));
        }
        int appliedEnchants = 0;
        for (String key : enchantValues.keySet()) {
            if (typeEnchantments.containsKey(key)) {
                Enchantment enchantment = typeEnchantments.get(key);
                int level = enchantValues.get(key).getAsInt();
                if (level > 0) {
                    itemStack.enchant(enchantment, level);
                    appliedEnchants++;
                }
            }
        }

        // Spawn the created item entity with some visual flair
        spawnCreatedItem(player, itemStack);

        // Send success message
        String itemName = ((customName != null) && !customName.equals("0")) ? customName : item.getDescription().getString();
        int finalAppliedEnchants = appliedEnchants;
        int finalTotalCost = totalCost;
        source.sendSuccess(() -> Component.literal(
                String.format("Created a %s with %d enchantments for %d lipids!",
                        itemName, finalAppliedEnchants, finalTotalCost)), true);

        return 1;
    }

    // TODO Make this in a common lib because MineHaCreationCommand uses it
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

    @SuppressWarnings("removal")
    private static void spawnCreatedItem(ServerPlayer player, ItemStack itemStack) {
        try {
            ServerLevel level = player.serverLevel();

            Vec3 playerPos = player.position();
            double spawnX = playerPos.x + (Math.random() - 0.5) * 1.0;
            double spawnY = playerPos.y + 1.0 + (Math.random() - 0.5) * 0.5;
            double spawnZ = playerPos.z + (Math.random() - 0.5) * 1.0;

            ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, itemStack);

            Random random = new Random();
            double motionX = (random.nextDouble() - 0.5) * 0.3;
            double motionY = 0.2 + random.nextDouble() * 0.1;
            double motionZ = (random.nextDouble() - 0.5) * 0.3;
            itemEntity.setDeltaMovement(motionX, motionY, motionZ);

            itemEntity.setPickUpDelay(20);

            level.addFreshEntity(itemEntity);

            level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.8f, 0.0f), 1.0f),
                    spawnX, spawnY, spawnZ, 15, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(ParticleTypes.ENCHANT,
                    spawnX, spawnY, spawnZ, 10, 0.2, 0.2, 0.2, 0.05);

        } catch (Exception e) {
            System.err.println("Error spawning created enchanted item: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
