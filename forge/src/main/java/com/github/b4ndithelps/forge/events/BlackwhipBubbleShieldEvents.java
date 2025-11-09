package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerVelocityS2CPacket;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.ability.AbilityUtil;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class BlackwhipBubbleShieldEvents {

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		LivingEntity victim = event.getEntity();
		if (!(victim instanceof ServerPlayer player)) return;
		if (victim.level().isClientSide) return;

		// Only when the Blackwhip Bubble Shield ability is currently enabled
		if (!AbilityUtil.isEnabled(player, ResourceLocation.parse("bql:blackwhip"), "blackwhip_bubble_shield")) return;

		DamageSource source = event.getSource();
		// Exclude environmental/self sources: allow only attacks with an attacker OR explosions/projectiles
		boolean hasAttacker = source.getEntity() != null;
		boolean isExplosion = source.is(DamageTypeTags.IS_EXPLOSION);
		boolean isProjectile = source.is(DamageTypeTags.IS_PROJECTILE);
		boolean isEnvironmental = !hasAttacker && !isProjectile && !isExplosion;
		// Explicitly exclude some environment types just in case
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FREEZE)
				|| source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.IN_WALL)
				|| source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE)) {
			isEnvironmental = true;
		}
		if (isEnvironmental) return;

		float incoming = event.getAmount();
		if (incoming <= 0f) return;

		// Convert damage to stamina cost; read cached config from ability first-tick
		double ratio = player.getPersistentData().getDouble("Bql.BubbleShield.StaminaPerDamage");
		if (ratio <= 0) ratio = 10.0; // fallback default
		int staminaCost = (int)Math.ceil(incoming * ratio);
		if (staminaCost > 0) {
			StaminaHelper.useStamina(player, staminaCost);
		}

		// Apply knockback away from source
		Vec3 dir;
		if (hasAttacker) {
			Vec3 from = source.getEntity().position();
			dir = victim.position().subtract(from);
		} else if (source.getSourcePosition() != null) {
			dir = victim.position().subtract(source.getSourcePosition());
		} else {
			dir = victim.getLookAngle().scale(-1);
		}
		double len = Math.sqrt(Math.max(1.0e-6, dir.lengthSqr()));
		dir = dir.scale(1.0 / len);
		double kb = player.getPersistentData().getDouble("Bql.BubbleShield.Knockback");
		if (kb <= 0) kb = 0.6;
		// Scale knockback by incoming damage amount
		double kbScaled = kb * incoming;
		Vec3 push = dir.scale(kbScaled);
		victim.push(push.x, Math.min(0.6, kbScaled * 0.5), push.z);
		victim.hurtMarked = true;
		victim.fallDistance = 0.0F;
		BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVelocityS2CPacket(push.x, Math.min(0.6, kbScaled * 0.5), push.z, 0.6f));

		// Visual feedback: tiny spark burst at sphere center
		if (victim.level() instanceof ServerLevel sl) {
			Vec3 center = victim.getEyePosition().add(victim.getLookAngle().normalize().scale(1.0));
			sl.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 6, 0.1, 0.1, 0.1, 0.02);
		}

		// Negate the damage
		event.setCanceled(true);
	}
}