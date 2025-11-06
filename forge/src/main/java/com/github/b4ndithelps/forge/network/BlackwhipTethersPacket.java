package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Clientbound packet to show/hide multiple persistent Blackwhip tethers from a player to multiple entity targets.
 */
public class BlackwhipTethersPacket {

	private final int sourcePlayerId;
	private final boolean active;
	private final float curve;
	private final float thickness;
	private final List<Integer> targetEntityIds;

	public BlackwhipTethersPacket(int sourcePlayerId, boolean active, float curve, float thickness, List<Integer> targetEntityIds) {
		this.sourcePlayerId = sourcePlayerId;
		this.active = active;
		this.curve = curve;
		this.thickness = thickness;
		this.targetEntityIds = targetEntityIds;
	}

	public static void encode(BlackwhipTethersPacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.sourcePlayerId);
		buf.writeBoolean(msg.active);
		buf.writeFloat(msg.curve);
		buf.writeFloat(msg.thickness);
		buf.writeVarInt(msg.targetEntityIds.size());
		for (int id : msg.targetEntityIds) buf.writeVarInt(id);
	}

	public static BlackwhipTethersPacket decode(FriendlyByteBuf buf) {
		int playerId = buf.readVarInt();
		boolean active = buf.readBoolean();
		float curve = buf.readFloat();
		float thickness = buf.readFloat();
		int n = buf.readVarInt();
		List<Integer> ids = new ArrayList<>(n);
		for (int i = 0; i < n; i++) ids.add(buf.readVarInt());
		return new BlackwhipTethersPacket(playerId, active, curve, thickness, ids);
	}

	public static void handle(BlackwhipTethersPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			BlackwhipRenderHandler.applyTethersPacket(msg.sourcePlayerId, msg.active, msg.curve, msg.thickness, msg.targetEntityIds);
		}));
		ctx.get().setPacketHandled(true);
	}
}







