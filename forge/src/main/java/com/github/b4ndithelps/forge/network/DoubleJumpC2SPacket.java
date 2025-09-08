package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.systems.DoubleJumpSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: request to perform a double jump now.
 * Server validates gene, state, and cooldown, then applies a vertical boost.
 */
public class DoubleJumpC2SPacket {

    public DoubleJumpC2SPacket() {}

    public static void encode(DoubleJumpC2SPacket msg, FriendlyByteBuf buf) { /* no payload */ }

    public static DoubleJumpC2SPacket decode(FriendlyByteBuf buf) { return new DoubleJumpC2SPacket(); }

    public static void handle(DoubleJumpC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            DoubleJumpSystem.tryDoubleJump(player);
        });
        ctx.setPacketHandled(true);
    }
}


