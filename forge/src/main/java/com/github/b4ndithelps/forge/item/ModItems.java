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

    public static final RegistryObject<Item> GENE_VIAL_COSMETIC = ITEMS.register("gene_vial_cosmetic",
            () -> new GeneVialItem(new Item.Properties().stacksTo(1), GeneVialItem.Category.COSMETIC));
    public static final RegistryObject<Item> GENE_VIAL_RESISTANCE = ITEMS.register("gene_vial_resistance",
            () -> new GeneVialItem(new Item.Properties().stacksTo(1), GeneVialItem.Category.RESISTANCE));
    public static final RegistryObject<Item> GENE_VIAL_BUILDER = ITEMS.register("gene_vial_builder",
            () -> new GeneVialItem(new Item.Properties().stacksTo(1), GeneVialItem.Category.BUILDER));
    public static final RegistryObject<Item> GENE_VIAL_LOWEND = ITEMS.register("gene_vial_lowend",
            () -> new GeneVialItem(new Item.Properties().stacksTo(1), GeneVialItem.Category.LOWEND));
    public static final RegistryObject<Item> GENE_VIAL_QUIRK = ITEMS.register("gene_vial_quirk",
            () -> new GeneVialItem(new Item.Properties().stacksTo(1), GeneVialItem.Category.QUIRK));

    public static final RegistryObject<Item> GENE_DATABASE = ITEMS.register("gene_database",
            () -> new GeneDatabaseItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector",
            () -> new InjectorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FAILED_SAMPLE = ITEMS.register("failed_sample",
            () -> new FailedSampleItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}