package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipStruggleStatusS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side struggle system: lets tagged players mash jump to break free.
 * Difficulty scales with the attacker's quirk factor.
 */
public final class BlackwhipStruggle {

	private static final class StruggleState {
		int taps;
		int threshold;
		boolean active;
	}

	private static final Map<UUID, StruggleState> STRUGGLE_BY_TARGET = new ConcurrentHashMap<>();

	private BlackwhipStruggle() {}

	public static void onTagged(ServerPlayer whipper, LivingEntity target) {
		if (!(target instanceof ServerPlayer victim)) return;
		ServerLevel level = (ServerLevel) whipper.level();
		// Compute threshold using maximum quirk factor among all current whippers
		int threshold = computeThresholdForTarget(level, victim);
		StruggleState s = STRUGGLE_BY_TARGET.computeIfAbsent(victim.getUUID(), id -> new StruggleState());
		// Reset taps when (re)tagged; set the highest threshold observed
		s.taps = 0;
		s.threshold = Math.max(s.threshold, threshold);
		s.active = true;
		sendStatus(victim, s);
	}

	public static void onPotentialUntag(ServerLevel level, LivingEntity target) {
		if (!(target instanceof ServerPlayer victim)) return;
		boolean stillTagged = BlackwhipTags.isEntityTagged(level, victim);
		if (!stillTagged) {
			StruggleState removed = STRUGGLE_BY_TARGET.remove(victim.getUUID());
			if (removed != null && removed.active) {
				// notify to hide HUD
				BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> victim),
						new BlackwhipStruggleStatusS2CPacket(false, 0, 0));
			}
		} else {
			// Update threshold if whippers changed (e.g., stronger whipper joined)
			StruggleState s = STRUGGLE_BY_TARGET.computeIfAbsent(victim.getUUID(), id -> new StruggleState());
			int threshold = computeThresholdForTarget(level, victim);
			if (threshold > s.threshold) {
				s.threshold = threshold;
				if (!s.active) s.active = true;
				sendStatus(victim, s);
			}
		}
	}

	public static void onTap(ServerPlayer target) {
		if (!(target.level() instanceof ServerLevel level)) return;
		// If not tagged by anyone, clear and hide HUD
		if (!BlackwhipTags.isEntityTagged(level, target)) {
			StruggleState removed = STRUGGLE_BY_TARGET.remove(target.getUUID());
			if (removed != null && removed.active) {
				BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target),
						new BlackwhipStruggleStatusS2CPacket(false, 0, 0));
			}
			return;
		}
		StruggleState s = STRUGGLE_BY_TARGET.computeIfAbsent(target.getUUID(), id -> {
			StruggleState st = new StruggleState();
			st.taps = 0;
			st.threshold = computeThresholdForTarget(level, target);
			st.active = true;
			return st;
		});
		if (!s.active) s.active = true;
		s.taps = Math.max(0, s.taps) + 1;
		sendStatus(target, s);
		if (s.taps >= Math.max(1, s.threshold)) {
			// Break free: remove tags from all whippers currently tagging this target
			List<ServerPlayer> whippers = BlackwhipTags.getWhippersForTarget(level, target);
			for (ServerPlayer w : whippers) {
				BlackwhipTags.removeTag(w, target);
			}
			// Clear state and hide HUD
			STRUGGLE_BY_TARGET.remove(target.getUUID());
			BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target),
					new BlackwhipStruggleStatusS2CPacket(false, 0, 0));
		}
	}

	private static int computeThresholdForTarget(ServerLevel level, ServerPlayer target) {
		List<ServerPlayer> whippers = BlackwhipTags.getWhippersForTarget(level, target);
		int maxQ = 0;
		for (ServerPlayer w : whippers) {
			int q = (int) Math.floor(QuirkFactorHelper.getQuirkFactor(w));
			if (q > maxQ) maxQ = q;
		}
		// Base 12 taps + 8 per quirk factor point (tweakable)
		return 12 + (maxQ * 8);
	}

	private static void sendStatus(ServerPlayer victim, StruggleState s) {
		BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> victim),
				new BlackwhipStruggleStatusS2CPacket(true, Math.max(0, s.taps), Math.max(1, s.threshold)));
	}
}


