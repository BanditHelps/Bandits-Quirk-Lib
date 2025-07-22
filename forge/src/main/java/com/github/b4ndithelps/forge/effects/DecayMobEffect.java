package com.github.b4ndithelps.forge.effects;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class DecayMobEffect extends MobEffect {

    public DecayMobEffect() {
        // Color: 0x000000 (black), Category: HARMFUL
        super(MobEffectCategory.HARMFUL, 0x000000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        // Only execute every 20 ticks (1 second)
        if (entity.tickCount % 20 != 0) {
            return;
        }

        // Damage held item in main hand
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!mainHandItem.isEmpty() && mainHandItem.isDamageableItem()) {
            int damageAmount = 15 * (amplifier + 1);
            mainHandItem.hurt(damageAmount, entity.getRandom(), null);
        }

        // Check and damage armor
        int armorWearing = 0;
        boolean allArmorBroken = true;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for (EquipmentSlot slot : armorSlots) {
            ItemStack armorPiece = entity.getItemBySlot(slot);
            if (!armorPiece.isEmpty()) {
                armorWearing++;

                if (armorPiece.isDamageableItem()) {
                    int damageAmount = 15 * (amplifier + 1);
                    armorPiece.hurt(damageAmount, entity.getRandom(), null);

                    // Check if armor piece should break
                    if (armorPiece.getDamageValue() >= armorPiece.getMaxDamage()) {
                        entity.setItemSlot(slot, ItemStack.EMPTY);
                    } else {
                        allArmorBroken = false;
                    }
                } else {
                    allArmorBroken = false;
                }
            }
        }

        try {
            // If all armor is broken or no armor is worn, damage the entity
            if (armorWearing == 0 || allArmorBroken) {
                DamageSource genericDamage = entity.level().damageSources().generic();
                entity.hurt(genericDamage, 1.5f * (amplifier + 1));

                // Check if entity is below 33% health
                if (entity instanceof ServerPlayer player && entity.getHealth() < entity.getMaxHealth() * 0.33f && entity.isAlive()) {
                    breakRandomLimb(player);

                    // Spawn particles and play sounds
                    spawnDecayEffects(entity);
                }
            }
        } catch (Exception e) {
            // Log error instead of console.error
            BanditsQuirkLibForge.LOGGER.error("Error in DecayMobEffect: ", e);
        }
    }

    private void breakRandomLimb(ServerPlayer entity) {
        RandomSource random = entity.getRandom();
        int randomLimb = random.nextInt(4);

        switch (randomLimb) {
            case 0: // right arm
                BodyStatusHelper.setDamage(entity, BodyPart.RIGHT_ARM.toString(), 80);
                break;
            case 1: // left arm
                BodyStatusHelper.setDamage(entity, BodyPart.LEFT_ARM.toString(), 80);
                break;
            case 2: // right leg
                BodyStatusHelper.setDamage(entity, BodyPart.RIGHT_LEG.toString(), 80);
                break;
            case 3: // left leg
                BodyStatusHelper.setDamage(entity, BodyPart.RIGHT_LEG.toString(), 80);
                break;
        }
    }

    private void spawnDecayEffects(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = entity.position().add(0, 1, 0);
        RandomSource random = entity.getRandom();

        // Spawn large smoke particles
        for (int i = 0; i < 15; i++) {
            double offsetX = (random.nextDouble() - 0.5);
            double offsetY = (random.nextDouble() - 0.5);
            double offsetZ = (random.nextDouble() - 0.5);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0, 0, 0.1);
        }

        // Spawn ash particles
        for (int i = 0; i < 10; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            serverLevel.sendParticles(ParticleTypes.ASH,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0, 0, 0.05);
        }

        // Spawn soul particles
        for (int i = 0; i < 8; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.8;
            double offsetY = (random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (random.nextDouble() - 0.5) * 0.8;
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    1, 0, 0, 0, 0.02);
        }

        // Play sounds
        serverLevel.playSound(null, entity.blockPosition(),
                SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.0f, 0.7f);
        serverLevel.playSound(null, entity.blockPosition(),
                SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.0f, 0.5f);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}