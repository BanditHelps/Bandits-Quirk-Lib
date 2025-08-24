package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ProgramScreenSyncS2CPacket {
    private final BlockPos pos;
    private final String text;

    public ProgramScreenSyncS2CPacket(BlockPos pos, String text) {
        this.pos = pos;
        this.text = text;
    }

    public static void encode(ProgramScreenSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.text == null ? "" : msg.text, 32767);
    }

    public static ProgramScreenSyncS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String text = buf.readUtf(32767);
        return new ProgramScreenSyncS2CPacket(pos, text);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;
            BlockEntity be = mc.level.getBlockEntity(this.pos);
            if (be instanceof BioTerminalBlockEntity terminal) {
                terminal.clientSetProgramScreenText(this.text);
            }
        });
        return true;
    }
}


