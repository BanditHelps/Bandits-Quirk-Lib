package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import com.github.b4ndithelps.forge.network.BlackwhipAnchorOverridePacket;
import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipRestrainTaggedAbility extends Ability {

	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Maximum distance to consider tags valid (0 = any)");

	public BlackwhipRestrainTaggedAbility() {
		super();
		this.withProperty(MAX_DISTANCE, 48);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		// Animation packet
		BQLNetwork.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new PlayerAnimationPacket(player.getId(), "restrain_animation")
		);
		// Force right-hand/high anchor for everyone viewing this player while restraining
		BQLNetwork.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipAnchorOverridePacket(player.getId(), true)
		);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		int duration = 10;
		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));

		List<LivingEntity> targets = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (targets.isEmpty()) return;
        for (LivingEntity t : targets) {
			// Apply stun/immobilize
			t.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), duration, 0, false, false));
			// Small visual to acknowledge action
			if (t.level() instanceof ServerLevel sl) {
				AABB bb = t.getBoundingBox();
				sl.sendParticles(ParticleTypes.END_ROD,
						(bb.minX + bb.maxX) * 0.5, t.getY(0.5), (bb.minZ + bb.maxZ) * 0.5,
						6, 0.15, 0.15, 0.15, 0.01);
			}
		}
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;

		BQLNetwork.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new PlayerAnimationPacket(player.getId(), "")
		);
		// Remove forced right-hand/high anchor
		BQLNetwork.CHANNEL.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipAnchorOverridePacket(player.getId(), false)
		);
	}
}