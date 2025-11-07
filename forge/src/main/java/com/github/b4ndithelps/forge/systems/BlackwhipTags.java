package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipTethersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared server-side store for Blackwhip tagged targets per player.
 * Other Blackwhip abilities operate on this shared tag list.
 */
public final class BlackwhipTags {

	public static final String PLAYER_DEPLOYED_TAG = "bandits_quirk_lib.blackwhip_active";

	public static final class TagEntry {
		public final long createdTick;
		public final int entityId;
		public final int expireTicks;

		public TagEntry(long createdTick, int entityId, int expireTicks) {
			this.createdTick = createdTick;
			this.entityId = entityId;
			this.expireTicks = expireTicks;
		}
	}

	private static final Map<UUID, Map<Integer, TagEntry>> PLAYER_TAGS = new ConcurrentHashMap<>();

	private BlackwhipTags() {}

    public static void addTag(ServerPlayer player, LivingEntity target, int expireTicks) {
        if (player == null || target == null || player.level().isClientSide) return;
        Map<Integer, TagEntry> map = PLAYER_TAGS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        int id = target.getId();
        int requested = Math.max(1, expireTicks);
        TagEntry existing = map.get(id);
        // Do not shorten an existing tag's allowed lifetime; prefer the larger TTL
        int ttl = existing == null ? requested : Math.max(existing.expireTicks, requested);
        map.put(id, new TagEntry(player.level().getGameTime(), id, ttl));
        updateActiveTag(player);
    }

    public static void addTag(ServerPlayer player, LivingEntity target, int expireTicks, int maxTags) {
        addTag(player, target, expireTicks);
        if (maxTags > 0) {
            Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
            if (map != null && map.size() > maxTags) {
                // remove oldest entries to fit limit
                List<Map.Entry<Integer, TagEntry>> all = new ArrayList<>(map.entrySet());
                all.sort(Comparator.comparingLong(e -> e.getValue().createdTick));
                int toRemove = map.size() - maxTags;
                for (int i = 0; i < toRemove && i < all.size(); i++) map.remove(all.get(i).getKey());
            }
        }
        updateActiveTag(player);
    }

	public static void clearTags(ServerPlayer player) {
		if (player == null) return;
        PLAYER_TAGS.remove(player.getUUID());
        syncToClients(player); // clear visuals
        updateActiveTag(player);
	}

	public static boolean removeTag(ServerPlayer player, int entityId) {
		if (player == null) return false;
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		if (map == null || map.isEmpty()) return false;
		boolean removed = map.remove(entityId) != null;
		if (map.isEmpty()) PLAYER_TAGS.remove(player.getUUID());
		if (removed) {
			syncToClients(player);
			updateActiveTag(player);
		}
		return removed;
	}

	public static boolean removeTag(ServerPlayer player, LivingEntity target) {
		return target != null && removeTag(player, target.getId());
	}

	public static List<LivingEntity> getTaggedEntities(ServerPlayer player, int maxDistance) {
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
		boolean mapModified = cleanupExpired(player);
		ServerLevel level = (ServerLevel) player.level();
		List<LivingEntity> out = new ArrayList<>();
		for (Map.Entry<Integer, TagEntry> e : new ArrayList<>(map.entrySet())) {
			Entity ent = level.getEntity(e.getKey());
			if (ent instanceof LivingEntity living && living.isAlive()) {
				if (maxDistance <= 0 || living.position().distanceTo(player.position()) <= maxDistance) {
					out.add(living);
				}
			} else {
				map.remove(e.getKey());
				mapModified = true;
			}
		}
		if (map.isEmpty()) PLAYER_TAGS.remove(player.getUUID());
		if (mapModified) syncToClients(player);
        updateActiveTag(player);
		return out;
	}

    public static List<LivingEntity> consumeTags(ServerPlayer player, int maxTargets, int maxDistance) {
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		if (map == null || map.isEmpty()) return java.util.Collections.emptyList();
		cleanupExpired(player);
		ServerLevel level = (ServerLevel) player.level();
		List<LivingEntity> out = new ArrayList<>();
		for (Map.Entry<Integer, TagEntry> e : new ArrayList<>(map.entrySet())) {
			if (maxTargets > 0 && out.size() >= maxTargets) break;
			Entity ent = level.getEntity(e.getKey());
			if (ent instanceof LivingEntity living && living.isAlive()) {
				if (maxDistance <= 0 || living.position().distanceTo(player.position()) <= maxDistance) {
					out.add(living);
					map.remove(e.getKey());
				}
			} else {
				map.remove(e.getKey());
			}
		}
        if (map.isEmpty()) PLAYER_TAGS.remove(player.getUUID());
        // sync visuals after consumption
        syncToClients(player);
        updateActiveTag(player);
		return out;
	}

	private static boolean cleanupExpired(ServerPlayer player) {
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		if (map == null) return false;
		boolean changed = false;
		long gt = player.level().getGameTime();
		for (Map.Entry<Integer, TagEntry> e : new ArrayList<>(map.entrySet())) {
			TagEntry te = e.getValue();
			if (gt - te.createdTick > te.expireTicks) {
				map.remove(e.getKey());
				changed = true;
			}
		}
		if (map.isEmpty()) PLAYER_TAGS.remove(player.getUUID());
		return changed;
	}

    /**
     * Server-side periodic maintenance for a player's Blackwhip tags.
     * Cleans up expired entries and pushes a client sync if anything changed.
     */
    public static void tick(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;
        boolean changed = cleanupExpired(player);
        if (changed) {
            syncToClients(player);
        } else {
            updateActiveTag(player);
        }
    }

    public static void syncToClients(ServerPlayer player) {
        // default visual parameters if not provided by ability
        syncToClients(player, 0.6F, 1.0F);
    }

    public static void syncToClients(ServerPlayer player, float curve, float thickness) {
        Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
        List<Integer> ids = new ArrayList<>();
        if (map != null && !map.isEmpty()) ids.addAll(map.keySet());
        boolean active = !ids.isEmpty();
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipTethersPacket(player.getId(), active, curve, thickness, ids));
        updateActiveTag(player);
    }

	public static boolean isWhipDeployed(ServerPlayer player) {
		boolean changed = cleanupExpired(player);
		if (changed) syncToClients(player);
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		return map != null && !map.isEmpty();
	}

	private static void updateActiveTag(ServerPlayer player) {
		if (player == null) return;
		cleanupExpired(player);
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		boolean hasAny = map != null && !map.isEmpty();
		if (hasAny) {
			if (!player.getTags().contains(PLAYER_DEPLOYED_TAG)) player.addTag(PLAYER_DEPLOYED_TAG);
		} else {
			if (player.getTags().contains(PLAYER_DEPLOYED_TAG)) player.removeTag(PLAYER_DEPLOYED_TAG);
		}
	}
}
