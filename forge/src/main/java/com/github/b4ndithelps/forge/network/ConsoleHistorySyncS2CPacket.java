package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConsoleHistorySyncS2CPacket {
    private final BlockPos pos;
    private final List<String> history;
    private final int cursor;

    public ConsoleHistorySyncS2CPacket(BlockPos pos, List<String> history, int cursor) {
        this.pos = pos;
        this.history = history != null ? history : new ArrayList<>();
        this.cursor = cursor;
    }

    public static void encode(ConsoleHistorySyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.history.size());
        for (String s : msg.history) {
            buf.writeUtf(s, 32767);
        }
        buf.writeVarInt(msg.cursor);
    }

    public static ConsoleHistorySyncS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();
        List<String> history = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            history.add(buf.readUtf(32767));
        }
        int cursor = buf.readVarInt();
        return new ConsoleHistorySyncS2CPacket(pos, history, cursor);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            BlockEntity be = level.getBlockEntity(this.pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                terminal.clientSetHistory(this.history, this.cursor);
            }
        });
        return true;
    }
}


