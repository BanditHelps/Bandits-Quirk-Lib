package com.github.b4ndithelps.forge.console;

import java.util.List;

/**
 * Base class for console programs providing common helpers for rendering and status handling.
 */
public abstract class AbstractConsoleProgram implements ConsoleProgram {
    protected String transientStatus = "";

    protected void setStatusInfo(String msg) { this.transientStatus = ConsoleText.color(msg, ConsoleText.ColorTag.AQUA); }
    protected void setStatusOk(String msg) { this.transientStatus = ConsoleText.color(msg, ConsoleText.ColorTag.GREEN); }
    protected void setStatusWarn(String msg) { this.transientStatus = ConsoleText.color(msg, ConsoleText.ColorTag.YELLOW); }
    protected void setStatusErr(String msg) { this.transientStatus = ConsoleText.color(msg, ConsoleText.ColorTag.RED); }

    protected ProgramScreenBuilder screen() {
        return new ProgramScreenBuilder();
    }

    protected void render(ConsoleContext ctx, String text) {
        if (transientStatus != null && !transientStatus.isEmpty()) {
            ctx.setScreenText(transientStatus + "\n\n" + (text == null ? "" : text));
        } else {
            ctx.setScreenText(text == null ? "" : text);
        }
    }

    /**
     * Utility for simple command dispatch maps.
     */
    protected boolean dispatch(ConsoleContext ctx, String name, List<String> args, List<Handler> handlers) {
        for (Handler h : handlers) {
            if (h.name.equalsIgnoreCase(name)) {
                h.exec.run(ctx, args);
                return true;
            }
        }
        return false;
    }

    protected record Handler(String name, Exec exec) {}
    @FunctionalInterface protected interface Exec { void run(ConsoleContext ctx, List<String> args); }
}


