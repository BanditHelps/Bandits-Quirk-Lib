package com.github.b4ndithelps.forge.client.programs;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight client cache for Gene Slicer input labels and running state.
 */
public final class ClientSlicerStateCache {
    private ClientSlicerStateCache() {}

    public static final class Entry {
        public final List<String> labels = new ArrayList<>();
        public boolean running;
        public long lastUpdateGameTime;
    }

    private static final Map<BlockPos, Entry> CACHE = new HashMap<>();

    public static void update(BlockPos pos, List<String> labels, boolean running, long gameTime) {
        Entry e = CACHE.computeIfAbsent(pos, p -> new Entry());
        e.labels.clear();
        if (labels != null) e.labels.addAll(labels);
        e.running = running;
        e.lastUpdateGameTime = gameTime;
    }

    public static Entry get(BlockPos pos) { return CACHE.get(pos); }
}