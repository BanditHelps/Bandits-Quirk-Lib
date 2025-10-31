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
        public final String type; // "VIAL", "SEQUENCED_SAMPLE", "SEQUENCED_GENE", or "SECTION"
        public final String label;
        public final String geneId; // may be empty for SECTION/SAMPLE rows
        public final int quality; // -1 if unknown/not applicable
        public final boolean known; // whether gene is known according to attached DB
        public final int progress; // 0..max
        public final int max; // >=1 when identifying, else 0
        public final int sourceIndex;
        public final int slotIndex;

        public EntryDTO(String type, String label, String geneId, int quality, boolean known, int progress, int max, int sourceIndex, int slotIndex) {
            this.type = type;
            this.label = label;
            this.geneId = geneId == null ? "" : geneId;
            this.quality = quality;
            this.known = known;
            this.progress = progress;
            this.max = max;
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


