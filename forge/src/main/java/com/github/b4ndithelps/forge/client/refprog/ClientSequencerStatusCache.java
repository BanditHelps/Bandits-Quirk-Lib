package com.github.b4ndithelps.forge.client.refprog;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight client cache of sequencer states to allow UI to update immediately after server changes.
 */
public final class ClientSequencerStatusCache {
    private static final Map<BlockPos, Entry> CACHE = new HashMap<>();

    public static final class Entry {
        public boolean running;
        public boolean analyzed;
        public long lastUpdateGameTime;
    }

    private ClientSequencerStatusCache() {}

    public static void update(BlockPos pos, boolean running, boolean analyzed, long gameTime) {
        Entry e = CACHE.computeIfAbsent(pos, p -> new Entry());
        e.running = running;
        e.analyzed = analyzed;
        e.lastUpdateGameTime = gameTime;
    }

    public static Entry get(BlockPos pos) { return CACHE.get(pos); }
}


