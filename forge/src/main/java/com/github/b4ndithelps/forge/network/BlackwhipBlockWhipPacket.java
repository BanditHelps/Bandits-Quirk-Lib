package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet to render a single Blackwhip tendril traveling to a static world anchor (block position).
 */
public class BlackwhipBlockWhipPacket {
    private final int sourcePlayerId;
    private final boolean active;
    private final double x;
    private final double y;
    private final double z;
    private final int travelTicks;
    private final float curve;
    private final float thickness;

    public BlackwhipBlockWhipPacket(int sourcePlayerId, boolean active, double x, double y, double z, int travelTicks,
                                    float curve, float thickness) {
        this.sourcePlayerId = sourcePlayerId;
        this.active = active;
        this.x = x; this.y = y; this.z = z;
        this.travelTicks = travelTicks;
        this.curve = curve;
        this.thickness = thickness;
    }

    public static void encode(BlackwhipBlockWhipPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sourcePlayerId);
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeVarInt(msg.travelTicks);
        buf.writeFloat(msg.curve);
        buf.writeFloat(msg.thickness);
    }

    public static BlackwhipBlockWhipPacket decode(FriendlyByteBuf buf) {
        int sourcePlayerId = buf.readVarInt();
        boolean active = buf.readBoolean();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int travelTicks = buf.readVarInt();
        float curve = buf.readFloat();
        float thickness = buf.readFloat();
        return new BlackwhipBlockWhipPacket(sourcePlayerId, active, x, y, z, travelTicks, curve, thickness);
    }

    public static void handle(BlackwhipBlockWhipPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            BlackwhipRenderHandler.applyBlockPacket(msg.sourcePlayerId, msg.active, msg.x, msg.y, msg.z,
                    msg.travelTicks, msg.curve, msg.thickness);
        }));
        ctx.get().setPacketHandled(true);
    }
}