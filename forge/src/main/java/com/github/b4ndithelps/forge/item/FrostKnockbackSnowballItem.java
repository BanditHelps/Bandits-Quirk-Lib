package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.forge.entities.FrostKnockbackSnowballEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;

public class FrostKnockbackSnowballItem extends SnowballItem {

    private static final String TAG_KNOCKBACK = "Knockback";
    private static final String TAG_REINFORCED_DAMAGE = "ReinforcedDamage";
    private static final String TAG_STONE_CORE = "HasStoneCore";
    private static final float DEFAULT_KNOCKBACK = 0.9f;

    public FrostKnockbackSnowballItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4F + 0.8f));

        if (!level.isClientSide) {
            FrostKnockbackSnowballEntity projectile = new FrostKnockbackSnowballEntity(level, player);
            projectile.setKnockbackStrength(getKnockback(stack));
            projectile.setReinforcedDamage(getReinforcedDamage(stack));
            projectile.setHasStoneCore(hasStoneCore(stack));
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static ItemStack imprintBehavior(ItemStack stack, float knockback, float reinforcedDamage, boolean stoneCore) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat(TAG_KNOCKBACK, knockback);
        tag.putFloat(TAG_REINFORCED_DAMAGE, reinforcedDamage);

        if (stoneCore) {
            tag.putBoolean(TAG_STONE_CORE, true);
        } else {
            tag.remove(TAG_STONE_CORE);
        }

        return stack;
    }

    public static float getKnockback(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_KNOCKBACK) ? tag.getFloat(TAG_KNOCKBACK) : DEFAULT_KNOCKBACK;
    }

    public static float getReinforcedDamage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_REINFORCED_DAMAGE) ? tag.getFloat(TAG_REINFORCED_DAMAGE) : 0.0F;
    }

    public static boolean hasStoneCore(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_STONE_CORE);
    }
}
