package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.blackwhip.BlackwhipRenderHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet to toggle and configure the Blackwhip aura visuals.
 */
public class BlackwhipAuraPacket {
    private final int sourcePlayerId;
    private final boolean active;
    private final int tentacleCount;
    private final float length;
    private final float curve;
    private final float thickness;
    private final float jaggedness;
    private final float orbitSpeed;
    private final long seed;

    public BlackwhipAuraPacket(int sourcePlayerId, boolean active, int tentacleCount, float length, float curve, float thickness, float jaggedness, float orbitSpeed, long seed) {
        this.sourcePlayerId = sourcePlayerId;
        this.active = active;
        this.tentacleCount = tentacleCount;
        this.length = length;
        this.curve = curve;
        this.thickness = thickness;
        this.jaggedness = jaggedness;
        this.orbitSpeed = orbitSpeed;
        this.seed = seed;
    }

    public static void encode(BlackwhipAuraPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.sourcePlayerId);
        buf.writeBoolean(msg.active);
        buf.writeVarInt(msg.tentacleCount);
        buf.writeFloat(msg.length);
        buf.writeFloat(msg.curve);
        buf.writeFloat(msg.thickness);
        buf.writeFloat(msg.jaggedness);
        buf.writeFloat(msg.orbitSpeed);
        buf.writeLong(msg.seed);
    }

    public static BlackwhipAuraPacket decode(FriendlyByteBuf buf) {
        int sourcePlayerId = buf.readVarInt();
        boolean active = buf.readBoolean();
        int tentacleCount = buf.readVarInt();
        float length = buf.readFloat();
        float curve = buf.readFloat();
        float thickness = buf.readFloat();
        float jaggedness = buf.readFloat();
        float orbitSpeed = buf.readFloat();
        long seed = buf.readLong();
        return new BlackwhipAuraPacket(sourcePlayerId, active, tentacleCount, length, curve, thickness, jaggedness, orbitSpeed, seed);
    }

    public static void handle(BlackwhipAuraPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            BlackwhipRenderHandler.applyAuraPacket(
                    msg.sourcePlayerId,
                    msg.active,
                    msg.tentacleCount,
                    msg.length,
                    msg.curve,
                    msg.thickness,
                    msg.jaggedness,
                    msg.orbitSpeed,
                    msg.seed
            );
        }));
        ctx.get().setPacketHandled(true);
    }
}