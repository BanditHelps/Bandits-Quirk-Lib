package com.github.b4ndithelps.forge.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@SuppressWarnings("removal")
public class ModDamageTypes {

    public static final ResourceKey<DamageType> EXHAUSTION_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MOD_ID, "exhaustion"));

    public static final ResourceKey<DamageType> PERMEATION_PUNCH_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MOD_ID, "permeation_punch"));

    // A helper method to create the damage source
    public static DamageSource exhaustion(Level level) {
        try {
            return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(EXHAUSTION_KEY));
        } catch (Exception e) {
            // Fallback to a vanilla damage source if custom one fails
            return level.damageSources().generic();
        }
    }

    // A helper method to create the damage source with a specific entity as the source
    public static DamageSource exhaustion(Level level, Entity entity) {
        try {
            return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(EXHAUSTION_KEY), entity);
        } catch (Exception e) {
            // Fallback to a vanilla damage source if custom one fails
            return level.damageSources().generic();
        }
    }

    // Helper method to create the damage source with both direct and indirect entities
    public static DamageSource exhaustion(Level level, Entity directEntity, Entity indirectEntity) {
        try {
            return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(EXHAUSTION_KEY), directEntity, indirectEntity);
        } catch (Exception e) {
            // Fallback to a vanilla damage source if custom one fails
            return level.damageSources().generic();
        }
    }

    public static DamageSource permeationPunch(Level level, Entity attacker) {
        try {
            return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(PERMEATION_PUNCH_KEY), attacker);
        } catch (Exception e) {
            return level.damageSources().generic();
        }
    }

    // Method to apply exhaustion damage to a player
    public static void applyExhaustionDamage(Player player, float damageAmount) {
        if (!player.level().isClientSide) {
            DamageSource exhaustionDamage = exhaustion(player.level());
            player.hurt(exhaustionDamage, damageAmount);
        }
    }

    // Method to apply exhaustion damage with a specific source entity
    public static void applyExhaustionDamage(Player player, float damageAmount, Entity sourceEntity) {
        if (!player.level().isClientSide) {
            DamageSource exhaustionDamage = exhaustion(player.level(), sourceEntity);
            player.hurt(exhaustionDamage, damageAmount);
        }
    }
}