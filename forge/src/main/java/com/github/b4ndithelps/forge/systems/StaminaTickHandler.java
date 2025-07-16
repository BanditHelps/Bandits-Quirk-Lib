package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This is the main ticker for updating stamina. It is done via java instead of Kube to reduce the overhead
 * that comes with interpreting the javascript
 */
@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class StaminaTickHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                event.getServer().getPlayerList().getPlayers()
                        .forEach(StaminaHelper::handleStaminaTick);
            }
        }
    }
}
