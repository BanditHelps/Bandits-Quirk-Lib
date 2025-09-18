package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwitchProgramC2SPacket {
    private final BlockPos pos;
    private final String programId;

    public SwitchProgramC2SPacket(BlockPos pos, String programId) {
        this.pos = pos;
        this.programId = programId == null ? "" : programId;
    }

    public static void encode(SwitchProgramC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.programId, 64);
    }

    public static SwitchProgramC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String id = buf.readUtf(64);
        return new SwitchProgramC2SPacket(pos, id);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(this.pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                terminal.openProgramById(this.programId);
            }
        });
        return true;
    }
}


