package com.github.b4ndithelps.forge.abilities.frog;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

public class StoreAbility extends Ability {
    private static final String STORED_ITEM_KEY = "frog_stored_item";

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        CompoundTag playerData = player.getPersistentData();

        float cd = BodyStatusHelper.getCustomFloat(player, "chest", "frog_stored_cd");

        if (cd > 0 && playerData.contains(STORED_ITEM_KEY)) {
            CompoundTag itemTag = playerData.getCompound(STORED_ITEM_KEY);
            ItemStack storedItem = ItemStack.of(itemTag);

            player.getPersistentData().remove(STORED_ITEM_KEY);

            // Try to add to inventory
            boolean added = player.getInventory().add(storedItem);

            // If inventory is full, drop at player location
            if (!added) {
                player.drop(storedItem, false);
            }

            return ;
        }

        ItemStack heldItem = player.getMainHandItem();
        ItemStack singleItem = heldItem.copy();
        singleItem.setCount(1);



        if (heldItem.isEmpty()) {
            return ;
        }

        CompoundTag itemTag = new CompoundTag();
        singleItem.save(itemTag);
        heldItem.shrink(1);
        playerData.put(STORED_ITEM_KEY, itemTag);
        BodyStatusHelper.setCustomFloat(player, "chest", "frog_stored_cd", 20);

    }

}
