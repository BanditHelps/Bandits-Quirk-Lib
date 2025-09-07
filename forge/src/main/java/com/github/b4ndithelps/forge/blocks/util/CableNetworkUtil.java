package com.github.b4ndithelps.forge.blocks.util;

import com.github.b4ndithelps.forge.blocks.BioCableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility to traverse cable networks from a starting position and collect connected targets.
 */
public final class CableNetworkUtil {
    private CableNetworkUtil() {}

    public static Set<BlockEntity> findConnected(Level level, BlockPos start, Predicate<BlockEntity> isTarget) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockEntity> results = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.pollFirst();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.relative(dir);
                if (!visited.add(neighborPos)) continue;

                BlockState neighborState = level.getBlockState(neighborPos);
                // If neighbor is cable, continue BFS
                if (neighborState.getBlock() instanceof BioCableBlock) {
                    queue.add(neighborPos);
                    continue;
                }
                // If neighbor is a block entity that matches target, collect it
                BlockEntity be = level.getBlockEntity(neighborPos);
                if (be != null && isTarget.test(be)) {
                    results.add(be);
                }
            }
        }

        return results;
    }
}


