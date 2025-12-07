package com.github.b4ndithelps.forge.abilities.frog;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.Block;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

import java.util.concurrent.atomic.AtomicBoolean;

public class StoreCooldownAbility extends Ability {
    private static final String STORED_ITEM_KEY = "frog_stored_item";

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        float cd = BodyStatusHelper.getCustomFloat(player, "chest", "frog_stored_cd");

        if (cd > 0 ) {
            BodyStatusHelper.setCustomFloat(player, "chest", "frog_stored_cd", --cd);
        }  else if (cd == 0) {

            CompoundTag playerData = player.getPersistentData();

            CompoundTag itemTag = playerData.getCompound(STORED_ITEM_KEY);
            ItemStack stack = ItemStack.of(itemTag);

            if (stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotion(stack);
                for (MobEffectInstance effect : potion.getEffects()) {
                    player.addEffect(new MobEffectInstance(effect));
                }
                MutableComponent component = Component.literal("Your stomach digests the potion!").withStyle(ChatFormatting.DARK_GREEN);

                ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
                player.connection.send(packet);
            } else if (stack.isEdible()) {
                player.eat(player.level(), stack);
                MutableComponent component = Component.literal("Your stomach happily digests the food!").withStyle(ChatFormatting.DARK_GREEN);

                ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
                player.connection.send(packet);
            } else if (stack.getItem().getRarity(stack) == Rarity.EPIC) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1, false, false));
                MutableComponent component = Component.literal("Your stomach couldn't disolve the item!")
                        .withStyle(ChatFormatting.RED);
                ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
                player.connection.send(packet);
                player.drop(stack, false);
            }else if (stack.isDamageableItem()) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                AtomicBoolean willBreak = new AtomicBoolean(false);
                ItemStack copy = stack.copy();

                copy.hurtAndBreak(300, player, (p) -> {;
                    willBreak.set(true);
                });

                if (willBreak.get()) {
                    dissolveItem(player, stack);
                } else {
                    stack.hurtAndBreak(300, player, (p) -> {;
                        p.broadcastBreakEvent(player.getUsedItemHand());
                    });
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1, false, false));
                    MutableComponent component = Component.literal("Your stomach spits out the durable item")
                            .withStyle(ChatFormatting.RED);
                    ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
                    player.connection.send(packet);
                    player.drop(stack, false);
                }
            } else {
                Block block = Block.byItem(stack.getItem());
                if (block != null && !block.defaultBlockState().isAir()) {
                    float resistance = block.getExplosionResistance();
                    if (resistance <= 600.0f) {
                        dissolveItem(player, stack);
                    } else {
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1, false, false));
                        MutableComponent component = Component.literal("Your stomach couldn't disolve the block and spit it out!")
                                .withStyle(ChatFormatting.RED);
                        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
                        player.connection.send(packet);
                        player.drop(stack, false);
                    }
                } else {
                    dissolveItem(player, stack);
                }
            }

            BodyStatusHelper.setCustomFloat(player, "chest", "frog_stored_cd", --cd);
        }

    }

    private void dissolveItem(ServerPlayer player, ItemStack stack) {
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 1, false, false));
        MutableComponent component = Component.literal("Your stomach dissolves the stored item!")
                .withStyle(ChatFormatting.RED);
        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
        player.connection.send(packet);
    }

}
