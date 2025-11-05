package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipBubbleShieldPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("removal")
public class BlackwhipBubbleShieldAbility extends Ability {

	public static final PalladiumProperty<Integer> TENTACLE_COUNT = new IntegerProperty("tentacle_count").configurable("How many tendrils form the shield");
	public static final PalladiumProperty<Float> RADIUS = new FloatProperty("radius").configurable("Radius of the shield sphere");
	public static final PalladiumProperty<Float> FORWARD_OFFSET = new FloatProperty("forward_offset").configurable("Distance in front of player to center the sphere");
	public static final PalladiumProperty<Float> CURVE = new FloatProperty("curve").configurable("Curvature of tendrils (visual only)");
	public static final PalladiumProperty<Float> THICKNESS = new FloatProperty("thickness").configurable("Base ribbon thickness");
	public static final PalladiumProperty<Float> JAGGEDNESS = new FloatProperty("jaggedness").configurable("Noise amount along tendrils");
	public static final PalladiumProperty<Float> KNOCKBACK = new FloatProperty("knockback_strength").configurable("Knockback strength applied to the player on block");
	public static final PalladiumProperty<Float> STAMINA_PER_DAMAGE = new FloatProperty("stamina_per_damage").configurable("Stamina cost per 1 damage prevented");

	public BlackwhipBubbleShieldAbility() {
		super();
		this.withProperty(TENTACLE_COUNT, 8)
				.withProperty(RADIUS, 1.4F)
				.withProperty(FORWARD_OFFSET, 1.2F)
				.withProperty(CURVE, 1.1F)
				.withProperty(THICKNESS, 1.0F)
				.withProperty(JAGGEDNESS, 0.35F)
				.withProperty(KNOCKBACK, 0.6F)
				.withProperty(STAMINA_PER_DAMAGE, 10.0F);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		long seed = ThreadLocalRandom.current().nextLong();
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipBubbleShieldPacket(
						player.getId(),
						true,
						Math.max(1, entry.getProperty(TENTACLE_COUNT)),
						Math.max(0.4F, entry.getProperty(RADIUS)),
						Math.max(0.0F, entry.getProperty(FORWARD_OFFSET)),
						Math.max(0.0F, entry.getProperty(CURVE)),
						Math.max(0.05F, entry.getProperty(THICKNESS)),
						Math.max(0.0F, entry.getProperty(JAGGEDNESS)),
						seed
				));

		// Cache relevant scalar settings to persistent data for quick access in damage event
		player.getPersistentData().putDouble("Bql.BubbleShield.Knockback", Math.max(0.0F, entry.getProperty(KNOCKBACK)));
		player.getPersistentData().putDouble("Bql.BubbleShield.StaminaPerDamage", Math.max(0.0F, entry.getProperty(STAMINA_PER_DAMAGE)));
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipBubbleShieldPacket(player.getId(), false, 0, 0, 0, 0, 0, 0, 0L));
	}
}


