package com.github.b4ndithelps.forge.utils;


import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransparencyManager {
    private static final Map<UUID, ArrayList<Float>> TRANSPARENCY_VALUES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> CURRENT_TRANSPARENCY = new ConcurrentHashMap<>();

    public static ArrayList<Float> getOrCreate(UUID uuid) {
        return TRANSPARENCY_VALUES.computeIfAbsent(uuid, id -> {
            ArrayList<Float> list = new ArrayList<>();
            list.add(1.0f);       // base value
            return list;
        });
    }

    public static void initializePlayer(UUID uuid) {
        getOrCreate(uuid);
        if (!CURRENT_TRANSPARENCY.containsKey(uuid)) {
            CURRENT_TRANSPARENCY.put(uuid, 1.0f);
        }
    }

    public static void addTransparency(UUID uuid, float value) {
        ArrayList<Float> list = getOrCreate(uuid);
        for (int i = 0; i < list.size(); i++) {
            float current = list.get(i);
            if (value < current) {
                list.add(i, value);
                return;
            }
        }
        list.add(value);
    }

    public static void removeTransparency(UUID uuid, float value) {
        ArrayList<Float> list = TRANSPARENCY_VALUES.get(uuid);
        list.remove(value);
    }

    public static float getTransparency(UUID uuid) {
        float current = CURRENT_TRANSPARENCY.get(uuid);
        if (current < TRANSPARENCY_VALUES.get(uuid).get(0)) {
            CURRENT_TRANSPARENCY.replace(uuid, current + 0.01f);
        } else if (current > TRANSPARENCY_VALUES.get(uuid).get(0)) {
            CURRENT_TRANSPARENCY.replace(uuid, current - 0.01f);
        }
        return current;
    }
}
