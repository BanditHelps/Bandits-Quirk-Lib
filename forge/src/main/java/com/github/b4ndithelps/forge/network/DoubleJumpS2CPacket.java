package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: apply a vertical boost locally to ensure immediate visual feedback.
 */
public class DoubleJumpS2CPacket {
    private final float verticalBoost;

    public DoubleJumpS2CPacket(float verticalBoost) {
        this.verticalBoost = verticalBoost;
    }

    public static void encode(DoubleJumpS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.verticalBoost);
    }

    public static DoubleJumpS2CPacket decode(FriendlyByteBuf buf) {
        return new DoubleJumpS2CPacket(buf.readFloat());
    }

    public static void handle(DoubleJumpS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var motion = mc.player.getDeltaMovement();
            double newY = Math.max(-0.2D, motion.y) + (double) msg.verticalBoost;
            mc.player.setDeltaMovement(motion.x, newY, motion.z);
            mc.player.hasImpulse = true;
            mc.player.fallDistance = 0.0F;
        }));
        ctx.setPacketHandled(true);
    }
}


