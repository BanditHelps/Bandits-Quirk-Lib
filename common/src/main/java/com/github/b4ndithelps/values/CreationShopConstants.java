package com.github.b4ndithelps.values;

import java.util.HashMap;
import java.util.Map;

/**
 * The following class contains constants available to the scripts
 * inside of the /data/mineha/kubejs_scripts/ folder.
 *
 * TODO: Add config settings for the following
 */
public class CreationShopConstants {

    // Stores the bit values for the Creation Shop - Map 1
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
    }};

    // Stores the bit values for the Creation Shop - Map 2
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
    }};

    // Stores the bit values for the Creation Shop - Map 3
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
    }};

    public static final Map<String, Integer> BIT_MAP_4_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:unbreaking", 1);
        put("minecraft:projectile_protection", 2);
        put("minecraft:smite", 4);
        put("minecraft:lure", 8);
        put("minecraft:fire_protection", 16);
        put("minecraft:knockback", 32);
        put("minecraft:punch", 64);
        put("minecraft:sweeping_edge", 128);
        put("minecraft:loyaty", 256);
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
        put("minecraft:swift_sneak", 536870812);
    }};

    public static final Map<String, Integer> BIT_MAP_5_TABLE = new HashMap<String, Integer>() {{
        put("minecraft:mending", 1);
        put("minecraft:infinity", 2);
        put("minecraft:looting", 4);
        put("minecraft:soul_speed", 8);
        put("minecraft:channeling", 16);
        put("minecraft:impaling", 32);
    }};

    // Stores the creation prices for items
    public static final Map<String, Integer> CREATION_PRICE_TABLE = new HashMap<String, Integer>() {{
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
        put("minecraft:stone_shovel", 30);
        put("minecraft:stone_hoe", 30);
        put("minecraft:stone_sword", 30);
        put("minecraft:iron_axe", 30);
        put("minecraft:iron_pickaxe", 30);
        put("minecraft:iron_shovel", 30);
        put("minecraft:iron_hoe", 30);
        put("minecraft:iron_sword", 30);
        put("minecraft:gold_axe", 30);
        put("minecraft:gold_pickaxe", 30);
        put("minecraft:gold_shovel", 30);
        put("minecraft:gold_hoe", 30);
        put("minecraft:gold_sword", 30);
        put("minecraft:diamond_axe", 30);
        put("minecraft:diamond_pickaxe", 30);
        put("minecraft:diamond_shovel", 30);
        put("minecraft:diamond_hoe", 30);
        put("minecraft:diamond_sword", 30);
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
    }};

    // Private constructor to prevent instantiation
    private CreationShopConstants() {
        throw new AssertionError("This class should not be instantiated");
    }
}
