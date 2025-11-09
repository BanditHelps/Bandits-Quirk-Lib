package com.github.b4ndithelps.forge.client.animation;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GeckoLibAnimationSubscriber {
	private static final String CONTROLLER = "bql_controller";

	@SubscribeEvent
	public static void onPlayClientAnimation(PlayClientAnimationEvent event) {
		String animName = extractName(event.animationId);
		if (!event.active) {
			// No explicit stop path here; most Gecko controllers swap on next trigger.
			return;
		}
		tryTriggerPlayerAnimation(event, CONTROLLER, animName);
	}

	private static void tryTriggerPlayerAnimation(PlayClientAnimationEvent event, String controller, String animation) {
		// Try Geckolib 4+ location first
		if (invokeTrigger(event, "software.bernie.geckolib.animation.AnimationController", controller, animation)) return;
		// Fallback: Geckolib 3 package (method may not exist, but try)
		invokeTrigger(event, "software.bernie.geckolib3.core.controller.AnimationController", controller, animation);
	}

	private static boolean invokeTrigger(PlayClientAnimationEvent event, String className, String controller, String animation) {
		try {
			Class<?> cls = Class.forName(className);
			Method m = cls.getMethod("triggerPlayerAnimation", Player.class, String.class, String.class);
			m.invoke(null, event.player, controller, animation);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static String extractName(String id) {
		int i = id.indexOf(':');
		return i >= 0 ? id.substring(i + 1) : id;
	}
}