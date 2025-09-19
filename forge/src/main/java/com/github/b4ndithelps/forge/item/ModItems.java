package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BanditsQuirkLib.MOD_ID);

    //  ------------------------------------------------- Triggers -------------------------------------------------
    public static final RegistryObject<Item> TIER1_TRIGGER = ITEMS.register("trigger_tier1",
            () -> new FactorTrigger(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 1));
    public static final RegistryObject<Item> TIER2_TRIGGER = ITEMS.register("trigger_tier2",
            () -> new FactorTrigger(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2));
    public static final RegistryObject<Item> TIER3_TRIGGER = ITEMS.register("trigger_tier3",
            () -> new FactorTrigger(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 3));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

