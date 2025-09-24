package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class PhantomSpawnHandler {

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.FinalizeSpawn event) {

        if (event.getEntity() instanceof Phantom phantom) {
            if (event.getLevel().isClientSide()) return;


            var players = event.getLevel().players();
            for (var player : players) {

                if (SuperpowerUtil.hasSuperpower(player, ResourceLocation.parse("bql:phantom_immunity"))) {
                    event.setSpawnCancelled(true);
                    break;
                }
            }
        }
    }
}
