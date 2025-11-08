package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.systems.BlackwhipStruggle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: a single "tap" (jump press) while struggling.
 */
public class BlackwhipStruggleTapC2SPacket {

	public BlackwhipStruggleTapC2SPacket() {}

	public static void encode(BlackwhipStruggleTapC2SPacket msg, FriendlyByteBuf buf) { /* no payload */ }

	public static BlackwhipStruggleTapC2SPacket decode(FriendlyByteBuf buf) { return new BlackwhipStruggleTapC2SPacket(); }

	public static void handle(BlackwhipStruggleTapC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer sender = ctx.getSender();
			if (sender == null) return;
			BlackwhipStruggle.onTap(sender);
		});
		ctx.setPacketHandled(true);
	}
}