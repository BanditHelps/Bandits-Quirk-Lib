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

    public static final RegistryObject<MobEffect> STUN_EFFECT =
            MOB_EFFECTS.register("stun_effect", StunMobEffect::new);

    public static final RegistryObject<MobEffect> TRUE_INVISIBILITY =
            MOB_EFFECTS.register("true_invisibility", TrueInvisibilityEffect::new);

    public static final RegistryObject<MobEffect> PSTOCK_OVERUSE =
            MOB_EFFECTS.register("pstock_overuse", PstockOveruseEffect::new);

    public static final RegistryObject<MobEffect> HEAT_RESISTANCE =
            MOB_EFFECTS.register("heat_resistance", HeatResistanceEffect::new);

    public static final RegistryObject<MobEffect> COLD_RESISTANCE =
            MOB_EFFECTS.register("cold_resistance", ColdResistanceEffect::new);

    public static final RegistryObject<MobEffect> POISON_RESISTANCE =
            MOB_EFFECTS.register("poison_resistance", PoisonResistanceEffect::new);

    public static final RegistryObject<MobEffect> HUNGER_RESISTANCE =
            MOB_EFFECTS.register("hunger_resistance", HungerResistanceEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}