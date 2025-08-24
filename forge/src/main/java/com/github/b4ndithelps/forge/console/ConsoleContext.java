package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.forge.blocks.DNASequencerBlockEntity;

import java.util.List;

/**
 * Per-execution context that provides safe access to the DNA Sequencer state and output utilities.
 */
public final class ConsoleContext {
    private final DNASequencerBlockEntity blockEntity;

    public ConsoleContext(DNASequencerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    /**
     * @return the live block entity for this console execution.
     */
    public DNASequencerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /**
     * Append a single line to the console output immediately.
     */
    public void println(String line) {
        blockEntity.appendConsole(line);
    }

    /**
     * Remove all the lines inside the console. Then just adds in a
     */
    public void clearConsole() {
        blockEntity.clearConsole();
        println("> ");
    }

    /**
     * Enqueue multiple lines with a delay in ticks between each, allowing for simple animations.
     */
    public void enqueueLines(List<String> lines, int ticksBetween) {
        blockEntity.queueConsoleLines(lines, ticksBetween);
    }

    /**
     * Enqueue the characters in a line with a delay in ticks between
     */
    public void enqueueCharacters(String line, int ticksBetween) {
        blockEntity.queueSingleConsoleLine(line, ticksBetween);
    }

    // --- Command history navigation ---
    public String historyPrev() {
        return blockEntity.historyPrev();
    }

    public String historyNext() {
        return blockEntity.historyNext();
    }
}


