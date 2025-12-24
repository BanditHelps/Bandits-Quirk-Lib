package com.github.b4ndithelps.forge.abilities;


import com.github.b4ndithelps.forge.config.BQLConfig;
import com.github.b4ndithelps.forge.particle.ModParticles;
import com.github.b4ndithelps.forge.sounds.ModSounds;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.entity.PalladiumAttributes;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

import static com.github.b4ndithelps.forge.systems.PowerStockHelper.sendPlayerPercentageMessage;
import static com.github.b4ndithelps.forge.utils.ActionBarHelper.sendPercentageDisplay;

public class BurstGeneAbility extends Ability {
    // Unique properties for tracking state
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    private static final UUID JUMP_MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    public BurstGeneAbility() {
        super();
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
    }

//    @Override
//    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
//        if (enabled && entity instanceof ServerPlayer player) {
//            // Initialize charge ( not needed )
////            entry.setUniqueProperty(CHARGE_TICKS, 0);
////            BodyStatusHelper.setCustomFloat(player, "chest", "burst_gene_charge", 0.0f);
//
//            if (entity.level() instanceof ServerLevel serverLevel) {
//                // Play charging start sound
//                serverLevel.playSound(null, player.blockPosition(),
//                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3f, 1.2f);
//            }
//        }
//    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "burst_charge");

        if (charge > 1) {
            if (charge == 25) {

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 5, true, false));
                AttributeInstance attribute = player.getAttribute(PalladiumAttributes.JUMP_POWER.get());

                attribute.removeModifier(JUMP_MODIFIER_UUID);

                if (attribute != null) {
                    AttributeModifier modifier = new AttributeModifier(
                            JUMP_MODIFIER_UUID,
                            "temporary_jump_debuff",
                            -9999, // +50% movement speed
                            AttributeModifier.Operation.ADDITION
                    );

                    attribute.addPermanentModifier(modifier);
                }
                // Play charging start sound
                serverLevel.playSound(null, player.blockPosition(),
                        ModSounds.EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 1f, 1.0f);
            }
            Vec3 playerPos = player.position();
            BodyStatusHelper.setCustomFloat(player, "chest", "burst_charge", --charge);
            // Example: 2 blocks away
            Vec3 direction = playerPos.subtract(playerPos).normalize().scale(1); // Velocity towards player

            float r = 0.5f, g = 0.5f, b = 0.5f, scale = 0.4f;

            DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), scale);

            // Noticeable upward velocity
            double motionX = 0.0D;
            double motionY = 0.5D; // make this higher for faster movement
            double motionZ = 0.0D;

            // Use overload that accepts motion values
            serverLevel.sendParticles(
                    ModParticles.RISING_DUST.get(),
                    playerPos.x, playerPos.y + 1, playerPos.z,
                    2,   // spawn multiple for visibility
                    0.8D, 0.5D, 0.8D, // spread vertically
                    0.0D  // speed
            );
        } else if (charge == 1) {
            BodyStatusHelper.setCustomFloat(player, "chest", "burst_charge", --charge);
            executeBurst(player, serverLevel, entry);
            AttributeInstance attribute = player.getAttribute(PalladiumAttributes.JUMP_POWER.get());
            if (attribute != null) {
                attribute.removeModifier(JUMP_MODIFIER_UUID);
            }
        }




    }

    private void executeBurst(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        float factor = (float) (QuirkFactorHelper.getQuirkFactor(player)+1);
        double factor_scaling = BQLConfig.INSTANCE.burstGeneFactorScaling.get();
        float damageDone = (float) (factor * factor_scaling * 3);
        player.hurt(level.damageSources().generic(), damageDone);
        level.explode(
                (Entity) player,                  // entity that caused it (null = no source)
                player.getX(),           // x
                player.getY() + 1,           // y
                player.getZ(),           // z
                (float) (factor * factor_scaling * 1.5F),                    // explosion power (TNT = 4)
                Level.ExplosionInteraction.MOB // what kind of explosion (controls block damage)
        );

        BodyStatusHelper.damageAll(player, (player.getMaxHealth() - damageDone) / player.getMaxHealth());
    }


    @Override
    public String getDocumentationDescription() {
        return "Burst ability";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}
