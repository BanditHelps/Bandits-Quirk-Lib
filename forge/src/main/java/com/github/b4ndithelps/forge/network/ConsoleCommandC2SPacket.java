package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConsoleCommandC2SPacket {
    private final BlockPos pos;
    private final String command;

    public ConsoleCommandC2SPacket(BlockPos pos, String command) {
        this.pos = pos;
        this.command = command;
    }

    public static void encode(ConsoleCommandC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.command, 32767);
    }

    public static ConsoleCommandC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String command = buf.readUtf(32767);
        return new ConsoleCommandC2SPacket(pos, command);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(this.pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                terminal.runCommand(this.command);
            }
        });
        return true;
    }
}


