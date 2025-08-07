package com.github.b4ndithelps.forge.config;

import net.minecraft.world.level.GameRules;

public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> CREATIVE_STAMINA_BYPASS;
    
    public static void register() {
        CREATIVE_STAMINA_BYPASS = GameRules.register(
            "creativeStaminaBypass", 
            GameRules.Category.PLAYER, 
            GameRules.BooleanValue.create(true)
        );
    }
}