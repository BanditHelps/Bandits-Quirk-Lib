package com.github.b4ndithelps.forge.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Common small text animations for console programs.
 */
public final class ProgramAnimations {
    private ProgramAnimations() {}

    public static List<String> loadingDots(String base, int frames) {
        frames = Math.max(1, frames);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < frames; i++) {
            int dots = i % 4;
            out.add(base + ".".repeat(dots));
        }
        return out;
    }

    public static List<String> spinner(String base, int frames) {
        frames = Math.max(1, frames);
        char[] seq = new char[]{'|', '/', '-', '\\'};
        List<String> out = new ArrayList<>();
        for (int i = 0; i < frames; i++) {
            out.add(base + " " + seq[i % seq.length]);
        }
        return out;
    }

    public static List<String> progressFrames(int totalFrames, int width) {
        totalFrames = Math.max(1, totalFrames);
        width = Math.max(1, width);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < totalFrames; i++) {
            int pct = (i * 100) / (totalFrames - 1);
            int filled = Math.max(0, Math.min(width, (pct * width) / 100));
            StringBuilder r = new StringBuilder("[");
            for (int j = 0; j < width; j++) r.append(j < filled ? '|' : '.');
            r.append("] ").append(pct).append("%");
            out.add(r.toString());
        }
        return out;
    }
}


