package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.client.refprog.ClientCatalogCache;
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
            buf.writeUtf(e.type, 16);
            buf.writeUtf(e.label, 256);
            buf.writeVarInt(e.sourceIndex);
            buf.writeVarInt(e.slotIndex);
        }
    }

    public static CatalogEntriesS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos term = buf.readBlockPos();
        int n = buf.readVarInt();
        List<ClientCatalogCache.EntryDTO> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String type = buf.readUtf(16);
            String label = buf.readUtf(256);
            int sourceIndex = buf.readVarInt();
            int slotIndex = buf.readVarInt();
            list.add(new ClientCatalogCache.EntryDTO(type, label, sourceIndex, slotIndex));
        }
        return new CatalogEntriesS2CPacket(term, list);
    }

    public static void handle(CatalogEntriesS2CPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ClientCatalogCache.put(msg.terminalPos, msg.entries);
        });
        ctx.setPacketHandled(true);
    }
}


