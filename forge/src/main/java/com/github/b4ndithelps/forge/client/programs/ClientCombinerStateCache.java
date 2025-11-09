package com.github.b4ndithelps.forge.client.programs;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight client cache for Gene Combiner last result message and status.
 */
public final class ClientCombinerStateCache {
    private ClientCombinerStateCache() {}

    public static final class Entry {
        public boolean success;
        public String message;
        public long lastUpdateGameTime;
    }

    private static final Map<BlockPos, Entry> CACHE = new HashMap<>();

    public static void update(BlockPos pos, boolean success, String message, long gameTime) {
        Entry e = CACHE.computeIfAbsent(pos, p -> new Entry());
        e.success = success;
        e.message = message == null ? "" : message;
        e.lastUpdateGameTime = gameTime;
    }

    public static Entry get(BlockPos pos) { return CACHE.get(pos); }
}