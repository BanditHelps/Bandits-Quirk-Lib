package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BanditsQuirkLib.MOD_ID))
            .icon(() -> new ItemStack(ModBlocks.RESEARCH_TABLE_BLOCK.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.RESEARCH_TABLE_BLOCK.get());
                output.accept(ModBlocks.BIO_TERMINAL.get());
                output.accept(ModBlocks.GENE_SEQUENCER.get());
                output.accept(ModBlocks.GENE_SLICER.get());
                output.accept(ModBlocks.SAMPLE_REFRIGERATOR.get());
                output.accept(ModBlocks.BIO_PRINTER.get());
                output.accept(ModBlocks.GENE_COMBINER.get());
                output.accept(ModBlocks.BIO_CABLE.get());
                output.accept(ModItems.TISSUE_EXTRACTOR.get());
                output.accept(ModItems.TISSUE_SAMPLE.get());
                output.accept(ModItems.SEQUENCED_SAMPLE.get());
                output.accept(ModItems.GENE_VIAL_COSMETIC.get());
                output.accept(ModItems.GENE_VIAL_RESISTANCE.get());
                output.accept(ModItems.GENE_VIAL_BUILDER.get());
                output.accept(ModItems.GENE_VIAL_QUIRK.get());
                output.accept(ModItems.GENE_DATABASE.get());
                output.accept(ModItems.INJECTOR.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}


