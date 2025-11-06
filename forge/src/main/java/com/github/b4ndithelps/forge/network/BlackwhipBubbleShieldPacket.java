package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlackwhipBubbleShieldPacket {
	public final int sourcePlayerId;
	public final boolean active;
	public final int tentacleCount;
	public final float radius;
	public final float forwardOffset;
	public final float curve;
	public final float thickness;
	public final float jaggedness;
	public final long seed;

	public BlackwhipBubbleShieldPacket(int sourcePlayerId, boolean active, int tentacleCount, float radius, float forwardOffset, float curve, float thickness, float jaggedness, long seed) {
		this.sourcePlayerId = sourcePlayerId;
		this.active = active;
		this.tentacleCount = tentacleCount;
		this.radius = radius;
		this.forwardOffset = forwardOffset;
		this.curve = curve;
		this.thickness = thickness;
		this.jaggedness = jaggedness;
		this.seed = seed;
	}

	public static void encode(BlackwhipBubbleShieldPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.sourcePlayerId);
		buf.writeBoolean(msg.active);
		buf.writeInt(msg.tentacleCount);
		buf.writeFloat(msg.radius);
		buf.writeFloat(msg.forwardOffset);
		buf.writeFloat(msg.curve);
		buf.writeFloat(msg.thickness);
		buf.writeFloat(msg.jaggedness);
		buf.writeLong(msg.seed);
	}

	public static BlackwhipBubbleShieldPacket decode(FriendlyByteBuf buf) {
		int id = buf.readInt();
		boolean active = buf.readBoolean();
		int count = buf.readInt();
		float radius = buf.readFloat();
		float offset = buf.readFloat();
		float curve = buf.readFloat();
		float thick = buf.readFloat();
		float jag = buf.readFloat();
		long seed = buf.readLong();
		return new BlackwhipBubbleShieldPacket(id, active, count, radius, offset, curve, thick, jag, seed);
	}

	public static void handle(BlackwhipBubbleShieldPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var mc = Minecraft.getInstance();
			com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler.applyBubbleShieldPacket(
					msg.sourcePlayerId,
					msg.active,
					msg.tentacleCount,
					msg.radius,
					msg.forwardOffset,
					msg.curve,
					msg.thickness,
					msg.jaggedness,
					msg.seed);
		}));
		ctx.setPacketHandled(true);
	}
}



