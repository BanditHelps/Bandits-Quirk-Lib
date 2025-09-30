package com.github.b4ndithelps.forge.events;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.SuperpowerUtil;

@Mod.EventBusSubscriber
public class ZombieTargetingHandler {

    @SubscribeEvent
    public static void onTargetSet(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        LivingEntity target = event.getNewTarget();
        if (!(target instanceof Player player)) return;

        if (SuperpowerUtil.hasSuperpower(player, ResourceLocation.parse("bql:dead_cells"))) {
            // Zombies attack back automatically if hurt, so we (me and chatgpt) only want to block "normal aggro"
            if (zombie.getLastAttacker() != player) {
                event.setCanceled(true);
            }
        }
    }
}
