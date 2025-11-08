package com.github.b4ndithelps.forge.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AmpuleEffect extends MobEffect{
    public AmpuleEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x98D982); // color
    }

    @Override
    public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
        // nothing
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
