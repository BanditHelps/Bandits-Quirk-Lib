package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PrinterStateS2CPacket {
    private final BlockPos printerPos;
    private final boolean success;
    private final String message;

    public PrinterStateS2CPacket(BlockPos printerPos, boolean success, String message) {
        this.printerPos = printerPos;
        this.success = success;
        this.message = message == null ? "" : message;
    }

    public static void encode(PrinterStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.printerPos);
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.message, 256);
    }

    public static PrinterStateS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean ok = buf.readBoolean();
        String m = buf.readUtf(256);
        return new PrinterStateS2CPacket(pos, ok, m);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            long gt = (Minecraft.getInstance().level == null) ? 0L : Minecraft.getInstance().level.getGameTime();
            com.github.b4ndithelps.forge.client.programs.ClientPrinterStateCache.update(printerPos, success, message, gt);
        });
        return true;
    }
}


