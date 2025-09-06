package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.abilities.TwinImpactMarkAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class TwinImpactEvents {

    public static class StoredMark {
        public final long createdTick;
        public final int entityId; // -1 when not for entity
        public final BlockPos blockPos; // may be null
        public final Vec3 position; // impact point as fallback

        public StoredMark(long createdTick, int entityId, BlockPos blockPos, Vec3 position) {
            this.createdTick = createdTick;
            this.entityId = entityId;
            this.blockPos = blockPos;
            this.position = position;
        }
    }

    private static final Map<UUID, StoredMark> LAST_MARK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;

        var source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        // Only record if player has TwinImpactMarkAbility enabled
        if (!isMarkingEnabled(player)) return;

        // Record entity mark with rough impact position
        Vec3 hitPos = target.position();
        storeMark(player, new StoredMark(player.level().getGameTime(), target.getId(), null, hitPos));
    }

    // Utility: check if player currently has TwinImpactMarkAbility enabled in any power
    private static boolean isMarkingEnabled(ServerPlayer player) {
        // Power is defined at data/bql/palladium/powers/twin_impact.json → id: bql:twin_impact
        if (AbilityUtil.isEnabled(player, ResourceLocation.parse("bql:twin_impact"), "mark")) return true;
        return false;
    }

    private static void storeMark(ServerPlayer player, StoredMark mark) {
        LAST_MARK.put(player.getUUID(), mark);
    }

    public static StoredMark consumeMark(ServerPlayer player, int maxDistance) {
        StoredMark mark = LAST_MARK.remove(player.getUUID());
        if (mark == null) return null;

        // Range safety check
        if (mark.position != null) {
            if (mark.position.distanceTo(player.position()) > maxDistance) return null;
        } else if (mark.blockPos != null) {
            if (player.blockPosition().distManhattan(mark.blockPos) > maxDistance) return null;
        } else if (mark.entityId >= 0) {
            Entity e = ((ServerLevel)player.level()).getEntity(mark.entityId);
            if (e != null && e.position().distanceTo(player.position()) > maxDistance) return null;
        }
        return mark;
    }

    public static void applySecondImpact(ServerPlayer player, StoredMark mark, float multiplier) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Prefer entity second hit
        if (mark.entityId >= 0) {
            Entity e = serverLevel.getEntity(mark.entityId);
            if (e instanceof LivingEntity living && living.isAlive()) {
                float base = 2.0F;
                living.hurt(level.damageSources().playerAttack(player), base * multiplier);
                return;
            }
        }

        // Otherwise apply an AoE knock/damage at position or block center
        Vec3 pos = mark.position;
        if (pos == null && mark.blockPos != null) {
            pos = Vec3.atCenterOf(mark.blockPos);
        }
        if (pos == null) return;

        double radius = Math.min(8.0, 2.0 + multiplier);
        for (LivingEntity le : serverLevel.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(radius))) {
            if (le == player) continue;
            double dist = Math.max(0.1, le.position().distanceTo(pos));
            double force = (radius / dist) * 0.4 * Math.max(1.0, (double)multiplier);
            Vec3 push = le.position().subtract(pos).normalize().scale(force);
            le.push(push.x, Math.min(0.7, force * 0.5), push.z);
            le.hurt(level.damageSources().playerAttack(player), (float)Math.max(1.0, multiplier));
        }
        serverLevel.levelEvent(2001, new BlockPos((int)pos.x, (int)pos.y, (int)pos.z), 0);
    }
}


