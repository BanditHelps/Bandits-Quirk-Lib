package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<MenuType<BioTerminalMenu>> BIO_TERMINAL = MENUS.register("bio_terminal",
            () -> IForgeMenuType.create((id, inv, buf) -> new BioTerminalMenu(id, inv, buf)));

    public static final RegistryObject<MenuType<BioTerminalRefMenu>> BIO_TERMINAL_REF = MENUS.register("bio_terminal_ref",
            () -> IForgeMenuType.create((id, inv, buf) -> new BioTerminalRefMenu(id, inv, buf)));

    public static final RegistryObject<MenuType<GeneSequencerMenu>> GENE_SEQUENCER = MENUS.register("gene_sequencer",
            () -> IForgeMenuType.create((id, inv, buf) -> new GeneSequencerMenu(id, inv, buf)));

    public static final RegistryObject<MenuType<GeneSlicerMenu>> GENE_SLICER = MENUS.register("gene_slicer",
            () -> IForgeMenuType.create((id, inv, buf) -> new GeneSlicerMenu(id, inv, buf)));

    public static final RegistryObject<MenuType<BioPrinterMenu>> BIO_PRINTER = MENUS.register("bio_printer",
            () -> IForgeMenuType.create((id, inv, buf) -> new BioPrinterMenu(id, inv, buf)));

    public static final RegistryObject<MenuType<GeneCombinerMenu>> GENE_COMBINER = MENUS.register("gene_combiner",
            () -> IForgeMenuType.create((id, inv, buf) -> new GeneCombinerMenu(id, inv, buf)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}


