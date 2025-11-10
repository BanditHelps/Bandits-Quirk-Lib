package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipTethersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
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
		public final int maxDistance; // 0 = unlimited

		public TagEntry(long createdTick, int entityId, int expireTicks, int maxDistance) {
			this.createdTick = createdTick;
			this.entityId = entityId;
			this.expireTicks = expireTicks;
			this.maxDistance = Math.max(0, maxDistance);
		}
	}

	private static final Map<UUID, Map<Integer, TagEntry>> PLAYER_TAGS = new ConcurrentHashMap<>();

	private BlackwhipTags() {}

    public static void addTag(ServerPlayer player, LivingEntity target, int expireTicks) {
        boolean isNew = putOrExtendTag(player, target, expireTicks, 0);
        updateActiveTag(player);
		// Only notify struggle system on first tag creation (not on TTL refresh)
		if (isNew) {
			BlackwhipStruggle.onTagged(player, target);
		}
    }

    public static void addTagWithMaxDistance(ServerPlayer player, LivingEntity target, int expireTicks, int maxDistance) {
        boolean isNew = putOrExtendTag(player, target, expireTicks, maxDistance);
        updateActiveTag(player);
		// Only notify struggle system on first tag creation (not on TTL/params refresh)
		if (isNew) {
			BlackwhipStruggle.onTagged(player, target);
		}
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
		// Clearing tags from this player may untag targets; affected players should hide struggle HUD
		if (player.level() instanceof ServerLevel level) {
			for (ServerPlayer sp : player.server.getPlayerList().getPlayers()) {
				BlackwhipStruggle.onPotentialUntag(level, sp);
			}
		}
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
			if (player.level() instanceof ServerLevel level) {
				Entity ent = level.getEntity(entityId);
				if (ent instanceof LivingEntity living) {
					BlackwhipStruggle.onPotentialUntag(level, living);
				}
			}
		}
		return removed;
	}

	public static boolean removeTag(ServerPlayer player, LivingEntity target) {
		if (target == null) return false;
		boolean res = removeTag(player, target.getId());
		if (player.level() instanceof ServerLevel level) {
			BlackwhipStruggle.onPotentialUntag(level, target);
		}
		return res;
	}

	public static List<LivingEntity> getTaggedEntities(ServerPlayer player, int maxDistance) {
		Map<Integer, TagEntry> map = PLAYER_TAGS.get(player.getUUID());
		if (map == null || map.isEmpty()) return Collections.emptyList();
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
		if (map == null || map.isEmpty()) return Collections.emptyList();
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
		ServerLevel level = (ServerLevel) player.level();
		for (Map.Entry<Integer, TagEntry> e : new ArrayList<>(map.entrySet())) {
			TagEntry te = e.getValue();
			if (gt - te.createdTick > te.expireTicks) {
				map.remove(e.getKey());
				changed = true;
				continue;
			}
			// Enforce max-distance constraint if set (> 0)
			if (te.maxDistance > 0) {
				Entity ent = level.getEntity(e.getKey());
				if (!(ent instanceof LivingEntity living) || !living.isAlive()) {
					map.remove(e.getKey());
					changed = true;
					continue;
				}
				double dist = living.position().distanceTo(player.position());
				if (dist > te.maxDistance) {
					map.remove(e.getKey());
					changed = true;
				}
			}
		}
		if (map.isEmpty()) PLAYER_TAGS.remove(player.getUUID());
		return changed;
	}

    private static boolean putOrExtendTag(ServerPlayer player, LivingEntity target, int expireTicks, int maxDistance) {
        if (player == null || target == null || player.level().isClientSide) return false;
        Map<Integer, TagEntry> map = PLAYER_TAGS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        int id = target.getId();
        int requested = Math.max(1, expireTicks);
        TagEntry existing = map.get(id);
        boolean isNew = (existing == null);
        // Do not shorten an existing tag's allowed lifetime; prefer the larger TTL
        int ttl = existing == null ? requested : Math.max(existing.expireTicks, requested);
        int storedMax = existing == null ? 0 : existing.maxDistance;
        int finalMax = maxDistance > 0 ? maxDistance : storedMax;
        map.put(id, new TagEntry(player.level().getGameTime(), id, ttl, finalMax));
        return isNew;
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

	/**
	 * Returns true if the given living entity is currently tagged by any player.
	 */
	public static boolean isEntityTagged(ServerLevel level, LivingEntity target) {
		if (target == null) return false;
		for (Map<Integer, TagEntry> map : PLAYER_TAGS.values()) {
			if (map != null && map.containsKey(target.getId())) return true;
		}
		return false;
	}

	/**
	 * Gets all players who currently have the given target tagged.
	 */
	public static List<ServerPlayer> getWhippersForTarget(ServerLevel level, LivingEntity target) {
		List<ServerPlayer> out = new ArrayList<>();
		if (target == null) return out;
		for (Map.Entry<UUID, Map<Integer, TagEntry>> e : new ArrayList<>(PLAYER_TAGS.entrySet())) {
			Map<Integer, TagEntry> map = e.getValue();
			if (map == null || !map.containsKey(target.getId())) continue;
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
			if (p != null) out.add(p);
		}
		return out;
	}
}