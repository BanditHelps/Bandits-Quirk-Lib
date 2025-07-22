package com.github.b4ndithelps.forge.effects;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<MobEffect> DECAY_EFFECT =
            MOB_EFFECTS.register("decay_effect", DecayMobEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
