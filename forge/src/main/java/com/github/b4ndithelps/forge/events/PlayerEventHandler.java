package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.abilities.HappenOnceAbility;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.UUID;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        HappenOnceAbility.cleanupPlayerData(playerUUID);

        BanditsQuirkLibForge.LOGGER.info("Cleaned up player data");
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // For the player's first log in, initialize to a random stamina, give the base powers, and a random quirk
        StaminaHelper.initializePlayerStamina(player);

        SuperpowerUtil.addSuperpower(player, ResourceLocation.parse("bql:base_quirk"));
    }
}
