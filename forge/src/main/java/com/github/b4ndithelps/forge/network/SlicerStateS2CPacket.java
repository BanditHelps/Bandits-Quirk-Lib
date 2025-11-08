package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.programs.ClientSlicerStateCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Broadcasts the running state and input gene labels of a Gene Slicer to clients for UI updates.
 */
public class SlicerStateS2CPacket {
    private final BlockPos pos;
    private final boolean running;
    private final List<String> labels;

    public SlicerStateS2CPacket(BlockPos pos, boolean running, List<String> labels) {
        this.pos = pos;
        this.running = running;
        this.labels = labels == null ? List.of() : new ArrayList<>(labels);
    }

    public static void encode(SlicerStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.running);
        buf.writeVarInt(msg.labels.size());
        for (String s : msg.labels) buf.writeUtf(s == null ? "" : s, 128);
    }

    public static SlicerStateS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean running = buf.readBoolean();
        int n = buf.readVarInt();
        List<String> labels = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) labels.add(buf.readUtf(128));
        return new SlicerStateS2CPacket(pos, running, labels);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            long gt = (Minecraft.getInstance().level == null) ? 0L : Minecraft.getInstance().level.getGameTime();
            ClientSlicerStateCache.update(pos, labels, running, gt);
        });
        return true;
    }
}