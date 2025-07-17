package com.github.b4ndithelps.forge.capabilities.Body;

public enum BodyPart {
    HEAD("head"),
    CHEST("chest"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm"),
    LEFT_LEG("left_leg"),
    RIGHT_LEG("right_leg"),
    LEFT_HAND("left_hand"),
    RIGHT_HAND("right_hand"),
    LEFT_FOOT("left_foot"),
    RIGHT_FOOT("right_foot");

    private final String name;

    BodyPart(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
