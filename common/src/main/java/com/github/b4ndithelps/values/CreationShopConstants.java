package com.github.b4ndithelps.values;

import java.util.HashMap;
import java.util.Map;

/**
 * The following class contains constants available to the scripts
 * inside of the /data/mineha/kubejs_scripts/ folder.
 *
 * This class manages both buy costs and learn costs for the Creation Shop.
 * Buy costs are for purchasing items you already know how to make.
 * Learn costs are for learning new items/enchantments for the first time.
 */
public class CreationShopConstants {

    // ===== BIT MAPS FOR TRACKING UNLOCKED ITEMS =====
    // These are static and not configurable - they define which bit represents each item

    // Stores the bit values for the Creation Shop - Map 1 (Static - not configurable)
    public static final Map<String, Integer> BIT_MAP_1_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:coal", 1);
        put("minecraft:copper_ingot", 2);
        put("minecraft:iron_ingot", 4);
        put("minecraft:lapis_lazuli", 8);
        put("minecraft:redstone", 16);
        put("minecraft:gold_ingot", 32);
        put("minecraft:quartz", 64);
        put("minecraft:diamond", 128);
        put("minecraft:emerald", 256);
        put("minecraft:dirt", 512);
        put("minecraft:gravel", 1024);
        put("minecraft:sand", 2048);
        put("minecraft:grass_block", 4096);
        put("minecraft:red_sand", 8192);
        put("minecraft:cobblestone", 16384);
        put("minecraft:stone", 32768);
        put("minecraft:andesite", 65536);
        put("minecraft:granite", 131072);
        put("minecraft:diorite", 262144);
        put("minecraft:netherrack", 524288);
        put("minecraft:nether_bricks", 1048576);
        put("minecraft:blackstone", 2097152);
        put("minecraft:soul_sand", 4194304);
        put("minecraft:obsidian", 8388608);
        put("minecraft:deepslate", 16777216);
        put("minecraft:cobbled_deepslate", 33554432);
        put("minecraft:tuff", 67108864);
        put("minecraft:stone_bricks", 134217728);
        put("minecraft:calcite", 268435456);
        put("minecraft:amethyst_shard", 536870912);
        put("palladium:lead_ingot", 1073741824);
    }};

    // Stores the bit values for the Creation Shop - Map 2 (Static - not configurable)
    public static final Map<String, Integer> BIT_MAP_2_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:stone_axe", 1);
        put("minecraft:stone_pickaxe", 2);
        put("minecraft:stone_shovel", 4);
        put("minecraft:stone_hoe", 8);
        put("minecraft:stone_sword", 16);
        put("minecraft:iron_axe", 32);
        put("minecraft:iron_pickaxe", 64);
        put("minecraft:iron_shovel", 128);
        put("minecraft:iron_hoe", 256);
        put("minecraft:iron_sword", 512);
        put("minecraft:gold_axe", 1024);
        put("minecraft:gold_pickaxe", 2048);
        put("minecraft:gold_shovel", 4096);
        put("minecraft:gold_hoe", 8192);
        put("minecraft:gold_sword", 16384);
        put("minecraft:diamond_axe", 32768);
        put("minecraft:diamond_pickaxe", 65536);
        put("minecraft:diamond_shovel", 131072);
        put("minecraft:diamond_hoe", 262144);
        put("minecraft:diamond_sword", 524288);
        put("minecraft:shield", 1048576);
        put("minecraft:bow", 2097152);
        put("minecraft:arrow", 4194304);
        put("minecraft:crossbow", 8388608);
        put("minecraft:firework_rocket", 16777216);
        put("palladium:raw_titanium", 33554432);
        put("minecraft:flint", 67108864);
        put("minecraft:string", 134217728);
        put("minecraft:stick", 268435456);
        put("minecraft:slime_ball", 536870912);
        put("minecraft:leather", 1073741824);
    }};

    // Stores the bit values for the Creation Shop - Map 3 (Static - not configurable)
    public static final Map<String, Integer> BIT_MAP_3_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:leather_helmet", 1);
        put("minecraft:leather_chestplate", 2);
        put("minecraft:leather_leggings", 4);
        put("minecraft:leather_boots", 8);
        put("minecraft:chainmail_helmet", 16);
        put("minecraft:chainmail_chestplate", 32);
        put("minecraft:chainmail_leggings", 64);
        put("minecraft:chainmail_boots", 128);
        put("minecraft:iron_helmet", 256);
        put("minecraft:iron_chestplate", 512);
        put("minecraft:iron_leggings", 1024);
        put("minecraft:iron_boots", 2048);
        put("minecraft:golden_helmet", 4096);
        put("minecraft:golden_chestplate", 8192);
        put("minecraft:golden_leggings", 16384);
        put("minecraft:golden_boots", 32768);
        put("minecraft:diamond_helmet", 65536);
        put("minecraft:diamond_chestplate", 131072);
        put("minecraft:diamond_leggings", 262144);
        put("minecraft:diamond_boots", 524288);
        put("minecraft:blaze_rod", 1048576);
        put("minecraft:book", 2097152);
        put("minecraft:paper", 4194304);
        put("minecraft:glowstone_dust", 8388608);
        put("minecraft:gunpowder", 16777216);
        put("minecraft:nautilus_shell", 33554432);
        put("minecraft:clay_ball", 67108864);
        put("minecraft:brick", 134217728);
        put("minecraft:prismarine_shard", 268435456);
        put("minecraft:prismarine_crystals", 536870912);
        put("minecraft:torch", 1073741824);
    }};

    // Stores the bit values for the Creation Shop - Map 4 (Static - not configurable)
    public static final Map<String, Integer> BIT_MAP_4_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:unbreaking", 1);
        put("minecraft:projectile_protection", 2);
        put("minecraft:smite", 4);
        put("minecraft:lure", 8);
        put("minecraft:fire_protection", 16);
        put("minecraft:knockback", 32);
        put("minecraft:punch", 64);
        put("minecraft:sweeping", 128);
        put("minecraft:loyalty", 256);
        put("minecraft:piercing", 512);
        put("minecraft:flame", 1024);
        put("minecraft:blast_protection", 2048);
        put("minecraft:efficiency", 4096);
        put("minecraft:feather_falling", 8192);
        put("minecraft:depth_strider", 16384);
        put("minecraft:power", 32768);
        put("minecraft:respiration", 65536);
        put("minecraft:riptide", 131072);
        put("minecraft:aqua_affinity", 262144);
        put("minecraft:silk_touch", 524288);
        put("minecraft:fortune", 1048576);
        put("minecraft:fire_aspect", 2097152);
        put("minecraft:luck_of_the_sea", 4194304);
        put("minecraft:thorns", 8388608);
        put("minecraft:sharpness", 16777216);
        put("minecraft:protection", 33554432);
        put("minecraft:frost_walker", 67108864);
        put("minecraft:multishot", 134217728);
        put("minecraft:quick_charge", 268435456);
        put("minecraft:swift_sneak", 536870912);
        put("minecraft:oak_log", 1073741824);
    }};

    // Stores the bit values for the Creation Shop - Map 5 (Static - not configurable)
    public static final Map<String, Integer> BIT_MAP_5_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:mending", 1);
        put("minecraft:infinity", 2);
        put("minecraft:looting", 4);
        put("minecraft:soul_speed", 8);
        put("minecraft:channeling", 16);
        put("minecraft:impaling", 32);
        put("minecraft:spruce_log", 64);
        put("minecraft:birch_log", 128);
        put("minecraft:jungle_log", 256);
        put("minecraft:acacia_log", 512);
        put("minecraft:dark_oak_log", 1024);
        put("minecraft:mangrove_log", 2048);
        put("minecraft:cherry_log", 4096);
        put("minecraft:glass", 8192);
        put("minecraft:smooth_stone", 16384);
        put("minecraft:chiseled_stone_bricks", 32768);
        put("minecraft:mossy_stone_bricks", 65536);
        put("minecraft:sandstone", 131072);
        put("minecraft:red_sandstone", 262144);
        put("minecraft:bricks", 524288);
        put("minecraft:crimson_stem", 1048576);
        put("minecraft:warped_stem", 2097152);
        put("minecraft:end_stone", 4194304);
        put("minecraft:prismarine", 8388608);
        put("minecraft:basalt", 16777216);
        put("minecraft:sea_lantern", 33554432);
        put("minecraft:dark_prismarine", 67108864);
        put("minecraft:totem_of_undying", 134217728);
        put("minecraft:bone", 268435456);
    }};

    // ===== BUY COSTS (Configurable) =====
    // These are the costs for purchasing items you already know how to make

    // Buy costs for items (Configurable)
    public static Map<String, Integer> ITEM_BUY_COST_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:coal", 10);
        put("minecraft:copper_ingot", 20);
        put("minecraft:iron_ingot", 40);
        put("minecraft:lapis_lazuli", 80);
        put("minecraft:redstone", 160);
        put("minecraft:gold_ingot", 200);
        put("minecraft:quartz", 300);
        put("minecraft:diamond", 1000);
        put("minecraft:emerald", 1000);
        put("minecraft:stone_axe", 30);
        put("minecraft:stone_pickaxe", 30);
        put("minecraft:stone_shovel", 20);
        put("minecraft:stone_hoe", 25);
        put("minecraft:stone_sword", 25);
        put("minecraft:iron_axe", 110);
        put("minecraft:iron_pickaxe", 110);
        put("minecraft:iron_shovel", 50);
        put("minecraft:iron_hoe", 70);
        put("minecraft:iron_sword", 70);
        put("minecraft:gold_axe", 550);
        put("minecraft:gold_pickaxe", 550);
        put("minecraft:gold_shovel", 150);
        put("minecraft:gold_hoe", 350);
        put("minecraft:gold_sword", 350);
        put("minecraft:diamond_axe", 2500);
        put("minecraft:diamond_pickaxe", 2500);
        put("minecraft:diamond_shovel", 500);
        put("minecraft:diamond_hoe", 1500);
        put("minecraft:diamond_sword", 1500);
        put("minecraft:shield", 30);
        put("minecraft:bow", 30);
        put("minecraft:arrow", 30);
        put("minecraft:crossbow", 30);
        put("minecraft:firework_rocket", 30);
        put("minecraft:dirt", 5);
        put("minecraft:gravel", 5);
        put("minecraft:sand", 5);
        put("minecraft:grass_block", 5);
        put("minecraft:red_sand", 5);
        put("minecraft:cobblestone", 5);
        put("minecraft:stone", 5);
        put("minecraft:andesite", 5);
        put("minecraft:granite", 5);
        put("minecraft:diorite", 5);
        put("minecraft:netherrack", 5);
        put("minecraft:nether_bricks", 5);
        put("minecraft:blackstone", 5);
        put("minecraft:soul_sand", 5);
        put("minecraft:obsidian", 5);
        put("minecraft:deepslate", 5);
        put("minecraft:cobbled_deepslate", 5);
        put("minecraft:tuff", 5);
        put("minecraft:stone_bricks", 5);
        put("minecraft:calcite", 5);
        put("minecraft:leather_helmet", 5);
        put("minecraft:leather_chestplate", 5);
        put("minecraft:leather_leggings", 5);
        put("minecraft:leather_boots", 5);
        put("minecraft:chainmail_helmet", 5);
        put("minecraft:chainmail_chestplate", 5);
        put("minecraft:chainmail_leggings", 5);
        put("minecraft:chainmail_boots", 5);
        put("minecraft:iron_helmet", 5);
        put("minecraft:iron_chestplate", 5);
        put("minecraft:iron_leggings", 5);
        put("minecraft:iron_boots", 5);
        put("minecraft:golden_helmet", 5);
        put("minecraft:golden_chestplate", 5);
        put("minecraft:golden_leggings", 5);
        put("minecraft:golden_boots", 5);
        put("minecraft:diamond_helmet", 5);
        put("minecraft:diamond_chestplate", 5);
        put("minecraft:diamond_leggings", 5);
        put("minecraft:diamond_boots", 5);
        put("minecraft:amethyst_shard", 50);
        put("palladium:lead_ingot", 100);
        put("palladium:raw_titanium", 150);
        put("minecraft:flint", 5);
        put("minecraft:string", 5);
        put("minecraft:stick", 5);
        put("minecraft:slime_ball", 10);
        put("minecraft:leather", 10);
        put("minecraft:bone", 5);
        put("minecraft:blaze_rod", 100);
        put("minecraft:book", 20);
        put("minecraft:paper", 5);
        put("minecraft:glowstone_dust", 30);
        put("minecraft:gunpowder", 20);
        put("minecraft:nautilus_shell", 200);
        put("minecraft:clay_ball", 5);
        put("minecraft:brick", 5);
        put("minecraft:prismarine_shard", 25);
        put("minecraft:prismarine_crystals", 35);
        put("minecraft:torch", 5);
        put("minecraft:totem_of_undying", 2000);
        put("minecraft:oak_log", 5);
        put("minecraft:spruce_log", 5);
        put("minecraft:birch_log", 5);
        put("minecraft:jungle_log", 5);
        put("minecraft:acacia_log", 5);
        put("minecraft:dark_oak_log", 5);
        put("minecraft:mangrove_log", 5);
        put("minecraft:cherry_log", 5);
        put("minecraft:glass", 5);
        put("minecraft:smooth_stone", 5);
        put("minecraft:chiseled_stone_bricks", 10);
        put("minecraft:mossy_stone_bricks", 10);
        put("minecraft:sandstone", 5);
        put("minecraft:red_sandstone", 5);
        put("minecraft:bricks", 10);
        put("minecraft:crimson_stem", 15);
        put("minecraft:warped_stem", 15);
        put("minecraft:end_stone", 50);
        put("minecraft:prismarine", 30);
        put("minecraft:basalt", 10);
        put("minecraft:sea_lantern", 40);
        put("minecraft:dark_prismarine", 35);
    }};

    // Buy costs for enchantments (Configurable)
    public static Map<String, Integer> ENCHANT_BUY_COST_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:unbreaking", 1);
        put("minecraft:projectile_protection", 1);
        put("minecraft:smite", 1);
        put("minecraft:lure", 1);
        put("minecraft:fire_protection", 1);
        put("minecraft:knockback", 1);
        put("minecraft:punch", 1);
        put("minecraft:sweeping", 1);
        put("minecraft:loyalty", 1);
        put("minecraft:piercing", 1);
        put("minecraft:flame", 1);
        put("minecraft:blast_protection", 1);
        put("minecraft:efficiency", 1);
        put("minecraft:feather_falling", 1);
        put("minecraft:depth_strider", 1);
        put("minecraft:power", 1);
        put("minecraft:respiration", 1);
        put("minecraft:riptide", 1);
        put("minecraft:aqua_affinity", 1);
        put("minecraft:silk_touch", 1);
        put("minecraft:fortune", 1);
        put("minecraft:fire_aspect", 1);
        put("minecraft:luck_of_the_sea", 1);
        put("minecraft:thorns", 1);
        put("minecraft:sharpness", 1);
        put("minecraft:protection", 1);
        put("minecraft:frost_walker", 1);
        put("minecraft:multishot", 1);
        put("minecraft:quick_charge", 1);
        put("minecraft:swift_sneak", 1);
        put("minecraft:mending", 1);
        put("minecraft:infinity", 1);
        put("minecraft:looting", 1);
        put("minecraft:soul_speed", 1);
        put("minecraft:channeling", 1);
        put("minecraft:impaling", 1);
    }};

    // ===== LEARN COSTS (Configurable) =====
    // These are the costs for learning new items/enchantments for the first time

    // Learn costs for items (Configurable)
    public static Map<String, Integer> ITEM_LEARN_COST_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:coal", 64);
        put("minecraft:copper_ingot", 64);
        put("minecraft:iron_ingot", 64);
        put("minecraft:lapis_lazuli", 64);
        put("minecraft:redstone", 64);
        put("minecraft:gold_ingot", 64);
        put("minecraft:quartz", 64);
        put("minecraft:diamond", 64);
        put("minecraft:emerald", 64);
        put("minecraft:stone_axe", 3);
        put("minecraft:stone_pickaxe", 3);
        put("minecraft:stone_shovel", 3);
        put("minecraft:stone_hoe", 3);
        put("minecraft:stone_sword", 3);
        put("minecraft:iron_axe", 4);
        put("minecraft:iron_pickaxe", 4);
        put("minecraft:iron_shovel", 4);
        put("minecraft:iron_hoe", 4);
        put("minecraft:iron_sword", 4);
        put("minecraft:gold_axe", 4);
        put("minecraft:gold_pickaxe", 4);
        put("minecraft:gold_shovel", 4);
        put("minecraft:gold_hoe", 4);
        put("minecraft:gold_sword", 4);
        put("minecraft:diamond_axe", 5);
        put("minecraft:diamond_pickaxe", 5);
        put("minecraft:diamond_shovel", 5);
        put("minecraft:diamond_hoe", 5);
        put("minecraft:diamond_sword", 5);
        put("minecraft:shield", 3);
        put("minecraft:bow", 3);
        put("minecraft:arrow", 24);
        put("minecraft:crossbow", 3);
        put("minecraft:firework_rocket", 64);
        put("minecraft:dirt", 32);
        put("minecraft:gravel", 32);
        put("minecraft:sand", 32);
        put("minecraft:grass_block", 32);
        put("minecraft:red_sand", 32);
        put("minecraft:cobblestone", 64);
        put("minecraft:stone", 64);
        put("minecraft:andesite", 64);
        put("minecraft:granite", 64);
        put("minecraft:diorite", 64);
        put("minecraft:netherrack", 64);
        put("minecraft:nether_bricks", 64);
        put("minecraft:blackstone", 64);
        put("minecraft:soul_sand", 128);
        put("minecraft:obsidian", 64);
        put("minecraft:deepslate", 64);
        put("minecraft:cobbled_deepslate", 64);
        put("minecraft:tuff", 64);
        put("minecraft:stone_bricks", 64);
        put("minecraft:calcite", 64);
        put("minecraft:leather_helmet", 2);
        put("minecraft:leather_chestplate", 2);
        put("minecraft:leather_leggings", 2);
        put("minecraft:leather_boots", 2);
        put("minecraft:chainmail_helmet", 3);
        put("minecraft:chainmail_chestplate", 3);
        put("minecraft:chainmail_leggings", 3);
        put("minecraft:chainmail_boots", 3);
        put("minecraft:iron_helmet", 3);
        put("minecraft:iron_chestplate", 3);
        put("minecraft:iron_leggings", 3);
        put("minecraft:iron_boots", 3);
        put("minecraft:golden_helmet", 4);
        put("minecraft:golden_chestplate", 4);
        put("minecraft:golden_leggings", 4);
        put("minecraft:golden_boots", 4);
        put("minecraft:diamond_helmet", 4);
        put("minecraft:diamond_chestplate", 4);
        put("minecraft:diamond_leggings", 4);
        put("minecraft:diamond_boots", 4);
        put("minecraft:amethyst_shard", 16);
        put("palladium:lead_ingot", 64);
        put("palladium:raw_titanium", 32);
        put("minecraft:flint", 16);
        put("minecraft:string", 16);
        put("minecraft:stick", 8);
        put("minecraft:slime_ball", 24);
        put("minecraft:leather", 24);
        put("minecraft:bone", 16);
        put("minecraft:blaze_rod", 16);
        put("minecraft:book", 32);
        put("minecraft:paper", 16);
        put("minecraft:glowstone_dust", 64);
        put("minecraft:gunpowder", 48);
        put("minecraft:nautilus_shell", 3);
        put("minecraft:clay_ball", 16);
        put("minecraft:brick", 16);
        put("minecraft:prismarine_shard", 48);
        put("minecraft:prismarine_crystals", 64);
        put("minecraft:torch", 8);
        put("minecraft:totem_of_undying", 5);
        put("minecraft:oak_log", 16);
        put("minecraft:spruce_log", 16);
        put("minecraft:birch_log", 16);
        put("minecraft:jungle_log", 16);
        put("minecraft:acacia_log", 16);
        put("minecraft:dark_oak_log", 16);
        put("minecraft:mangrove_log", 16);
        put("minecraft:cherry_log", 16);
        put("minecraft:glass", 16);
        put("minecraft:smooth_stone", 24);
        put("minecraft:chiseled_stone_bricks", 32);
        put("minecraft:mossy_stone_bricks", 32);
        put("minecraft:sandstone", 24);
        put("minecraft:red_sandstone", 24);
        put("minecraft:bricks", 32);
        put("minecraft:crimson_stem", 48);
        put("minecraft:warped_stem", 48);
        put("minecraft:end_stone", 128);
        put("minecraft:prismarine", 64);
        put("minecraft:basalt", 32);
        put("minecraft:sea_lantern", 24);
        put("minecraft:dark_prismarine", 64);
    }};

    // Learn costs for enchantments (Configurable)
    public static Map<String, Integer> ENCHANT_LEARN_COST_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:unbreaking", 5);
        put("minecraft:projectile_protection", 5);
        put("minecraft:smite", 5);
        put("minecraft:lure", 5);
        put("minecraft:fire_protection", 5);
        put("minecraft:knockback", 5);
        put("minecraft:punch", 5);
        put("minecraft:sweeping", 5);
        put("minecraft:loyalty", 5);
        put("minecraft:piercing", 5);
        put("minecraft:flame", 5);
        put("minecraft:blast_protection", 5);
        put("minecraft:efficiency", 5);
        put("minecraft:feather_falling", 5);
        put("minecraft:depth_strider", 5);
        put("minecraft:power", 5);
        put("minecraft:respiration", 5);
        put("minecraft:riptide", 5);
        put("minecraft:aqua_affinity", 10);
        put("minecraft:silk_touch", 10);
        put("minecraft:fortune", 10);
        put("minecraft:fire_aspect", 10);
        put("minecraft:luck_of_the_sea", 10);
        put("minecraft:thorns", 10);
        put("minecraft:sharpness", 10);
        put("minecraft:protection",10);
        put("minecraft:frost_walker",10);
        put("minecraft:multishot",10);
        put("minecraft:quick_charge",10);
        put("minecraft:swift_sneak",10);
        put("minecraft:mending",10);
        put("minecraft:infinity",10);
        put("minecraft:looting",10);
        put("minecraft:soul_speed",10);
        put("minecraft:channeling",10);
        put("minecraft:impaling",10);
    }};

    public static double CREATION_STAMINA_COST = 0.8;

    // Private constructor to prevent instantiation
    private CreationShopConstants() {
        throw new AssertionError("This class should not be instantiated");
    }
}
