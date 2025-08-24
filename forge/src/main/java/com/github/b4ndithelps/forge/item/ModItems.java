package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<Item> TISSUE_EXTRACTOR = ITEMS.register("tissue_extractor", () -> new TissueExtractorItem(new Item.Properties().stacksTo(1).durability(128)));
    public static final RegistryObject<Item> TISSUE_SAMPLE = ITEMS.register("tissue_sample", () -> new TissueSampleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SEQUENCED_SAMPLE = ITEMS.register("sequenced_sample", () -> new SequencedSampleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> READOUT = ITEMS.register("readout", () -> new ReadoutItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}


