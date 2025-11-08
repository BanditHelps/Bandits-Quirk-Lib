package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.programs.ClientCombinerStateCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sends the last combine attempt result for a combiner to the requesting client.
 */
public class CombinerStateS2CPacket {
    private final BlockPos combinerPos;
    private final boolean success;
    private final String message;

    public CombinerStateS2CPacket(BlockPos combinerPos, boolean success, String message) {
        this.combinerPos = combinerPos;
        this.success = success;
        this.message = message == null ? "" : message;
    }

    public static void encode(CombinerStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.combinerPos);
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.message, 256);
    }

    public static CombinerStateS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean ok = buf.readBoolean();
        String m = buf.readUtf(256);
        return new CombinerStateS2CPacket(pos, ok, m);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            long gt = (Minecraft.getInstance().level == null) ? 0L : Minecraft.getInstance().level.getGameTime();
            ClientCombinerStateCache.update(combinerPos, success, message, gt);
        });
        return true;
    }
}