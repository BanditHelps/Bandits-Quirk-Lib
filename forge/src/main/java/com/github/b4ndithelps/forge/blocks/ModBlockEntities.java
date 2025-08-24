package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<BlockEntityType<DNASequencerBlockEntity>> DNA_SEQUENCER = BLOCK_ENTITIES.register("dna_sequencer",
            () -> BlockEntityType.Builder.of(DNASequencerBlockEntity::new, ModBlocks.DNA_SEQUENCER.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}


