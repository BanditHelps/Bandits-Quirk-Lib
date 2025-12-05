package com.github.b4ndithelps.forge.effects;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class SnowBlindnessEffect extends MobEffect {

    public SnowBlindnessEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8F4FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}