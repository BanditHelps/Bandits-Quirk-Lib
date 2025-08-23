package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;

import java.util.List;

/**
 * A single-target punch that can be charged, scaling damage and knockback linearly by powerUsed/maxPower.
 * No explosions. At higher power, emits a shockwave ring of particles on hit.
 */
public class ChargedPunchAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Integer> MAX_CHARGE_TICKS = new IntegerProperty("max_charge_ticks").configurable("Maximum charge time in ticks");
    public static final PalladiumProperty<Float> MAX_RANGE = new FloatProperty("max_range").configurable("Maximum reach for targeting");
    public static final PalladiumProperty<Float> MAX_POWER = new FloatProperty("max_power").configurable("Power value considered 100% scaling (e.g., 500000)");
    public static final PalladiumProperty<Float> MAX_DAMAGE = new FloatProperty("max_damage").configurable("Maximum damage at full power");
    public static final PalladiumProperty<Float> MAX_KNOCKBACK = new FloatProperty("max_knockback").configurable("Maximum knockback at full power");
    public static final PalladiumProperty<Float> SHOCKWAVE_THRESHOLD = new FloatProperty("shockwave_threshold").configurable("Power ratio threshold (0-1) to trigger shockwave");

    // Unique tracking
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public ChargedPunchAbility() {
        super();
        this.withProperty(MAX_CHARGE_TICKS, 60)
                .withProperty(MAX_RANGE, 4.5f)
                .withProperty(MAX_POWER, 500000.0f)
                .withProperty(MAX_DAMAGE, 80.0f)
                .withProperty(MAX_KNOCKBACK, 4.0f)
                .withProperty(SHOCKWAVE_THRESHOLD, 0.6f);
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        entry.setUniqueProperty(CHARGE_TICKS, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.4f, 1.2f);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int maxTicks = entry.getProperty(MAX_CHARGE_TICKS);
        int t = Math.min(maxTicks, entry.getProperty(CHARGE_TICKS) + 1);
        entry.setUniqueProperty(CHARGE_TICKS, t);

        // Light charge particles every few ticks
        if (t % 5 == 0) {
            float chargeRatio = t / (float) maxTicks;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 p = eye.add(look.scale(1.0 + chargeRatio * 0.8));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2 + (int)(chargeRatio * 4), 0.1, 0.1, 0.1, 0.02);

            // Charging HUD message with safety color coding
            float storedPower = PowerStockHelper.getStoredPower(player);
            float chargePercent = chargeRatio * 100.0f;
            float powerUsed = storedPower * chargeRatio;
            PowerStockHelper.sendPlayerPercentageMessage(player, powerUsed, chargePercent, "Charging Punch");
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int chargeTicks = entry.getProperty(CHARGE_TICKS);
        if (chargeTicks <= 0) return;

        int maxTicks = entry.getProperty(MAX_CHARGE_TICKS);
        float chargeRatio = Math.min(1.0f, chargeTicks / (float) maxTicks);

        float storedPower = PowerStockHelper.getStoredPower(player);
        float powerUsed = storedPower * chargeRatio; // linear consumption vs charge
        float maxPower = Math.max(1.0f, entry.getProperty(MAX_POWER));
        float powerRatio = Math.max(0.0f, Math.min(1.0f, powerUsed / maxPower));

        float finalDamage = Math.max(0.0f, entry.getProperty(MAX_DAMAGE) * powerRatio);
        float finalKnockback = Math.max(0.0f, entry.getProperty(MAX_KNOCKBACK) * powerRatio);

        // Swing and play sound
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Target and apply
        float reach = entry.getProperty(MAX_RANGE);
        LivingEntity target = findTargetEntity(player, reach);
        if (target != null && target != player && target.isAlive()) {
            // brief resistance to self to avoid reflect-like damage
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, true, false));

            // Base attack to trigger vanilla hooks/anim
            player.attack(target);
            if (finalDamage > 0.0f && target.isAlive()) {
                target.hurt(player.damageSources().playerAttack(player), finalDamage);
            }

            // Knockback
            if (finalKnockback > 0.0f && target.isAlive()) {
                Vec3 dir = target.position().subtract(player.position());
                if (dir.lengthSqr() > 1.0E-4) {
                    Vec3 knock = dir.normalize().scale(finalKnockback * 0.6).add(0, 0.12F * finalKnockback, 0);
                    target.setDeltaMovement(target.getDeltaMovement().add(knock));
                }
            }

            // Impact particles and optional shockwave ring
            float threshold = Math.max(0.0f, Math.min(1.0f, entry.getProperty(SHOCKWAVE_THRESHOLD)));
            spawnImpactVfx(level, target.position(), powerRatio, threshold);

            // High-power shockwave knockback affects all nearby living entities (including players)
            if (powerRatio >= threshold) {
                double shockRadius = 2.5 + 5.5 * powerRatio;
                AABB area = new AABB(
                        target.getX() - shockRadius, target.getY() - 1.0, target.getZ() - shockRadius,
                        target.getX() + shockRadius, target.getY() + 2.0, target.getZ() + shockRadius);
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != null && e.isAlive() && e != player);
                for (LivingEntity e : nearby) {
                    Vec3 dir = e.position().subtract(target.position());
                    double dist = Math.max(0.25, dir.length());
                    Vec3 norm = dir.scale(1.0 / dist);
                    // Knockback scales down with distance, up with power
                    double strength = (finalKnockback * 0.5) * (1.0 + powerRatio) * Math.max(0.25, 1.0 - (dist / shockRadius));
                    Vec3 kb = new Vec3(norm.x * strength, Math.max(0.15, norm.y + 0.2) * (0.8 + powerRatio * 0.4), norm.z * strength);
                    e.setDeltaMovement(e.getDeltaMovement().add(kb));
                }
            }
        } else {
            // Miss feedback
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 hand = eye.add(look.scale(1.4));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, hand.x, hand.y - 0.2, hand.z, 4, 0.15, 0.15, 0.15, 0.0);
        }

        entry.setUniqueProperty(CHARGE_TICKS, 0);
    }

    private LivingEntity findTargetEntity(Player player, float range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        EntityHitResult hit = null;
        double closest = range;

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eyePos, endPos).inflate(1.0),
                e -> e != player && e.isAlive());

        for (LivingEntity e : entities) {
            if (e.getBoundingBox().inflate(0.3).clip(eyePos, endPos).isPresent()) {
                double dist = eyePos.distanceTo(e.position());
                if (dist < closest) {
                    closest = dist;
                    hit = new EntityHitResult(e);
                }
            }
        }

        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private void spawnImpactVfx(ServerLevel level, Vec3 pos, float powerRatio, float shockwaveThreshold) {
        // Core crit burst
        int crit = Math.max(2, (int) Math.round(6 + 14 * powerRatio));
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.5, pos.z, crit, 0.35, 0.25, 0.35, 0.15 * powerRatio);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.2, pos.z, Math.max(4, crit / 2), 0.4, 0.3, 0.4, 0.05);

        // Shockwave ring if sufficiently charged
        if (powerRatio >= shockwaveThreshold) {
            // Firework-like visual: central flash and expanding particle ring over time using AreaEffectCloud
            level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 0.2, pos.z, 1, 0.0, 0.0, 0.0, 0.0);

            double maxRadius = 2.0 + 4.0 * powerRatio;
            int duration = 12 + (int)(powerRatio * 10); // 12-22 ticks
            float radiusPerTick = (float)(maxRadius / duration);

            // Layered expanding discs to approximate a growing globe
            int layers = 3 + (int)Math.floor(powerRatio * 3); // 3-6 layers
            double halfSpan = 0.45 + 0.35 * powerRatio; // vertical span
            for (int i = 0; i < layers; i++) {
                double t = layers == 1 ? 0.0 : (i / (double)(layers - 1));
                double yo = -halfSpan + t * (2 * halfSpan);

                // Cloud layer
                AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y + yo, pos.z);
                cloud.setRadius(0.2f);
                cloud.setRadiusPerTick(radiusPerTick);
                cloud.setDuration(duration);
                cloud.setParticle(ParticleTypes.CLOUD);
                cloud.setNoGravity(true);
                level.addFreshEntity(cloud);

                // Sparkle layer
                AreaEffectCloud sparks = new AreaEffectCloud(level, pos.x, pos.y + yo + 0.05, pos.z);
                sparks.setRadius(0.15f);
                sparks.setRadiusPerTick(radiusPerTick);
                sparks.setDuration(duration);
                sparks.setParticle(ParticleTypes.END_ROD);
                sparks.setNoGravity(true);
                level.addFreshEntity(sparks);
            }
        }
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}


