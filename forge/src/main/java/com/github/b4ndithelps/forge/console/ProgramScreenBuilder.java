package com.github.b4ndithelps.forge.console;

import java.util.ArrayList;
import java.util.List;

import static com.github.b4ndithelps.forge.console.ConsoleText.*;

/**
 * Builder for composing program screen text with headers, separators, alignment and colors.
 */
public final class ProgramScreenBuilder {
    private final List<String> lines = new ArrayList<>();

    public ProgramScreenBuilder header(String title) {
        lines.add(center(color(title, ColorTag.AQUA)));
        lines.add(center(sep(Math.max(16, Math.min(64, title.length() + 6)))));
        return this;
    }

    public ProgramScreenBuilder line(String text) {
        lines.add(text);
        return this;
    }

    public ProgramScreenBuilder line(String text, ColorTag color) {
        lines.add(color(text, color));
        return this;
    }

    public ProgramScreenBuilder centerLine(String text) {
        lines.add(center(text));
        return this;
    }

    public ProgramScreenBuilder rightLine(String text) {
        lines.add(right(text));
        return this;
    }

    public ProgramScreenBuilder blank() {
        lines.add("");
        return this;
    }

    public ProgramScreenBuilder separator() {
        lines.add(sep(33));
        return this;
    }

    public ProgramScreenBuilder progressBar(int percent, int width) {
        width = Math.max(1, width);
        int filled = Math.max(0, Math.min(width, (percent * width) / 100));
        StringBuilder r = new StringBuilder("[");
        for (int i = 0; i < width; i++) r.append(i < filled ? '|' : '.');
        r.append("] ").append(percent).append("%");
        lines.add(r.toString());
        return this;
    }

    public ProgramScreenBuilder twoColumn(String left, String right, int leftWidth) {
        if (left == null) left = "";
        if (right == null) right = "";
        lines.add(String.format("%-" + leftWidth + "s  %s", left, right));
        return this;
    }

    public String build() {
        return String.join("\n", lines);
    }
}


