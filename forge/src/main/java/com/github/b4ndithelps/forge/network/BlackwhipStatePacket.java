package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet carrying visual state for Blackwhip rendering.
 */
public class BlackwhipStatePacket {
    private final int sourcePlayerId;
    private final boolean active;
    private final boolean restraining;
    private final int targetEntityId; // -1 for miss
    private final int ticksLeft;
    private final int missRetractTicks;
    private final float range;
    private final float curve;
    private final float thickness;

    public BlackwhipStatePacket(int sourcePlayerId, boolean active, boolean restraining, int targetEntityId,
                                int ticksLeft, int missRetractTicks, float range, float curve, float thickness) {
        this.sourcePlayerId = sourcePlayerId;
        this.active = active;
        this.restraining = restraining;
        this.targetEntityId = targetEntityId;
        this.ticksLeft = ticksLeft;
        this.missRetractTicks = missRetractTicks;
        this.range = range;
        this.curve = curve;
        this.thickness = thickness;
    }

    public static void encode(BlackwhipStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sourcePlayerId);
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.restraining);
        buf.writeVarInt(msg.targetEntityId);
        buf.writeVarInt(msg.ticksLeft);
        buf.writeVarInt(msg.missRetractTicks);
        buf.writeFloat(msg.range);
        buf.writeFloat(msg.curve);
        buf.writeFloat(msg.thickness);
    }

    public static BlackwhipStatePacket decode(FriendlyByteBuf buf) {
        int sourcePlayerId = buf.readVarInt();
        boolean active = buf.readBoolean();
        boolean restraining = buf.readBoolean();
        int targetEntityId = buf.readVarInt();
        int ticksLeft = buf.readVarInt();
        int missRetractTicks = buf.readVarInt();
        float range = buf.readFloat();
        float curve = buf.readFloat();
        float thickness = buf.readFloat();
        return new BlackwhipStatePacket(sourcePlayerId, active, restraining, targetEntityId, ticksLeft, missRetractTicks, range, curve, thickness);
    }

    public static void handle(BlackwhipStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            BlackwhipRenderHandler.applyPacket(msg.sourcePlayerId, msg.active, msg.restraining, msg.targetEntityId,
                    msg.ticksLeft, msg.missRetractTicks, msg.range, msg.curve, msg.thickness);
        }));
        ctx.get().setPacketHandled(true);
    }
}