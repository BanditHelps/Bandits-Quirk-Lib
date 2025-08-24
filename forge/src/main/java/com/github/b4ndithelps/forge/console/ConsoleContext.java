package com.github.b4ndithelps.forge.console;

import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;

import java.util.List;

/**
 * Per-execution context that provides safe access to the DNA Sequencer state and output utilities.
 */
public final class ConsoleContext {
    private final BioTerminalBlockEntity blockEntity;

    public ConsoleContext(BioTerminalBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    /**
     * @return the live block entity for this console execution.
     */
    public BioTerminalBlockEntity getBlockEntity() {
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

    // --- Program control ---
    public void pushProgram(ConsoleProgram program) {
        blockEntity.pushProgram(program);
    }

    public void exitProgram() {
        blockEntity.exitCurrentProgram();
    }

    // --- Program screen ---
    public void setScreenText(String text) {
        blockEntity.setProgramScreenText(text == null ? "" : text);
    }

    // --- Command history navigation ---
    public String historyPrev() {
        return blockEntity.historyPrev();
    }

    public String historyNext() {
        return blockEntity.historyNext();
    }
}


