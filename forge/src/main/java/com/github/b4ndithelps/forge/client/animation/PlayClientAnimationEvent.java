package com.github.b4ndithelps.forge.client.animation;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class PlayClientAnimationEvent extends Event {
	public final Player player;
	public final String animationId;
	public final boolean active;

	public PlayClientAnimationEvent(Player player, String animationId, boolean active) {
		this.player = player;
		this.animationId = animationId;
		this.active = active;
	}
}


