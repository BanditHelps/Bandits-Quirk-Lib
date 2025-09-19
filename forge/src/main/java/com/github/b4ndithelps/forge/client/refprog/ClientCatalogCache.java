package com.github.b4ndithelps.forge.client.refprog;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache for catalog entries per terminal position.
 */
public final class ClientCatalogCache {
    private ClientCatalogCache() {}

    public static final class EntryDTO {
        public final String type; // "VIAL", "SEQUENCED_SAMPLE", or "SECTION"
        public final String label;
        public final int sourceIndex;
        public final int slotIndex;

        public EntryDTO(String type, String label, int sourceIndex, int slotIndex) {
            this.type = type;
            this.label = label;
            this.sourceIndex = sourceIndex;
            this.slotIndex = slotIndex;
        }
    }

    private static final Map<BlockPos, List<EntryDTO>> cache = new HashMap<>();

    public static void put(BlockPos terminal, List<EntryDTO> entries) {
        cache.put(terminal, new ArrayList<>(entries));
    }

    public static List<EntryDTO> get(BlockPos terminal) {
        return cache.getOrDefault(terminal, List.of());
    }
}


