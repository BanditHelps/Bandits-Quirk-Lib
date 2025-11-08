package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.effects.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class ResistanceEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!(entity instanceof ServerPlayer)) return;

        var source = event.getSource();
        float amount = event.getAmount();

        // Fire and hot floor (heat). Allow reduction for lava, but only fully negate non-lava fire at max.
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.HOT_FLOOR)) {
            MobEffectInstance heat = entity.getEffect(ModEffects.HEAT_RESISTANCE.get());
            if (heat != null) {
                boolean isLava = source.is(DamageTypes.LAVA);
                int amp = heat.getAmplifier();
                if (!isLava && amp >= 3) {
                    amount = 0.0F; // Max heat resistance cancels non-lava fire damage
                } else {
                    amount = reduceByAmplifier(amount, amp);
                }
            }
        }
        // Freezing (cold)
        if (source.is(DamageTypes.FREEZE)) {
            MobEffectInstance inst = entity.getEffect(ModEffects.COLD_RESISTANCE.get());
            if (inst != null) {
                amount = reduceByAmplifier(amount, inst.getAmplifier());
            }
        }
        // Poison effect damage only (exclude generic/indirect magic). Poison ticks are MAGIC with no attacker.
        if (source.is(DamageTypes.MAGIC)) {
            boolean isPoisonTick = entity.hasEffect(MobEffects.POISON) && source.getEntity() == null && !source.is(DamageTypes.INDIRECT_MAGIC);
            if (isPoisonTick) {
                MobEffectInstance inst = entity.getEffect(ModEffects.POISON_RESISTANCE.get());
                if (inst != null) {
                    amount = reduceByAmplifier(amount, inst.getAmplifier());
                }
            }
        }
        // Starvation (hunger)
        if (source.is(DamageTypes.STARVE)) {
            MobEffectInstance inst = entity.getEffect(ModEffects.HUNGER_RESISTANCE.get());
            if (inst != null) {
                amount = reduceByAmplifier(amount, inst.getAmplifier());
            }
        }

        event.setAmount(amount);
    }

    private static float reduceByAmplifier(float base, int amplifier) {
        // amplifier: 0..3 corresponds to 20%, 35%, 50%, 65%, 80% (cap at 4)
        int lvl = Math.min(4, amplifier + 1);
        double[] reductions = new double[]{0.20, 0.35, 0.50, 0.65, 0.80};
        double factor = 1.0 - reductions[lvl - 1];
        return (float)(base * factor);
    }
}