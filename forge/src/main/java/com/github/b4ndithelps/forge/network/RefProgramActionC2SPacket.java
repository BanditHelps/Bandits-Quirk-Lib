package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity;
import com.github.b4ndithelps.forge.blocks.GeneSequencerBlockEntity;
import com.github.b4ndithelps.forge.blocks.util.CableNetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Simple C2S message for the ref-screen programs to request actions, e.g. start a sequencer.
 */
public class RefProgramActionC2SPacket {
    private final BlockPos terminalPos;
    private final String action;
    private final BlockPos targetPos; // optional, may be null for some actions

    public RefProgramActionC2SPacket(BlockPos terminalPos, String action, BlockPos targetPos) {
        this.terminalPos = terminalPos;
        this.action = action == null ? "" : action;
        this.targetPos = targetPos;
    }

    public static void encode(RefProgramActionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeUtf(msg.action, 128);
        buf.writeBoolean(msg.targetPos != null);
        if (msg.targetPos != null) buf.writeBlockPos(msg.targetPos);
    }

    public static RefProgramActionC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos term = buf.readBlockPos();
        String act = buf.readUtf(128);
        BlockPos target = null;
        if (buf.readBoolean()) target = buf.readBlockPos();
        return new RefProgramActionC2SPacket(term, act, target);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || player.level() == null) return;
            BlockEntity be = player.level().getBlockEntity(this.terminalPos);
            if (!(be instanceof BioTerminalRefBlockEntity)) return;

            if ("analyze.start".equals(this.action) && this.targetPos != null) {
                BlockEntity target = player.level().getBlockEntity(this.targetPos);
                if (target instanceof GeneSequencerBlockEntity seq) {
                    // Validate the sequencer is connected via cables to the terminal
                    var connected = CableNetworkUtil.findConnected(player.level(), this.terminalPos, t -> t instanceof GeneSequencerBlockEntity);
                    boolean ok = connected.stream().anyMatch(t -> t.getBlockPos().equals(this.targetPos));
                    if (ok) {
                        seq.startProcessing();
                        // Notify clients immediately that it is running
                        com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> ((net.minecraft.server.level.ServerLevel)player.level()).getChunkAt(this.targetPos)),
                            new com.github.b4ndithelps.forge.network.SequencerStateS2CPacket(this.targetPos, true, false)
                        );
                    }
                }
            }
        });
        return true;
    }
}


