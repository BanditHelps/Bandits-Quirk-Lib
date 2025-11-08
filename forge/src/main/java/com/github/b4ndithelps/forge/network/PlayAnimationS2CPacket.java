package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.animation.ClientAnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: request playing or stopping a client-side animation by id.
 * This is engine-agnostic; client code can listen and bridge to Geckolib or other systems.
 */
public class PlayAnimationS2CPacket {
	private final int sourcePlayerId;
	private final String animationId;
	private final boolean active;

	public PlayAnimationS2CPacket(int sourcePlayerId, String animationId, boolean active) {
		this.sourcePlayerId = sourcePlayerId;
		this.animationId = animationId;
		this.active = active;
	}

	public static void encode(PlayAnimationS2CPacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.sourcePlayerId);
		buf.writeUtf(msg.animationId);
		buf.writeBoolean(msg.active);
	}

	public static PlayAnimationS2CPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String anim = buf.readUtf();
		boolean active = buf.readBoolean();
		return new PlayAnimationS2CPacket(id, anim, active);
	}

	public static void handle(PlayAnimationS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (msg.active) {
				ClientAnimationHandler.playAnimation(msg.sourcePlayerId, msg.animationId);
			} else {
				ClientAnimationHandler.stopAnimation(msg.sourcePlayerId, msg.animationId);
			}
		}));
		ctx.get().setPacketHandled(true);
	}
}


