package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.BioTerminalScreen;
import com.github.b4ndithelps.forge.client.programs.CatalogProgram;
import com.github.b4ndithelps.forge.client.programs.ClientCatalogCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CatalogEntriesS2CPacket {
    private final BlockPos terminalPos;
    private final List<ClientCatalogCache.EntryDTO> entries;

    public CatalogEntriesS2CPacket(BlockPos terminalPos, List<ClientCatalogCache.EntryDTO> entries) {
        this.terminalPos = terminalPos;
        this.entries = entries;
    }

    public static void encode(CatalogEntriesS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.terminalPos);
        buf.writeVarInt(msg.entries.size());
        for (var e : msg.entries) {
            buf.writeUtf(e.type, 24);
            buf.writeUtf(e.label, 256);
            buf.writeUtf(e.geneId, 256);
            buf.writeVarInt(e.quality);
            buf.writeBoolean(e.known);
            buf.writeVarInt(e.progress);
            buf.writeVarInt(e.max);
            buf.writeVarInt(e.sourceIndex);
            buf.writeVarInt(e.slotIndex);
            // Optional source position
            buf.writeBoolean(e.sourcePos != null);
            if (e.sourcePos != null) buf.writeBlockPos(e.sourcePos);
        }
    }

    public static CatalogEntriesS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos term = buf.readBlockPos();
        int n = buf.readVarInt();
        List<ClientCatalogCache.EntryDTO> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String type = buf.readUtf(24);
            String label = buf.readUtf(256);
            String geneId = buf.readUtf(256);
            int quality = buf.readVarInt();
            boolean known = buf.readBoolean();
            int progress = buf.readVarInt();
            int max = buf.readVarInt();
            int sourceIndex = buf.readVarInt();
            int slotIndex = buf.readVarInt();
            BlockPos sourcePos = null;
            boolean hasPos = buf.readBoolean();
            if (hasPos) sourcePos = buf.readBlockPos();
            list.add(new ClientCatalogCache.EntryDTO(type, label, geneId, quality, known, progress, max, sourceIndex, slotIndex, sourcePos));
        }
        return new CatalogEntriesS2CPacket(term, list);
    }

    public static void handle(CatalogEntriesS2CPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ClientCatalogCache.put(msg.terminalPos, msg.entries);
            // Proactively refresh the catalog screen if it's open so progress appears immediately
            try {
                var mc = Minecraft.getInstance();
                if (mc != null && mc.screen instanceof BioTerminalScreen scr) {
                    // Only refresh if catalog tab is active
                    java.lang.reflect.Field f = BioTerminalScreen.class.getDeclaredField("catalogProgram");
                    f.setAccessible(true);
                    Object prog = f.get(scr);
                    if (prog instanceof CatalogProgram cat) {
                        cat.refresh();
                    }
                }
            } catch (Throwable ignored) {}
        });
        ctx.setPacketHandled(true);
    }
}