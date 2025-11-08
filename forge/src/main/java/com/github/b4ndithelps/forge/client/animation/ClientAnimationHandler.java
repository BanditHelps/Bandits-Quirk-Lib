package com.github.b4ndithelps.forge.client.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

public final class ClientAnimationHandler {
	private ClientAnimationHandler() {}

	public static void playAnimation(int playerId, String animationId) {
		var mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Player player = (Player) mc.level.getEntity(playerId);
		if (player == null) return;

		MinecraftForge.EVENT_BUS.post(new PlayClientAnimationEvent(player, animationId, true));
	}

	public static void stopAnimation(int playerId, String animationId) {
		var mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Player player = (Player) mc.level.getEntity(playerId);
		if (player == null) return;

		MinecraftForge.EVENT_BUS.post(new PlayClientAnimationEvent(player, animationId, false));
	}
}


