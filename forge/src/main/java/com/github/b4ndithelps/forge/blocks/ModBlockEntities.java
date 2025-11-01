package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<BlockEntityType<BioTerminalBlockEntity>> BIO_TERMINAL = BLOCK_ENTITIES.register("bio_terminal",
            () -> BlockEntityType.Builder.of(BioTerminalBlockEntity::new, ModBlocks.BIO_TERMINAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<GeneSequencerBlockEntity>> GENE_SEQUENCER = BLOCK_ENTITIES.register("gene_sequencer",
            () -> BlockEntityType.Builder.of(GeneSequencerBlockEntity::new, ModBlocks.GENE_SEQUENCER.get()).build(null));

    public static final RegistryObject<BlockEntityType<GeneSlicerBlockEntity>> GENE_SLICER = BLOCK_ENTITIES.register("gene_slicer",
            () -> BlockEntityType.Builder.of(GeneSlicerBlockEntity::new, ModBlocks.GENE_SLICER.get()).build(null));

    public static final RegistryObject<BlockEntityType<SampleRefrigeratorBlockEntity>> SAMPLE_REFRIGERATOR = BLOCK_ENTITIES.register("sample_refrigerator",
            () -> BlockEntityType.Builder.of(SampleRefrigeratorBlockEntity::new, ModBlocks.SAMPLE_REFRIGERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<BioPrinterBlockEntity>> BIO_PRINTER = BLOCK_ENTITIES.register("bio_printer",
            () -> BlockEntityType.Builder.of(BioPrinterBlockEntity::new, ModBlocks.BIO_PRINTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<GeneCombinerBlockEntity>> GENE_COMBINER = BLOCK_ENTITIES.register("gene_combiner",
            () -> BlockEntityType.Builder.of(GeneCombinerBlockEntity::new, ModBlocks.GENE_COMBINER.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}


