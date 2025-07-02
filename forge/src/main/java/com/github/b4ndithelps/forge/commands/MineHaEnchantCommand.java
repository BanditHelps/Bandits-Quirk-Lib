package com.github.b4ndithelps.forge.commands;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

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
        ENCHANTMENT_MAPPINGS.put("tool_type", toolEnchants);

        // Chestplate type enchantments
        Map<String, Enchantment> chestplateEnchants = new HashMap<>();
        chestplateEnchants.put("creation_e1", Enchantments.ALL_DAMAGE_PROTECTION);
        chestplateEnchants.put("creation_e2", Enchantments.FIRE_PROTECTION);
        chestplateEnchants.put("creation_e3", Enchantments.BLAST_PROTECTION);
        chestplateEnchants.put("creation_e4", Enchantments.PROJECTILE_PROTECTION);
        chestplateEnchants.put("creation_e5", Enchantments.THORNS);
        chestplateEnchants.put("creation_e6", Enchantments.MENDING);
        chestplateEnchants.put("creation_e7", Enchantments.UNBREAKING);
        chestplateEnchants.put("creation_e8", Enchantments.BINDING_CURSE);
        ENCHANTMENT_MAPPINGS.put("chestplate_type", chestplateEnchants);

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

        //Parse the JSON object for enchant values
        JsonObject enchantValues;
        try {
            enchantValues = JsonParser.parseString(enchantValuesJson).getAsJsonObject();
        } catch (Exception e ) {
            throw INVALID_JSON.create();
        }

        ItemStack itemStack = new ItemStack(item);

        // Apply custom name if provided
        if (customName != null && !customName.isEmpty()) {
            itemStack.setHoverName(Component.literal(customName));
        }

        // Apply enchantments
        Map<String, Enchantment> typeEnchantments = ENCHANTMENT_MAPPINGS.get(type);
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

        // Give it to the player
        if (!player.getInventory().add(itemStack)) {
            player.drop(itemStack, false);
        }

        // Send success message
        String itemName = customName != null ? customName : item.getDescription().getString();
        int finalAppliedEnchants = appliedEnchants;
        source.sendSuccess(() -> Component.literal(
                String.format("Gave %s a custom %s with %d enchantments!",
                        player.getName().getString(), itemName, finalAppliedEnchants)), true);

        return 1;
    }
}
