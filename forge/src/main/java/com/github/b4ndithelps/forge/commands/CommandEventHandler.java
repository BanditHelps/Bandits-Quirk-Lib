package com.github.b4ndithelps.forge.commands;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class CommandEventHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MineHaSlotCommand.register(event.getDispatcher());
        MineHaCreationCommand.register(event.getDispatcher());
        MineHaEnchantCommand.register(event.getDispatcher());
        StaminaCommand.register(event.getDispatcher());
        BodyStatusCommand.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
        GenomeCommand.register(event.getDispatcher());
    }
}
