package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ZoomStatePacket;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.StringProperty;

/**
 * Zooms the player's view similar to a spyglass/optifine zoom.
 * Zoom amount is configurable via a multiplier (e.g., 3.0 => 3x zoom).
 */
@SuppressWarnings("removal")
public class ZoomAbility extends Ability {

	public static final PalladiumProperty<Float> ZOOM_MULTIPLIER = new FloatProperty("zoom_multiplier").configurable("Default zoom multiplier if BodySystem value missing (e.g., 3.0 = 3x)");
	public static final PalladiumProperty<String> ZOOM_BODY_PART = new StringProperty("zoom_body_part").configurable("Body part storing zoom float (e.g., 'head')");
	public static final PalladiumProperty<String> ZOOM_KEY = new StringProperty("zoom_key").configurable("Custom float key for zoom (e.g., 'zoom_level')");

	// Unique per-instance state
	public static final PalladiumProperty<Float> LAST_SENT_SCALE = new FloatProperty("last_sent_scale");
	public static final PalladiumProperty<Boolean> LAST_ENABLED = new BooleanProperty("last_enabled");

	public ZoomAbility() {
		super();
		this.withProperty(ZOOM_MULTIPLIER, 3.0F)
				.withProperty(ZOOM_BODY_PART, "head")
				.withProperty(ZOOM_KEY, "zoom_level");
	}

	@Override
	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(LAST_SENT_SCALE, 1.0F);
		manager.register(LAST_ENABLED, false);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		float fovScale = computeFovScaleFromBodySystem(player, entry);
		entry.setUniqueProperty(LAST_SENT_SCALE, fovScale);
		entry.setUniqueProperty(LAST_ENABLED, true);
		BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoomStatePacket(true, fovScale));
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		// Disable zoom on client when ability turns off
		Boolean wasEnabled = entry.getProperty(LAST_ENABLED);
		if (wasEnabled != null && wasEnabled) {
			BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoomStatePacket(false, 1.0F));
		}
		entry.setUniqueProperty(LAST_ENABLED, false);
		entry.setUniqueProperty(LAST_SENT_SCALE, 1.0F);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		if (!enabled) {
			Boolean wasEnabled = entry.getProperty(LAST_ENABLED);
			if (wasEnabled != null && wasEnabled) {
				BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoomStatePacket(false, 1.0F));
			}
			entry.setUniqueProperty(LAST_ENABLED, false);
			entry.setUniqueProperty(LAST_SENT_SCALE, 1.0F);
			return;
		}

		float newScale = computeFovScaleFromBodySystem(player, entry);
		float lastScale = entry.getProperty(LAST_SENT_SCALE);
		if (Math.abs(newScale - lastScale) > 1.0e-4f) {
			entry.setUniqueProperty(LAST_SENT_SCALE, newScale);
			entry.setUniqueProperty(LAST_ENABLED, true);
			BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ZoomStatePacket(true, newScale));
		}
	}

	private float computeFovScaleFromBodySystem(ServerPlayer player, AbilityInstance entry) {
		String part = entry.getProperty(ZOOM_BODY_PART);
		String key = entry.getProperty(ZOOM_KEY);
		float multiplier;
		try {
			float stored = BodyStatusHelper.getCustomFloat(player, part, key);
			multiplier = stored > 0.0F ? stored : entry.getProperty(ZOOM_MULTIPLIER);
		} catch (Exception e) {
			multiplier = entry.getProperty(ZOOM_MULTIPLIER);
		}
		multiplier = Math.max(1.0F, multiplier);
		return 1.0F / multiplier;
	}

	@Override
	public String getDocumentationDescription() {
		return "Applies a client-side FOV reduction while active to simulate zoom. The zoom amount is configurable via 'zoom_multiplier'.";
	}
}