package com.github.b4ndithelps.forge.console;

import java.util.Objects;

/**
 * Small helpers for composing console text with tags understood by BioTerminalScreen.
 */
public final class ConsoleText {
    private ConsoleText() {}

    public enum ColorTag {
        RED("[RED]"),
        GREEN("[GREEN]"),
        YELLOW("[YELLOW]"),
        AQUA("[AQUA]"),
        GRAY("[GRAY]"),
        WHITE("");

        private final String tag;

        ColorTag(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    public static String color(String text, ColorTag color) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(color, "color");
        return color.tag() + text;
    }

    public static String center(String text) {
        Objects.requireNonNull(text, "text");
        return "[CENTER]" + text;
    }

    public static String right(String text) {
        Objects.requireNonNull(text, "text");
        return "[RIGHT]" + text;
    }

    public static String sep(int length) {
        if (length <= 0) length = 32;
        return "=".repeat(length);
    }
}


