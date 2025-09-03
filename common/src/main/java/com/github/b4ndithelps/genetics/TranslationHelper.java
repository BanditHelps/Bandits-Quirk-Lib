package com.github.b4ndithelps.genetics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class TranslationHelper {
    private static final Gson GSON = new Gson();
    private static Map<String, String> cached;

    private TranslationHelper() {}

    public static Map<String, String> getEnUs() {
        if (cached != null) return cached;
        Map<String, String> out = new HashMap<>();
        try (InputStream is = TranslationHelper.class.getResourceAsStream("/assets/bandits_quirk_lib/lang/en_us.json")) {
            if (is != null) {
                JsonObject obj = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    if (e.getValue().isJsonPrimitive()) {
                        out.put(e.getKey(), e.getValue().getAsString());
                    }
                }
            }
        } catch (Exception ignored) {}
        cached = out;
        return out;
    }
}


