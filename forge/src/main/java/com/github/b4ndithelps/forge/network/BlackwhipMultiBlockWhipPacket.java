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
 * Clientbound packet to render multiple Blackwhip tendrils traveling to static world anchors.
 */
public class BlackwhipMultiBlockWhipPacket {
    private final int sourcePlayerId;
    private final boolean active;
    private final List<Double> xs;
    private final List<Double> ys;
    private final List<Double> zs;
    private final int travelTicks;
    private final float curve;
    private final float thickness;

    public BlackwhipMultiBlockWhipPacket(int sourcePlayerId, boolean active, List<Double> xs, List<Double> ys, List<Double> zs,
                                         int travelTicks, float curve, float thickness) {
        this.sourcePlayerId = sourcePlayerId;
        this.active = active;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.travelTicks = travelTicks;
        this.curve = curve;
        this.thickness = thickness;
    }

    public static void encode(BlackwhipMultiBlockWhipPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sourcePlayerId);
        buf.writeBoolean(msg.active);
        int n = Math.min(Math.min(msg.xs.size(), msg.ys.size()), msg.zs.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeDouble(msg.xs.get(i));
            buf.writeDouble(msg.ys.get(i));
            buf.writeDouble(msg.zs.get(i));
        }
        buf.writeVarInt(msg.travelTicks);
        buf.writeFloat(msg.curve);
        buf.writeFloat(msg.thickness);
    }

    public static BlackwhipMultiBlockWhipPacket decode(FriendlyByteBuf buf) {
        int sourcePlayerId = buf.readVarInt();
        boolean active = buf.readBoolean();
        int n = buf.readVarInt();
        List<Double> xs = new ArrayList<>(n);
        List<Double> ys = new ArrayList<>(n);
        List<Double> zs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            xs.add(buf.readDouble());
            ys.add(buf.readDouble());
            zs.add(buf.readDouble());
        }
        int travel = buf.readVarInt();
        float curve = buf.readFloat();
        float thickness = buf.readFloat();
        return new BlackwhipMultiBlockWhipPacket(sourcePlayerId, active, xs, ys, zs, travel, curve, thickness);
    }

    public static void handle(BlackwhipMultiBlockWhipPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            BlackwhipRenderHandler.applyMultiBlockPacket(msg.sourcePlayerId, msg.active, msg.xs, msg.ys, msg.zs,
                    msg.travelTicks, msg.curve, msg.thickness);
        }));
        ctx.get().setPacketHandled(true);
    }
}


