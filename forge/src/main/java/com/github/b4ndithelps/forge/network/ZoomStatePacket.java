package com.github.b4ndithelps.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet to toggle and set zoom FOV scaling on the client.
 */
public class ZoomStatePacket {

	public static volatile boolean ENABLED = false;
	public static volatile float FOV_SCALE = 1.0F;

	private final boolean enabled;
	private final float fovScale;

	public ZoomStatePacket(boolean enabled, float fovScale) {
		this.enabled = enabled;
		this.fovScale = fovScale;
	}

	public static void encode(ZoomStatePacket msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.enabled);
		buf.writeFloat(msg.fovScale);
	}

	public static ZoomStatePacket decode(FriendlyByteBuf buf) {
		boolean enabled = buf.readBoolean();
		float fovScale = buf.readFloat();
		return new ZoomStatePacket(enabled, fovScale);
	}

	public static void handle(ZoomStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			ENABLED = msg.enabled;
			FOV_SCALE = msg.fovScale;
		}));
		ctx.get().setPacketHandled(true);
	}
}


