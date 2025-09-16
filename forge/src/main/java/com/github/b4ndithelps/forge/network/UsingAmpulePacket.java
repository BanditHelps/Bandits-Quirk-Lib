package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UsingAmpulePacket {
    private final boolean using;

    public UsingAmpulePacket(boolean using) {
        this.using = using;
    }

    public static void encode(UsingAmpulePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.using);
    }

    public static UsingAmpulePacket decode(FriendlyByteBuf buf) {
        return new UsingAmpulePacket(buf.readBoolean());
    }

    public static void handle(UsingAmpulePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Player player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.getPersistentData().putBoolean("usingampule", msg.using);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
