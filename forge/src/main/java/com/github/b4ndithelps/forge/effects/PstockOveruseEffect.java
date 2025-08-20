package com.github.b4ndithelps.forge.effects;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class PstockOveruseEffect extends MobEffect {

    public PstockOveruseEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        if (entity instanceof ServerPlayer player) {
            // Every second, damage their body
            if (entity.tickCount % 20 == 0) {
                BodyStatusHelper.damageAll(player, amplifier + 1);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick every tick to ensure movement is consistently prevented
        return true;
    }
}
