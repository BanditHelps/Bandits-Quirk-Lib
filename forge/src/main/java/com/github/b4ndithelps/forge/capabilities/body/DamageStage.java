package com.github.b4ndithelps.forge.capabilities.body;

public enum DamageStage {
    HEALTHY(0, "healthy"),
    SPRAINED(1, "sprained"),
    BROKEN(2, "broken"),
    DESTROYED(3, "destroyed");

    private final int severity;
    private final String name;

    DamageStage(int severity, String name) {
        this.severity = severity;
        this.name = name;
    }

    public int getSeverity() {
        return severity;
    }

    public String getName() {
        return name;
    }

    public static DamageStage fromSeverity(int severity) {
        for (DamageStage stage : values()) {
            if (stage.severity == severity) {
                return stage;
            }
        }
        return HEALTHY;
    }
}
