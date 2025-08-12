package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Client-side state: which entity ids should not render a shadow.
 */
public class NoShadowTagPacket {
    public static final Map<Integer, Boolean> NO_SHADOW_IDS = new ConcurrentHashMap<>();

    private final int entityId;
    private final boolean noShadow;

    public NoShadowTagPacket(int entityId, boolean noShadow) {
        this.entityId = entityId;
        this.noShadow = noShadow;
    }

    public static void encode(NoShadowTagPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.noShadow);
    }

    public static NoShadowTagPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        boolean noShadow = buf.readBoolean();
        return new NoShadowTagPacket(id, noShadow);
    }

    public static void handle(NoShadowTagPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            NO_SHADOW_IDS.put(msg.entityId, msg.noShadow);
        }));
        ctx.get().setPacketHandled(true);
    }

    // Helper for client-only mixin
    public static boolean isNoShadowEntity(Entity entity) {
        Boolean v = NO_SHADOW_IDS.get(entity.getId());
        return v != null && v;
    }
}



