package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.*;

public class QuirkRandomizer {

    public static final List<ResourceLocation> QUIRKS = new ArrayList<>();

    // Does a "un-weighted" randomization of the quirks, giving them a power
    public static void giveRandomQuirk(ServerPlayer player) {
        Random random = new Random();

        // Create a method that devs can run to register their quirks
        // Randomly choose through the list of registered
        // Apply the power.

        SuperpowerUtil.addSuperpower(player, QUIRKS.get(random.nextInt(0, QUIRKS.size() - 1)));
    }

    public static void registerQuirk(String quirkResourceId) {
        try {
            QUIRKS.add(ResourceLocation.parse(quirkResourceId));
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Attempted to register invalid quirk resource id: " + quirkResourceId);
        }
    }



}
