package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.programs.ClientSequencerStatusCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Broadcasts the running/analyzed state of a Gene Sequencer to clients for UI updates.
 */
public class SequencerStateS2CPacket {
    private final BlockPos pos;
    private final boolean running;
    private final boolean analyzed;

    public SequencerStateS2CPacket(BlockPos pos, boolean running, boolean analyzed) {
        this.pos = pos;
        this.running = running;
        this.analyzed = analyzed;
    }

    public static void encode(SequencerStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.running);
        buf.writeBoolean(msg.analyzed);
    }

    public static SequencerStateS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean running = buf.readBoolean();
        boolean analyzed = buf.readBoolean();
        return new SequencerStateS2CPacket(pos, running, analyzed);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            // Update client-side cache for ref programs
            ClientSequencerStatusCache.update(pos, running, analyzed, Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime());
        });
        return true;
    }
}