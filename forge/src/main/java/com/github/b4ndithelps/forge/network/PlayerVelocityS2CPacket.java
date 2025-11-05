package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: apply a directed velocity impulse locally for immediate feedback.
 */
public class PlayerVelocityS2CPacket {
    private final double dx;
    private final double dy;
    private final double dz;
    private final float currentScale;

    public PlayerVelocityS2CPacket(double dx, double dy, double dz, float currentScale) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.currentScale = currentScale;
    }

    public static void encode(PlayerVelocityS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.dx);
        buf.writeDouble(msg.dy);
        buf.writeDouble(msg.dz);
        buf.writeFloat(msg.currentScale);
    }

    public static PlayerVelocityS2CPacket decode(FriendlyByteBuf buf) {
        double dx = buf.readDouble();
        double dy = buf.readDouble();
        double dz = buf.readDouble();
        float scale = buf.readFloat();
        return new PlayerVelocityS2CPacket(dx, dy, dz, scale);
    }

    public static void handle(PlayerVelocityS2CPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.scale(msg.currentScale).add(msg.dx, msg.dy, msg.dz));
            mc.player.hasImpulse = true;
            mc.player.fallDistance = 0.0F;
        }));
        ctx.setPacketHandled(true);
    }
}


