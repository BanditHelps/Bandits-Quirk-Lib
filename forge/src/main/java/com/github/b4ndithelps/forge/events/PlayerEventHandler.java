package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.abilities.HappenOnceAbility;
import com.github.b4ndithelps.forge.item.FactorTrigger;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import net.minecraft.network.chat.Component;
import com.github.b4ndithelps.forge.network.MineHaSlotSyncPacket;
import com.github.b4ndithelps.forge.network.StaminaSyncPacket;
import com.github.b4ndithelps.forge.systems.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.NoShadowTagPacket;
import com.github.b4ndithelps.forge.damage.ModDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.util.Mth;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = MOD_ID)
public class PlayerEventHandler {
    private static final Map<Integer, Boolean> LAST_NO_SHADOW_SENT = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {

            BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    StaminaSyncPacket.fullSync(sp));

            // Sync MineHa slot persistent keys to client after respawn (0..3 based on command usage)
            BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    MineHaSlotSyncPacket.fullSync(sp, 3));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        HappenOnceAbility.cleanupPlayerData(playerUUID);

        BanditsQuirkLibForge.LOGGER.info("Cleaned up player data");
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getEntity().getItem();

        if (dropped.getItem() instanceof FactorTrigger) {
            if (player.level().isClientSide) return;
            if (!((FactorTrigger) dropped.getItem()).isUsing()) return;
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                    new PlayerAnimationPacket("")
            );
            ((FactorTrigger) dropped.getItem()).setUsing(false);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeathDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            // reset ampule usage
            int tier = 0;
            int factor = 0;
            float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");
            if ((usage / 100) % 10 >= 1) {
                tier++;
                factor++;
            }
            if ((usage / 10) % 10 >= 1) {
                tier++;
            }
            if (usage % 10 >= 1) {
                tier++;
            }
            double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);

            BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", 0);
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", 0);
            BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddictionCD", 200+ 100*tier);
            QuirkFactorHelper.setQuirkFactor(player,  quirkFactor-factor - tier);
            SuperpowerUtil.removeSuperpower(player, ResourceLocation.parse("bql:ampule_use"));

            // Player died and dropped items
            for (ItemEntity item : event.getDrops()) {
                ItemStack stack = item.getItem();
                if (stack.getItem() instanceof FactorTrigger) {
                    ((FactorTrigger) stack.getItem()).setUsing(false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        var source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;

        // Only convert standard melee player attacks
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
        // Avoid recursion or double-processing if the damage already bypasses armor
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return;

        if (AbilityUtil.isEnabled(player, new ResourceLocation("mineha:permeation"), "permeation_punch")) {
            float amount = event.getAmount();

            if (player instanceof ServerPlayer sp) {
                double quirkFactor = QuirkFactorHelper.getQuirkFactor(sp);
                amount += (float)Math.min(4.0, quirkFactor);
            }

            event.setCanceled(true);
            boolean damaged = target.hurt(ModDamageTypes.permeationPunch(target.level(), player), amount);

            if (damaged) {
                double yawRad = player.getYRot() * ((float)Math.PI / 180F);
                int kbLevel = EnchantmentHelper.getKnockbackBonus(player);
                if (player.isSprinting()) kbLevel++;
                double strength = 0.4D + (kbLevel * 0.5D);
                if (strength > 0.0D) {
                    target.knockback(strength, Mth.sin((float)yawRad), -Mth.cos((float)yawRad));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        var source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) return;
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;

        if (!AbilityUtil.isEnabled(player, new ResourceLocation("mineha:permeation"), "permeation_punch")) return;

        event.setCanceled(true);

        double baseDamage = player.getAttribute(Attributes.ATTACK_DAMAGE) != null ? player.getAttribute(Attributes.ATTACK_DAMAGE).getValue() : 1.0;
        float cooldown = player.getAttackStrengthScale(0.5F);
        float attackDamage = (float)(baseDamage * (0.2F + cooldown * cooldown * 0.8F));

        ItemStack mainHand = player.getMainHandItem();
        float enchantBonus = EnchantmentHelper.getDamageBonus(mainHand, target.getMobType());
        if (enchantBonus > 0.0F) {
            attackDamage += enchantBonus * cooldown;
        }

        if (player instanceof ServerPlayer sp) {
            double quirkFactor = QuirkFactorHelper.getQuirkFactor(sp);
            attackDamage += (float)Math.min(4.0, quirkFactor);
        }

        if (attackDamage <= 0.0F) return;

        boolean damaged = target.hurt(ModDamageTypes.permeationPunch(target.level(), player), attackDamage);
        if (damaged) {
            double yawRad = player.getYRot() * ((float)Math.PI / 180F);
            int kbLevel = EnchantmentHelper.getKnockbackBonus(player);
            if (player.isSprinting()) kbLevel++;
            double strength = 0.4D + (kbLevel * 0.5D);
            if (strength > 0.0D) {
                target.knockback(strength, Mth.sin((float)yawRad), -Mth.cos((float)yawRad));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // Only runs if they haven't been here before
        boolean wasInitialized = StaminaHelper.initializePlayerStamina(player);

        // Since I store a "was initialized" inside of the StaminaHelper, just use it to decide if we need these again
        if (wasInitialized) {
            SuperpowerUtil.addSuperpower(player, ResourceLocation.parse("bql:base_quirk"));
            SuperpowerUtil.addSuperpower(player, ResourceLocation.parse("bql:body_status"));
        }

        if (player instanceof ServerPlayer sp) {

            // Send full stamina sync on login to ensure client-side GUI shows correct values
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    StaminaSyncPacket.fullSync(sp)
            );

            // Send MineHa slot full sync on login as well (0..3 slots)
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    MineHaSlotSyncPacket.fullSync(sp, 3)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) return;
        // Update per-tick double jump availability and cooldown
        DoubleJumpSystem.serverTick(player);
        // Apply genome-driven resistance and utility effects
        ResistanceSystem.applyGenomeBasedEffects(player);
        // Keep no-shadow flag in sync for permeation-related tags; only send when changed
        boolean wantNoShadow = player.getTags().contains("Bql.PermeateActive") || player.getTags().contains("Bql.PermeateRise") || player.getTags().contains("Bql.NoShadow");
        Boolean prev = LAST_NO_SHADOW_SENT.put(player.getId(), wantNoShadow);
        if (prev == null || prev != wantNoShadow) {
            BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new NoShadowTagPacket(player.getId(), wantNoShadow));
        }
        
        // Auto-switch handedness based on destroyed arms for better visuals
        // Store the player's original handedness once, then flip when one arm is destroyed
        boolean storedOriginal = player.getPersistentData().contains("Bql.OrigLeftHanded");
        if (!storedOriginal) {
            player.getPersistentData().putBoolean("Bql.OrigLeftHanded", player.getMainArm() == HumanoidArm.LEFT);
        }

        boolean rightArmDestroyed = BodyStatusHelper.isPartDestroyed(player, "right_arm");
        boolean leftArmDestroyed = BodyStatusHelper.isPartDestroyed(player, "left_arm");

        boolean desiredLeftHanded;
        if (rightArmDestroyed && !leftArmDestroyed) {
            // Use left as main if right arm is destroyed
            desiredLeftHanded = true;
        } else if (leftArmDestroyed && !rightArmDestroyed) {
            // Use right as main if left arm is destroyed
            desiredLeftHanded = false;
        } else {
            // Neither or both destroyed: restore original preference
            desiredLeftHanded = player.getPersistentData().getBoolean("Bql.OrigLeftHanded");
        }

        if ((player.getMainArm() == HumanoidArm.LEFT) != desiredLeftHanded) {
            player.setMainArm(desiredLeftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        }

        // Quad Zip collision damage window: apply damage/knockback to every entity passed through during the window
        if (player.getPersistentData().contains("Bql.QZipTicks")) {
            int ticks = player.getPersistentData().getInt("Bql.QZipTicks");
            if (ticks > 0) {
                player.getPersistentData().putInt("Bql.QZipTicks", ticks - 1);
                Vec3 prevPos = new Vec3(player.xOld, player.yOld, player.zOld);
                Vec3 currPos = player.position();
                AABB swept = new AABB(prevPos, currPos).inflate(0.6);
                List<LivingEntity> hits = player.level().getEntitiesOfClass(LivingEntity.class, swept, e -> e != null && e != player && e.isAlive());
                CompoundTag hitSet = player.getPersistentData().getCompound("Bql.QZipHits");
                for (LivingEntity e : hits) {
                    String idKey = Integer.toString(e.getId());
                    if (hitSet.contains(idKey)) continue; // already hit this entity during this window
                    var opt = e.getBoundingBox().inflate(0.3).clip(prevPos, currPos);
                    if (opt.isPresent()) {
                        float dmg = (float) player.getPersistentData().getDouble("Bql.QZipDamage");
                        float kb = (float) player.getPersistentData().getDouble("Bql.QZipKnock");
                        if (dmg > 0.0f) {
                            e.hurt(player.damageSources().playerAttack(player), dmg);
                        } else {
                            player.attack(e);
                        }
                        if (kb > 0.0f && e.isAlive()) {
                            Vec3 dir = currPos.subtract(prevPos);
                            if (dir.lengthSqr() > 1.0E-4) {
                                Vec3 k = dir.normalize().scale(kb * 0.6).add(0, 0.12F * kb, 0);
                                e.setDeltaMovement(e.getDeltaMovement().add(k));
                                if (e instanceof ServerPlayer sp) {
                                    sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
                                }
                            }
                        }
                        hitSet.putBoolean(idKey, true);
                    }
                }
                // write back updated hit set
                player.getPersistentData().put("Bql.QZipHits", hitSet);
            } else {
                player.getPersistentData().remove("Bql.QZipTicks");
                player.getPersistentData().remove("Bql.QZipDamage");
                player.getPersistentData().remove("Bql.QZipKnock");
                player.getPersistentData().remove("Bql.QZipHits");
            }
        }

        // Check if player has the movement restriction tag
        if (player.getTags().contains("Bql.RestrictMove")) {
            // Check if player still has any stun effect active
            boolean hasStunEffect = false;
            for (var effect : player.getActiveEffects()) {
                if (effect.getEffect().getClass().getSimpleName().equals("StunMobEffect")) {
                    hasStunEffect = true;
                    break;
                }
            }
            
            // If no stun effect, remove movement restriction and cleanup
            if (!hasStunEffect) {
                player.removeTag("Bql.RestrictMove");
                player.removeTag("Bql.RestrictMove.HasStoredPos");
                player.getPersistentData().remove("Bql.StoredX");
                player.getPersistentData().remove("Bql.StoredZ");
                player.getPersistentData().remove("Bql.StoredYaw");
                player.getPersistentData().remove("Bql.StoredPitch");
                return;
            }
            
            // Store current horizontal position and rotation (only once when tag is first detected)
            if (!player.getTags().contains("Bql.RestrictMove.HasStoredPos")) {
                player.getPersistentData().putDouble("Bql.StoredX", player.getX());
                player.getPersistentData().putDouble("Bql.StoredZ", player.getZ());
                player.getPersistentData().putFloat("Bql.StoredYaw", player.getYRot());
                player.getPersistentData().putFloat("Bql.StoredPitch", player.getXRot());
                player.addTag("Bql.RestrictMove.HasStoredPos");
            }
            
            // Get stored horizontal position and rotation
            double storedX = player.getPersistentData().getDouble("Bql.StoredX");
            double storedZ = player.getPersistentData().getDouble("Bql.StoredZ");
            float storedYaw = player.getPersistentData().getFloat("Bql.StoredYaw");
            float storedPitch = player.getPersistentData().getFloat("Bql.StoredPitch");
            
            // Check if player has moved horizontally from stored position
            double deltaX = Math.abs(player.getX() - storedX);
            double deltaZ = Math.abs(player.getZ() - storedZ);
            double deltaYaw = Math.abs(player.getYRot() - storedYaw);
            double deltaPitch = Math.abs(player.getXRot() - storedPitch);
            
            // Movement threshold (small tolerance for floating point precision)
            double movementThreshold = 0.01;
            double rotationThreshold = 0.5;
            
            // If player has moved horizontally beyond threshold or rotated, reset their position
            if (deltaX > movementThreshold || deltaZ > movementThreshold ||
                deltaYaw > rotationThreshold || deltaPitch > rotationThreshold) {
                
                // Reset player horizontal position and rotation to stored values, keep current Y
                player.teleportTo(storedX, player.getY(), storedZ);
                player.setYRot(storedYaw);
                player.setXRot(storedPitch);
                
                // Set horizontal velocity to zero but preserve downward velocity for falling
                Vec3 currentVelocity = player.getDeltaMovement();
                double yVelocity = Math.min(currentVelocity.y, 0.0); // Only preserve downward movement
                player.setDeltaMovement(0.0, yVelocity, 0.0);
            }
        }
    }
}