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

    public static final RegistryObject<MenuType<DNASequencerMenu>> DNA_SEQUENCER = MENUS.register("dna_sequencer",
            () -> IForgeMenuType.create((id, inv, buf) -> new DNASequencerMenu(id, inv, buf)));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}


