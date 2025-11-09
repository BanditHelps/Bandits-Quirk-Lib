package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.genegraph.ClientGeneGraphState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Clientbound: instructs the client to open the gene graph screen with a given
 * world-specific builder gene order.
 */
public class OpenGeneGraphS2CPacket {
    private final List<ResourceLocation> builderOrder;

    public OpenGeneGraphS2CPacket(List<ResourceLocation> builderOrder) {
        this.builderOrder = builderOrder == null ? List.of() : List.copyOf(builderOrder);
    }

    public static void encode(OpenGeneGraphS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.builderOrder.size());
        for (ResourceLocation rl : msg.builderOrder) {
            buf.writeResourceLocation(rl);
        }
    }

    public static OpenGeneGraphS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ResourceLocation> list = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) list.add(buf.readResourceLocation());
        return new OpenGeneGraphS2CPacket(list);
    }

    public static void handle(OpenGeneGraphS2CPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientGeneGraphState.openWithBuilderOrder(msg.builderOrder);
        }));
        ctx.setPacketHandled(true);
    }
}