package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConsoleSyncS2CPacket {
    private final BlockPos pos;
    private final String text;

    public ConsoleSyncS2CPacket(BlockPos pos, String text) {
        this.pos = pos;
        this.text = text;
    }

    public static void encode(ConsoleSyncS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.text, 1_000_000);
    }

    public static ConsoleSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new ConsoleSyncS2CPacket(buf.readBlockPos(), buf.readUtf(1_000_000));
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var be = mc.level.getBlockEntity(this.pos);
            if (be instanceof com.github.b4ndithelps.forge.blocks.DNASequencerBlockEntity sequencer) {
                sequencer.clientSetConsoleText(this.text);
            }
        }));
        return true;
    }
}


