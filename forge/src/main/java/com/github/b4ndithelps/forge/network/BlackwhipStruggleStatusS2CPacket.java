package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipStruggleClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: HUD indicator for struggling; shows progress and requirement.
 */
public class BlackwhipStruggleStatusS2CPacket {

	private final boolean active;
	private final int taps;
	private final int threshold;

	public BlackwhipStruggleStatusS2CPacket(boolean active, int taps, int threshold) {
		this.active = active;
		this.taps = taps;
		this.threshold = threshold;
	}

	public static void encode(BlackwhipStruggleStatusS2CPacket msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.active);
		buf.writeVarInt(msg.taps);
		buf.writeVarInt(msg.threshold);
	}

	public static BlackwhipStruggleStatusS2CPacket decode(FriendlyByteBuf buf) {
		boolean active = buf.readBoolean();
		int taps = buf.readVarInt();
		int threshold = buf.readVarInt();
		return new BlackwhipStruggleStatusS2CPacket(active, taps, threshold);
	}

	public static void handle(BlackwhipStruggleStatusS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			BlackwhipStruggleClient.applyStatus(msg.active, msg.taps, msg.threshold);
		}));
		ctx.setPacketHandled(true);
	}
}