package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.StringProperty;

import java.util.List;
import java.util.UUID;

/**
 * Steals an item from a targeted entity and pulls it to the user. Two modes:
 * - hand: steals from main hand; if empty, steals from offhand
 * - helmet: steals the helmet (head slot)
 * Only steals stacks with count <= base_limit + quirk_factor.
 */
@SuppressWarnings("removal")
public class LarcenyAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum distance to target entities");
    public static final PalladiumProperty<String> MODE = new StringProperty("mode").configurable("Mode: 'hand' or 'helmet'");
    public static final PalladiumProperty<Integer> BASE_LIMIT = new IntegerProperty("base_limit").configurable("Base max stack count allowed to steal (before quirk factor)");
    public static final PalladiumProperty<Float> PULL_STRENGTH = new FloatProperty("pull_strength").configurable("Velocity added toward player each tick for the stolen item");

    // Per-instance state
    public static final PalladiumProperty<Boolean> IN_PROGRESS = new net.threetag.palladium.util.property.BooleanProperty("in_progress");
    public static final PalladiumProperty<String> IN_FLIGHT_UUID = new StringProperty("in_flight_uuid");
    public static final PalladiumProperty<Integer> TICKS_LEFT = new IntegerProperty("ticks_left");

    public LarcenyAbility() {
        super();
        this.withProperty(RANGE, 8.0F)
                .withProperty(MODE, "hand")
                .withProperty(BASE_LIMIT, 16)
                .withProperty(PULL_STRENGTH, 0.35F);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(IN_PROGRESS, false);
        manager.register(IN_FLIGHT_UUID, "");
        manager.register(TICKS_LEFT, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel)) return;

        // If we're already pulling an item, ignore new activations
        if (Boolean.TRUE.equals(entry.getProperty(IN_PROGRESS))) return;

        LivingEntity target = findTargetEntity(player, entry.getProperty(RANGE));
        if (target == null || target == player) return;

        String mode = entry.getProperty(MODE);
        if (mode == null) mode = "hand";

        ItemStack toSteal = ItemStack.EMPTY;
        SlotRef slotRef = null;

        if ("helmet".equalsIgnoreCase(mode)) {
            ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                toSteal = helmet.copy();
                slotRef = SlotRef.helmet();
            }
        } else if ("elytra".equalsIgnoreCase(mode)) {
            ItemStack chest = target.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty() && net.minecraft.world.item.Items.ELYTRA.equals(chest.getItem())) {
                toSteal = chest.copy();
                slotRef = SlotRef.chest();
            }
        } else {
            // hand mode: prefer main hand, otherwise offhand
            ItemStack main = target.getMainHandItem();
            if (!main.isEmpty()) {
                toSteal = main.copy();
                slotRef = SlotRef.mainHand();
            } else {
                ItemStack off = target.getOffhandItem();
                if (!off.isEmpty()) {
                    toSteal = off.copy();
                    slotRef = SlotRef.offHand();
                }
            }
        }

        if (toSteal.isEmpty() || slotRef == null) return;

        int base = Math.max(0, entry.getProperty(BASE_LIMIT));
        int limit = base + (int) Math.max(0, QuirkFactorHelper.getQuirkFactor(player));
        if (toSteal.getCount() > limit) return;

        // Remove from target
        clearSlot(target, slotRef);

        // Spawn item and start pulling
        ServerLevel level = (ServerLevel) player.level();
        Vec3 spawnPos = target.getEyePosition();
        ItemEntity itemEntity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, toSteal);
        level.addFreshEntity(itemEntity);

        entry.setUniqueProperty(IN_PROGRESS, true);
        entry.setUniqueProperty(IN_FLIGHT_UUID, itemEntity.getUUID().toString());
        entry.setUniqueProperty(TICKS_LEFT, 20 * 5); // 5 seconds timeout
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        if (!Boolean.TRUE.equals(entry.getProperty(IN_PROGRESS))) return;

        String uuidStr = entry.getProperty(IN_FLIGHT_UUID);
        if (uuidStr == null || uuidStr.isEmpty()) {
            stop(entry);
            return;
        }

        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { stop(entry); return; }

        ItemEntity item = findItemEntity(level, uuid);
        if (item == null || !item.isAlive()) {
            stop(entry);
            return;
        }

        // Pull toward player's torso
        Vec3 targetPos = player.position().add(0, 1.0, 0);
        Vec3 toPlayer = targetPos.subtract(item.position());
        double dist = toPlayer.length();
        float strength = Math.max(0.05F, entry.getProperty(PULL_STRENGTH));
        if (dist > 1.0e-4) {
            Vec3 desired = toPlayer.normalize().scale(strength);
            item.setDeltaMovement(item.getDeltaMovement().scale(0.82).add(desired));
            item.hasImpulse = true;
        }

        int ticksLeft = Math.max(0, entry.getProperty(TICKS_LEFT) - 1);
        entry.setUniqueProperty(TICKS_LEFT, ticksLeft);

        // Close enough: try to insert into player's inventory
        if (dist < 1.0 || ticksLeft == 0) {
            ItemStack stack = item.getItem().copy();
            boolean added = player.addItem(stack);
            if (added) {
                item.discard();
            } else {
                // If inventory full, drop at player's feet and let gravity resume
                item.setNoGravity(false);
                item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            }
            stop(entry);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // Clear state when disabled
        entry.setUniqueProperty(IN_PROGRESS, false);
        entry.setUniqueProperty(IN_FLIGHT_UUID, "");
        entry.setUniqueProperty(TICKS_LEFT, 0);
    }

    private void stop(AbilityInstance entry) {
        entry.setUniqueProperty(IN_PROGRESS, false);
        entry.setUniqueProperty(IN_FLIGHT_UUID, "");
        entry.setUniqueProperty(TICKS_LEFT, 0);
    }

    private LivingEntity findTargetEntity(ServerPlayer player, float range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        EntityHitResult entityHit = null;
        double closestDistance = range;

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eyePos, endPos).inflate(1.0), e -> e != player && e.isAlive());

        for (LivingEntity e : entities) {
            var bb = e.getBoundingBox().inflate(0.3);
            Vec3 hit = bb.clip(eyePos, endPos).orElse(null);
            if (hit != null) {
                double d = eyePos.distanceTo(hit);
                if (d < closestDistance) {
                    closestDistance = d;
                    entityHit = new EntityHitResult(e, hit);
                }
            }
        }

        if (entityHit != null) return (LivingEntity) entityHit.getEntity();

        // Fallback: nearest living within range
        LivingEntity closest = null;
        double closestDist = range;
        for (LivingEntity e : entities) {
            double d = player.distanceTo(e);
            if (d < closestDist) { closest = e; closestDist = d; }
        }
        return closest;
    }

    private ItemEntity findItemEntity(ServerLevel level, UUID uuid) {
        var e = level.getEntity(uuid);
        return (e instanceof ItemEntity ie) ? ie : null;
    }

    private void clearSlot(LivingEntity target, SlotRef ref) {
        if (ref.hand != null) {
            target.setItemInHand(ref.hand, ItemStack.EMPTY);
            if (target instanceof Player p) p.getInventory().setChanged();
        } else if (ref.slot != null) {
            target.setItemSlot(ref.slot, ItemStack.EMPTY);
            if (target instanceof Player p) p.getInventory().setChanged();
        }
    }

    private static class SlotRef {
        final InteractionHand hand;
        final EquipmentSlot slot;
        private SlotRef(InteractionHand hand, EquipmentSlot slot) { this.hand = hand; this.slot = slot; }
        static SlotRef mainHand() { return new SlotRef(InteractionHand.MAIN_HAND, null); }
        static SlotRef offHand() { return new SlotRef(InteractionHand.OFF_HAND, null); }
        static SlotRef helmet() { return new SlotRef(null, EquipmentSlot.HEAD); }
        static SlotRef chest() { return new SlotRef(null, EquipmentSlot.CHEST); }
    }

    @Override
    public String getDocumentationDescription() {
        return "Steals a held item or helmet from a targeted entity if the stack count is within the allowed limit, then pulls it to the user and inserts it into their inventory.";
    }
}


