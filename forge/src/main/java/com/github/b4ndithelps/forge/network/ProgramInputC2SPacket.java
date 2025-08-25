package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import com.github.b4ndithelps.forge.console.ConsoleContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ProgramInputC2SPacket {
    private final BlockPos pos;
    private final String action;

    public ProgramInputC2SPacket(BlockPos pos, String action) {
        this.pos = pos;
        this.action = action;
    }

    public static void encode(ProgramInputC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.action, 256);
    }

    public static ProgramInputC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String action = buf.readUtf(256);
        return new ProgramInputC2SPacket(pos, action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(this.pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                var prog = terminal.getActiveProgram();
                if (prog != null) {
                    prog.onKey(new ConsoleContext(terminal), this.action);
                }
            }
        });
        return true;
    }
}


