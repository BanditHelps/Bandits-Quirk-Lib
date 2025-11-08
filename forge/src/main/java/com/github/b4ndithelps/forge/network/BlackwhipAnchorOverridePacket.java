package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet to toggle forcing the Blackwhip start anchor to the right-hand side
 * for a given player (used to align visuals with the restrain animation).
 */
public class BlackwhipAnchorOverridePacket {

	private final int sourcePlayerId;
	private final boolean active;

	public BlackwhipAnchorOverridePacket(int sourcePlayerId, boolean active) {
		this.sourcePlayerId = sourcePlayerId;
		this.active = active;
	}

	public static void encode(BlackwhipAnchorOverridePacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.sourcePlayerId);
		buf.writeBoolean(msg.active);
	}

	public static BlackwhipAnchorOverridePacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		boolean active = buf.readBoolean();
		return new BlackwhipAnchorOverridePacket(id, active);
	}

	public static void handle(BlackwhipAnchorOverridePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			BlackwhipRenderHandler.applyAnchorOverride(msg.sourcePlayerId, msg.active);
		}));
		ctx.get().setPacketHandled(true);
	}
}


