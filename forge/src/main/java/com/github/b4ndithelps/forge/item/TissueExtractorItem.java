package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.genetics.GeneticsHelper;
import com.github.b4ndithelps.forge.config.BQLConfig;
import com.github.b4ndithelps.genetics.SequenceGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TissueExtractorItem extends Item {
    public TissueExtractorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            LivingEntity target = getTargetedLivingEntity(player);
            if (target != null && isValidTarget(target)) {

                int minCount;
                int maxCount;
                if (target instanceof Player) {
                    minCount = maxCount = BQLConfig.INSTANCE.seqLenPlayer.get();
                } else if (target instanceof Villager) {
                    var r = BQLConfig.INSTANCE.seqLenVillagerRange.get();
                    minCount = r.get(0);
                    maxCount = r.get(1);
                } else if (target instanceof Zombie) {
                    var r = BQLConfig.INSTANCE.seqLenZombieRange.get();
                    minCount = r.get(0);
                    maxCount = r.get(1);
                } else if (target instanceof Husk) {
                    var r = BQLConfig.INSTANCE.seqLenHuskRange.get();
                    minCount = r.get(0);
                    maxCount = r.get(1);
                } else if (target instanceof Drowned) {
                    var r = BQLConfig.INSTANCE.seqLenDrownedRange.get();
                    minCount = r.get(0);
                    maxCount = r.get(1);
                } else {
                    minCount = 2; maxCount = 3;
                }

                var instances = SequenceGenerator.generateForEntity(target, minCount, maxCount);

                ItemStack sample = new ItemStack(ModItems.TISSUE_SAMPLE.get());
                var tag = sample.getOrCreateTag();
                tag.putString("entity_name", target.getName().getString());
                tag.putString("entity_uuid", target.getUUID().toString());
                tag.putLong("layout_salt", level.getRandom().nextLong());
                ListTag genes = new ListTag();
                for (int i = 0; i < instances.size(); i++) {
                    var gi = instances.get(i);
                    CompoundTag g = new CompoundTag();
                    g.putString("id", gi.id.toString());
                    g.putInt("quality", gi.quality);
                    // Stable display name per entity/gene/index
                    String display = GeneticsHelper.generateStableGeneName(target.getUUID(), gi.id, i);
                    g.putString("name", display);
                    genes.add(g);
                }
                tag.put("genes", genes);

                if (!player.addItem(sample)) {
                    player.drop(sample, false);
                }

                // Play sound and optionally damage target per config
                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.2f);
                if (BQLConfig.INSTANCE.extractorDamageTarget.get()) {
                    float dmg = BQLConfig.INSTANCE.extractorDamageAmount.get().floatValue();
                    target.hurt(level.damageSources().playerAttack(player), dmg);
                }

                // Durability loss from config
                int cost = Math.max(1, BQLConfig.INSTANCE.extractorDurabilityCost.get());
                stack.hurtAndBreak(cost, player, p -> p.broadcastBreakEvent(hand));

                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private LivingEntity getTargetedLivingEntity(Player player) {
        HitResult hit = player.pick(4.5, 0.0f, false);
        if (hit == null) return null;

        // Cast out and grab the entity that is in front of the player that they hit
        // Determines the closest
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(4.5));
        AABB aabb = new AABB(start, end).inflate(1.0);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, aabb, e -> e != player && e.isPickable());
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : entities) {
            double dist = e.distanceToSqr(player);
            if (dist < closestDist) {
                closest = e;
                closestDist = dist;
            }
        }
        return closest;
    }

    private boolean isValidTarget(LivingEntity target) {
        try {
            var list = BQLConfig.INSTANCE.extractorValidEntityTypes.get();
            if (list != null && !list.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                String key = id != null ? id.toString() : "";
                return list.contains(key);
            }
        } catch (Exception ignored) {}
        return GeneticsHelper.isEntityHumanoid(target);
    }
}