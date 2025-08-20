package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.entities.WindProjectileEntity;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

public class WindProjectileAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> MAX_RANGE = new FloatProperty("max_range").configurable("Maximum range of the air projectile");
    public static final PalladiumProperty<Float> MAX_DAMAGE = new FloatProperty("max_damage").configurable("Maximum damage at full power");
    public static final PalladiumProperty<Float> MAX_KNOCKBACK = new FloatProperty("max_knockback").configurable("Maximum knockback force at full power");
    public static final PalladiumProperty<Float> DAMAGE_THRESHOLD = new FloatProperty("damage_threshold").configurable("Power stock threshold where damage starts (0-100000)");
    public static final PalladiumProperty<Integer> PROJECTILE_SPEED = new IntegerProperty("projectile_speed").configurable("Speed of the projectile in blocks per tick");
    public static final PalladiumProperty<Float> PROJECTILE_WIDTH = new FloatProperty("projectile_width").configurable("Width of the projectile hitbox");

    // These are used as denominators to the power ratio, to decrease the amount gained per 10,000 power
    private static final float DAMAGE_SCALER = 2.0f;
    private static final float KNOCKBACK_SCALER = 5.0f;
    private static final float BOOST_SCALER = 2.0f;

    private static final float BASE_KNOCKBACK = 0.3f;
    private static final float BASE_RANGE = 1.0f;
    private static final float BASE_BOOST = 1.0f;
    private static final float MAX_BOOST_STRENGTH = 5.0f;

    public WindProjectileAbility() {
        super();
        this.withProperty(MAX_RANGE, 30.0F)
                .withProperty(MAX_DAMAGE, 12.0F)
                .withProperty(MAX_KNOCKBACK, 4.0F)
                .withProperty(DAMAGE_THRESHOLD, 10000.0F) // Start doing damage at 10k power stock
                .withProperty(PROJECTILE_SPEED, 2)
                .withProperty(PROJECTILE_WIDTH, 1.5F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {

            if (player.isFallFlying()) {
                giveElytraBoost(player);
            } else {
                executeAirForce(player, entry);
            }
        }
    }

    private void executeAirForce(ServerPlayer player, AbilityInstance entry) {
        ServerLevel level = player.serverLevel();
        
        // Get power stock from chest
        float powerStock = BodyStatusHelper.getCustomFloat(player, "chest", "pstock_stored");
        
        // Calculate power scaling - linear growth of ability every 10,000 power
        float powerRatio = powerStock / 10000.0F;
        
        // Get configurable values
        float maxRange = entry.getProperty(MAX_RANGE);
        float maxDamage = entry.getProperty(MAX_DAMAGE);
        float maxKnockback = entry.getProperty(MAX_KNOCKBACK);
        float damageThreshold = entry.getProperty(DAMAGE_THRESHOLD);
        int projectileSpeed = entry.getProperty(PROJECTILE_SPEED);
        float projectileWidth = entry.getProperty(PROJECTILE_WIDTH);
        
        // Calculate actual values based on power
        float currentRange = Math.min(maxRange, BASE_RANGE + powerRatio); // Extend range by 1 for every 10,000 power
        float currentKnockback = Math.min(maxKnockback, BASE_KNOCKBACK + powerRatio / KNOCKBACK_SCALER); // Extend knockback by 0.2 every 10,000 power
        float currentDamage = powerStock >= damageThreshold ? Math.min(maxDamage, powerRatio / DAMAGE_SCALER) : 0.0F; // After threshold, do 0.5 damage every 10,000 power

        Vec3 lookDirection = player.getLookAngle();
        
        // Create bounded ratio for visual/audio effects (0.0 to 1.0)
        float visualRatio = Math.min(1.0F, powerRatio / 10.0F); // Cap at power level 100,000 for max visual effects
        
        // Play sound effect
        float pitch = 0.8F + (visualRatio * 0.4F); // Higher pitch for more power
        float volume = 0.5F + (visualRatio * 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, volume, pitch);
        
        // Create the real projectile entity
        createWindProjectileEntity(level, player, lookDirection, currentDamage, currentKnockback, visualRatio, projectileSpeed, currentRange);
        
//        BanditsQuirkLibForge.LOGGER.info("power: {}, damage: {}, knockback: {}, speed: {}, range: {}",
//                                       powerStock, currentDamage, currentKnockback, projectileSpeed, currentRange);
    }

    private void createWindProjectileEntity(ServerLevel level, ServerPlayer shooter, Vec3 direction, 
                                          float damage, float knockback, float visualRatio, int speed, float range) {
        // Create the wind projectile entity
        WindProjectileEntity projectile = new WindProjectileEntity(level, shooter, damage, knockback, visualRatio, range);
        
        // Set position at player's eye level
        Vec3 startPos = shooter.getEyePosition();
        projectile.setPos(startPos.x, startPos.y, startPos.z);
        
        // Set velocity based on look direction and speed
        Vec3 velocity = direction.scale(speed * 0.5); // Scale down for realistic projectile speed
        projectile.setDeltaMovement(velocity);
        
        // Spawn the entity
        level.addFreshEntity(projectile);
    }

    private static void giveElytraBoost(ServerPlayer player) {
        Vec3 lookDirection = player.getLookAngle();

        // Get power stock from chest
        float powerStock = BodyStatusHelper.getCustomFloat(player, "chest", "pstock_stored");

        // Calculate power scaling - linear growth of speed every 10,000 power
        float powerRatio = powerStock / 10000.0F;
        // We have to hard cap boost strength, because the higher it gets, the more unstable the control becomes
        float boostStrength = Math.min(MAX_BOOST_STRENGTH, BASE_BOOST + powerRatio / BOOST_SCALER);
        Vec3 boostVelocity = lookDirection.scale(boostStrength);
        player.setDeltaMovement(boostVelocity);

        // Send velocity update to client
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        addBoostEffects(player);

    }

    private static void addBoostEffects(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                1.0f, 1.0f);

        createBoostParticleRing((ServerLevel) player.level(), player);
    }

    private static void createBoostParticleRing(ServerLevel level, ServerPlayer player) {
        // Get player position at feet level
        Vec3 position = new Vec3(player.getX(), player.getY(), player.getZ());

        // Get player's look direction for ring orientation
        Vec3 direction = player.getLookAngle();

        // Create two perpendicular vectors to the direction for ring orientation
        Vec3 perpendicular1, perpendicular2;

        // Find a vector that's not parallel to direction
        if (Math.abs(direction.y) < 0.9) {
            perpendicular1 = new Vec3(0, 1, 0).cross(direction).normalize();
        } else {
            perpendicular1 = new Vec3(1, 0, 0).cross(direction).normalize();
        }

        // Second perpendicular vector
        perpendicular2 = direction.cross(perpendicular1).normalize();

        // Ring parameters - adjust for boost visual effect
        int particleCount = 16;
        double ringRadius = 2.4;

        // Create main boost ring at player's feet
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;

            // Calculate position on ring using perpendicular vectors
            Vec3 ringOffset = perpendicular1.scale(Math.cos(angle) * ringRadius)
                    .add(perpendicular2.scale(Math.sin(angle) * ringRadius));

            Vec3 particlePos = position.add(ringOffset);

            // Main ring particles - using CLOUD for visible effect
            level.sendParticles(ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.05, 0.05, 0.05, 0.02);

            // Add some FIREWORK particles for extra flair
            level.sendParticles(ParticleTypes.FIREWORK,
                    particlePos.x, particlePos.y + 0.1, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }

        // Create an inner ring with different particles for layered effect
        double innerRadius = ringRadius * 0.6;
        int innerParticles = particleCount / 2;

        for (int i = 0; i < innerParticles; i++) {
            double angle = (2 * Math.PI * i) / innerParticles;

            Vec3 innerOffset = perpendicular1.scale(Math.cos(angle) * innerRadius)
                    .add(perpendicular2.scale(Math.sin(angle) * innerRadius));

            Vec3 innerPos = position.add(innerOffset);

            // Inner ring with POOF particles
            level.sendParticles(ParticleTypes.POOF,
                    innerPos.x, innerPos.y + 0.05, innerPos.z,
                    1, 0.02, 0.02, 0.02, 0.01);
        }

        // Add some upward particles at the center for extra effect
        level.sendParticles(ParticleTypes.FIREWORK,
                position.x, position.y, position.z,
                5, 0.2, 0.1, 0.2, 0.1);
    }

    @Override
    public String getDocumentationDescription() {
        return "Creates a real wind projectile entity (air force) that travels in the direction the player is looking. " +
               "Power is determined by the 'pstock_stored' value in the chest body part (0-100,000 for full power). " +
               "At low power levels, only provides knockback. At higher power levels (above damage threshold), " +
               "also deals damage. The projectile can be blocked like arrows, creates particle effects, and is not affected by gravity.";
    }
}