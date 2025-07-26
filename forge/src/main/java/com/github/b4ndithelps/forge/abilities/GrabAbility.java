package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.StringProperty;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
public class GrabAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum range to grab entities");
    public static final PalladiumProperty<String> MODE = new StringProperty("mode").configurable("Grab mode: 'hold' or 'hold_and_carry'");
    public static final PalladiumProperty<String> POTION_EFFECT = new StringProperty("potion_effect").configurable("Optional potion effect to apply in hold mode (e.g., 'minecraft:slowness')");
    public static final PalladiumProperty<Integer> EFFECT_AMPLIFIER = new IntegerProperty("effect_amplifier").configurable("Amplifier for the potion effect (0-255)");
    public static final PalladiumProperty<Float> CARRY_HEIGHT = new FloatProperty("carry_height").configurable("Height offset for carried entities");
    public static final PalladiumProperty<Float> CARRY_DISTANCE = new FloatProperty("carry_distance").configurable("Distance in front of player for carried entities");
    public static final PalladiumProperty<Float> DAMAGE_THRESHOLD = new FloatProperty("damage_threshold").configurable("Amount of damage needed to break the grab (0 = disabled)");

    // Unique properties for tracking grab state
    public static final PalladiumProperty<String> TARGET_UUID;
    public static final PalladiumProperty<Float> ORIGINAL_X;
    public static final PalladiumProperty<Float> ORIGINAL_Y;
    public static final PalladiumProperty<Float> ORIGINAL_Z;
    public static final PalladiumProperty<Float> PLAYER_ORIGINAL_X;
    public static final PalladiumProperty<Float> PLAYER_ORIGINAL_Y;
    public static final PalladiumProperty<Float> PLAYER_ORIGINAL_Z;
    public static final PalladiumProperty<Float> PLAYER_START_HEALTH;
    public static final PalladiumProperty<Boolean> IS_GRABBING;
    public static final PalladiumProperty<Integer> GRAB_TICKS;

    public GrabAbility() {
        super();
        this.withProperty(RANGE, 5.0F)
                .withProperty(MODE, "hold")
                .withProperty(POTION_EFFECT, "")
                .withProperty(EFFECT_AMPLIFIER, 0)
                .withProperty(CARRY_HEIGHT, 1.5F)
                .withProperty(CARRY_DISTANCE, 3.0F)
                .withProperty(DAMAGE_THRESHOLD, 3.0F);
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(TARGET_UUID, "");
        manager.register(ORIGINAL_X, 0.0F);
        manager.register(ORIGINAL_Y, 0.0F);
        manager.register(ORIGINAL_Z, 0.0F);
        manager.register(PLAYER_ORIGINAL_X, 0.0F);
        manager.register(PLAYER_ORIGINAL_Y, 0.0F);
        manager.register(PLAYER_ORIGINAL_Z, 0.0F);
        manager.register(PLAYER_START_HEALTH, 0.0F);
        manager.register(IS_GRABBING, false);
        manager.register(GRAB_TICKS, 0);
    }

    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {

            // Reset grab state
            entry.setUniqueProperty(IS_GRABBING, false);
            entry.setUniqueProperty(TARGET_UUID, "");
            entry.setUniqueProperty(GRAB_TICKS, 0);
            
            // Find target entity
            LivingEntity target = findTargetEntity(player, entry.getProperty(RANGE));
            
            if (target != null && target != player) {
                // Check if target has grab immunity
                if (target.getTags().contains("MineHa.GrabProof")) {
                    player.sendSystemMessage(Component.literal("§cThe target resists your grasp!"));
                    return;
                }
                
                // Start grab
                entry.setUniqueProperty(IS_GRABBING, true);
                entry.setUniqueProperty(TARGET_UUID, target.getUUID().toString());
                entry.setUniqueProperty(ORIGINAL_X, (float) target.getX());
                entry.setUniqueProperty(ORIGINAL_Y, (float) target.getY());
                entry.setUniqueProperty(ORIGINAL_Z, (float) target.getZ());
                entry.setUniqueProperty(PLAYER_ORIGINAL_X, (float) player.getX());
                entry.setUniqueProperty(PLAYER_ORIGINAL_Y, (float) player.getY());
                entry.setUniqueProperty(PLAYER_ORIGINAL_Z, (float) player.getZ());
                entry.setUniqueProperty(PLAYER_START_HEALTH, (float) player.getHealth());
                
                // Play grab sound
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, player.blockPosition(),
                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 1.8f);
                }
                
                // Apply initial effect for hold mode
                if ("hold".equals(entry.getProperty(MODE))) {
                    applyHoldEffects(player, target, entry);
                }
            } else {
                // No valid target found, ability fails
                BanditsQuirkLibForge.LOGGER.info("No valid target found for grab ability");
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled || !entry.getProperty(IS_GRABBING)) return;

        int currentTicks = entry.getProperty(GRAB_TICKS);
        entry.setUniqueProperty(GRAB_TICKS, currentTicks + 1);

        // Find the target entity
        String targetUuidStr = entry.getProperty(TARGET_UUID);
        if (targetUuidStr.isEmpty()) return;

        try {
            UUID targetUuid = UUID.fromString(targetUuidStr);
            LivingEntity target = findEntityByUuid(player, targetUuid);
            
            if (target == null || target.isDeadOrDying()) {
                // Target is gone, end the grab
                endGrab(entry);
                return;
            }

            // Check if target gained grab immunity during the grab
            if (target.getTags().contains("MineHa.GrabProof")) {
                player.sendSystemMessage(Component.literal("§cThe target breaks free of your grasp!"));
                endGrab(entry);
                player.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), 60, 1, false, false));
                
                // Play immunity sound
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, target.blockPosition(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
                return;
            }

            // Check if target is still in range
            double distance = player.distanceTo(target);
            if (distance > entry.getProperty(RANGE) * 2) { // Allow some extra range during grab
                endGrab(entry);
                return;
            }

            // Check if player has taken enough damage to break the grab
            float damageThreshold = entry.getProperty(DAMAGE_THRESHOLD) + (int)(QuirkFactorHelper.getQuirkFactor(player) * 2.0);
            if (damageThreshold > 0) {
                float startHealth = entry.getProperty(PLAYER_START_HEALTH);
                float currentHealth = player.getHealth();
                float damageTaken = startHealth - currentHealth;
                
                if (damageTaken >= damageThreshold) {
                    // Player took enough damage, break the grab
                    endGrab(entry);
                    
                    // Play break sound
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, player.blockPosition(),
                                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    return;
                }
            }

            String mode = entry.getProperty(MODE);
            
            if ("hold".equals(mode)) {
                executeHoldMode(player, target, entry);
            } else if ("hold_and_carry".equals(mode)) {
                executeCarryMode(player, target, entry);
            }

            // Add particles around target
            if (entity.level() instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position();
                for (int i = 0; i < 3; i++) {
                    double offsetX = (Math.random() - 0.5) * 2.0;
                    double offsetY = Math.random() * 2.0;
                    double offsetZ = (Math.random() - 0.5) * 2.0;
                    serverLevel.sendParticles(ParticleTypes.ASH,
                            targetPos.x + offsetX, targetPos.y + offsetY, targetPos.z + offsetZ,
                            1, 0, 0, 0, 0);
                }
            }

        } catch (IllegalArgumentException e) {
            BanditsQuirkLibForge.LOGGER.error("Invalid UUID in grab ability: " + targetUuidStr);
            endGrab(entry);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer player) {
            // Clean up grab state
            String targetUuidStr = entry.getProperty(TARGET_UUID);
            if (!targetUuidStr.isEmpty()) {
                try {
                    UUID targetUuid = UUID.fromString(targetUuidStr);
                    LivingEntity target = findEntityByUuid(player, targetUuid);
                    if (target != null) {
                        // Remove any effects applied by the grab
                        cleanupTargetEffects(target, entry);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, just continue cleanup
                }
            }
            
            endGrab(entry);
        }
    }

    private LivingEntity findTargetEntity(ServerPlayer player, float range) {
        // Raycast to find entity player is looking at
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        EntityHitResult entityHit = null;
        double closestDistance = range;

        // Check for entities in the line of sight
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eyePos, endPos).inflate(1.0),
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            AABB entityBB = entity.getBoundingBox().inflate(0.3);
            Vec3 hitPos = entityBB.clip(eyePos, endPos).orElse(null);
            
            if (hitPos != null) {
                double distance = eyePos.distanceTo(hitPos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    entityHit = new EntityHitResult(entity, hitPos);
                }
            }
        }

        // If no direct hit, find closest entity within range
        if (entityHit == null) {
            List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(range),
                    e -> e != player && e.isAlive());

            LivingEntity closest = null;
            double closestDist = range;

            for (LivingEntity entity : nearbyEntities) {
                double dist = player.distanceTo(entity);
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }

            return closest;
        }

        return (LivingEntity) entityHit.getEntity();
    }

    private LivingEntity findEntityByUuid(ServerPlayer player, UUID uuid) {
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(50), // Large search area
                e -> e.getUUID().equals(uuid));
        
        return entities.isEmpty() ? null : entities.get(0);
    }

    private void executeHoldMode(ServerPlayer player, LivingEntity target, AbilityInstance entry) {
        // Keep target locked in original position
        target.teleportTo(entry.getProperty(ORIGINAL_X), 
                         entry.getProperty(ORIGINAL_Y), 
                         entry.getProperty(ORIGINAL_Z));
        
        // Prevent target movement
        target.setDeltaMovement(Vec3.ZERO);

        // Keep player locked in original position as well
        player.teleportTo(entry.getProperty(PLAYER_ORIGINAL_X), 
                         entry.getProperty(PLAYER_ORIGINAL_Y), 
                         entry.getProperty(PLAYER_ORIGINAL_Z));
        
        // Prevent player movement
        player.setDeltaMovement(Vec3.ZERO);
        
        // Apply potion effect if specified
        String effectName = entry.getProperty(POTION_EFFECT);
        if (!effectName.isEmpty() && entry.getProperty(GRAB_TICKS) % 20 == 0) { // Refresh every second
            applyPotionEffect(target, effectName, entry.getProperty(EFFECT_AMPLIFIER));
        }
    }

    private void executeCarryMode(ServerPlayer player, LivingEntity target, AbilityInstance entry) {
        // Calculate position in front of player based on crosshair
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getLookAngle();
        
        float distance = entry.getProperty(CARRY_DISTANCE);
        float height = entry.getProperty(CARRY_HEIGHT);
        
        Vec3 targetPos = playerPos.add(lookVec.scale(distance)).add(0, height, 0);
        
        // Teleport target to calculated position
        target.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        
        // Keep target stable
        target.setDeltaMovement(Vec3.ZERO);
        target.setOnGround(false);
        
        // Optional: Make target face the same direction as player
        target.setYRot(player.getYRot());
        target.setXRot(0); // Keep target upright
    }

    private void applyHoldEffects(ServerPlayer player, LivingEntity target, AbilityInstance entry) {
        // Apply initial potion effect for hold mode
        String effectName = entry.getProperty(POTION_EFFECT);
        int scaledPotionEffect = entry.getProperty(EFFECT_AMPLIFIER) + (int)(QuirkFactorHelper.getQuirkFactor(player) * 1);

        if (!effectName.isEmpty()) {
            applyPotionEffect(target, effectName, scaledPotionEffect);
        }
    }

    private void applyPotionEffect(LivingEntity target, String effectName, int amplifier) {
        try {
            ResourceLocation effectId = new ResourceLocation(effectName);
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
            
            if (effect != null) {
                MobEffectInstance effectInstance = new MobEffectInstance(effect, 40, amplifier); // 3 second duration
                target.addEffect(effectInstance);
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.warn("Invalid potion effect: " + effectName);
        }
    }

    private void cleanupTargetEffects(LivingEntity target, AbilityInstance entry) {
        // Remove any effects that were applied by the grab
        String effectName = entry.getProperty(POTION_EFFECT);
        if (!effectName.isEmpty()) {
            try {
                ResourceLocation effectId = new ResourceLocation(effectName);
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect != null) {
                    target.removeEffect(effect);
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    private void endGrab(AbilityInstance entry) {
        entry.setUniqueProperty(IS_GRABBING, false);
        entry.setUniqueProperty(TARGET_UUID, "");
        entry.setUniqueProperty(GRAB_TICKS, 0);
    }

    @Override
    public String getDocumentationDescription() {
        return "Allows grabbing and manipulating nearby living entities with two modes: 'hold' (locks both entities) and 'hold_and_carry' (allows moving the target). The grab can be broken if the player takes enough damage. Entities with the 'MineHa.GrabProof' tag cannot be grabbed and will be freed if they gain this tag during a grab.";
    }

    static {
        TARGET_UUID = new StringProperty("target_uuid");
        ORIGINAL_X = new FloatProperty("original_x");
        ORIGINAL_Y = new FloatProperty("original_y");
        ORIGINAL_Z = new FloatProperty("original_z");
        PLAYER_ORIGINAL_X = new FloatProperty("player_original_x");
        PLAYER_ORIGINAL_Y = new FloatProperty("player_original_y");
        PLAYER_ORIGINAL_Z = new FloatProperty("player_original_z");
        PLAYER_START_HEALTH = new FloatProperty("player_start_health");
        IS_GRABBING = new net.threetag.palladium.util.property.BooleanProperty("is_grabbing");
        GRAB_TICKS = new IntegerProperty("grab_ticks");
    }
} 