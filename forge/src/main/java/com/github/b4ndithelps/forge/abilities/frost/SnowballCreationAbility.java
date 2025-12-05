package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.forge.item.FrostKnockbackSnowballItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

public class SnowballCreationAbility extends Ability {
    public static final PalladiumProperty<Integer> SNOWBALL_COUNT =
            new IntegerProperty("snowball_count").configurable("Number of snowballs created per activation");
    public static final PalladiumProperty<Float> SPAWN_RADIUS =
            new FloatProperty("spawn_radius").configurable("Horizontal radius (in blocks) where snowballs may appear");
    public static final PalladiumProperty<Float> VERTICAL_VARIANCE =
            new FloatProperty("vertical_variance").configurable("Random Y offset added to the drop height");
    public static final PalladiumProperty<Float> BASE_KNOCKBACK =
            new FloatProperty("base_knockback").configurable("Knockback strength applied by each snowball");
    public static final PalladiumProperty<Float> REINFORCED_DAMAGE =
            new FloatProperty("reinforced_damage").configurable("Damage dealt when a stone core is embedded");
    public static final PalladiumProperty<Boolean> ADD_MOMENTUM =
            new BooleanProperty("add_momentum").configurable("Give spawned items a slight outward motion");

    public SnowballCreationAbility() {
        this.withProperty(SNOWBALL_COUNT, 1)
                .withProperty(SPAWN_RADIUS, 1.8F)
                .withProperty(VERTICAL_VARIANCE, 0.45F)
                .withProperty(BASE_KNOCKBACK, 1.1F)
                .withProperty(REINFORCED_DAMAGE, 3.5F)
                .withProperty(ADD_MOMENTUM, true);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        RandomSource random = serverLevel.getRandom();
        int count = Math.max(1, entry.getProperty(SNOWBALL_COUNT));
        float radius = Math.max(0.1F, entry.getProperty(SPAWN_RADIUS));
        float variance = Mth.clamp(entry.getProperty(VERTICAL_VARIANCE), 0.0F, 2.0F);
        float knockback = Math.max(0.1F, entry.getProperty(BASE_KNOCKBACK));
        float reinforcedDamage = Math.max(0.0F, entry.getProperty(REINFORCED_DAMAGE));
        boolean addMomentum = entry.getProperty(ADD_MOMENTUM);

        for (int i = 0; i < count; i++) {
            boolean reinforced = tryConsumeStone(player);
            ItemStack stack = createConfiguredSnowball(knockback, reinforcedDamage, reinforced);
            dropConfiguredStack(serverLevel, player, stack, radius, variance, addMomentum, random);
        }

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 0.5F, 1.1F);
    }

    private ItemStack createConfiguredSnowball(float knockback, float reinforcedDamage, boolean reinforced) {
        ItemStack stack = new ItemStack(ModItems.FROST_KNOCKBACK_SNOWBALL.get());
        FrostKnockbackSnowballItem.imprintBehavior(stack, knockback, reinforcedDamage, reinforced);
        return stack;
    }

    private void dropConfiguredStack(ServerLevel level, Player player, ItemStack stack, float radius, float variance,
                                     boolean addMomentum, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * radius;
        Vec3 base = player.position();
        double x = base.x + Math.cos(angle) * distance;
        double z = base.z + Math.sin(angle) * distance;
        double y = base.y + 0.1 + (random.nextDouble() - 0.5) * variance;

        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        if (addMomentum) {
            double motionScale = 0.08 + random.nextDouble() * 0.05;
            Vec3 motion = new Vec3(Math.cos(angle), 0.05, Math.sin(angle)).scale(motionScale);
            entity.setDeltaMovement(motion);
        }
        level.addFreshEntity(entity);
    }

    private boolean tryConsumeStone(Player player) {
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offhand.isEmpty()) {
            return false;
        }
        if (!(offhand.is(Items.COBBLESTONE) || offhand.is(Items.STONE))) {
            return false;
        }
        offhand.shrink(1);
        if (offhand.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Drops custom snowball ammunition at the caster's feet. Snowballs inherit knockback values from the ability, "
                + "and consume cobblestone or stone from the off-hand to embed a damaging core.";
    }
}
