package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.icon.IIcon;
import net.threetag.palladium.util.property.CommandFunctionProperty;
import net.threetag.palladium.util.property.ComponentProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HappenOnceCondition extends Condition {
    private static final Map<UUID, Set<String>> executedAbilities = new ConcurrentHashMap<>();

    private final String identifier;

    public HappenOnceCondition(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();

        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        UUID playerId = player.getUUID();
        String uniqueKey = "MineHa.HappenOnce." + identifier;

        // get or create the set of executed abilities for the player
        Set<String> playerExecuted = executedAbilities.computeIfAbsent(playerId, k -> {
            Set<String> newSet = ConcurrentHashMap.newKeySet();
            loadFromPersistentData(player, newSet);
            return newSet;
        });

        if (playerExecuted.contains(uniqueKey)) {
            return false;
        }

        playerExecuted.add(uniqueKey);
        saveToPersistentData(player, uniqueKey);
        return true;
    }

    @Override
    public ConditionSerializer getSerializer() {
//        return CustomConditionSerializers.HAPPEN_ONCE.get();

        return null;
    }

    private void loadFromPersistentData(ServerPlayer player, Set<String> playerExecuted) {
        try {
            var persistentData = player.getPersistentData();
            var compound = persistentData.getCompound("MineHa.HappenOnce");

            // Load all boolean keys that are true
            for (String key : compound.getAllKeys()) {
                if (compound.getBoolean(key));
                playerExecuted.add(key);
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to load happen once fata for player " + player.getGameProfile().getName());
        }
    }

    private void saveToPersistentData(ServerPlayer player, String uniqueKey) {
        try {
            var persistentData = player.getPersistentData();
            var compound = persistentData.getCompound("MineHa.HappenOnce");
            compound.putBoolean(uniqueKey, true);
            persistentData.put("MineHa.HappenOnce", compound);
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to save happen once fata for player " + player.getGameProfile().getName());
        }
    }

    // Clean up data when player leaves (optional, call this from a player logout event)
    public static void cleanupPlayerData(UUID playerId) {
        executedAbilities.remove(playerId);
    }

    public static class Serializer extends ConditionSerializer {
        public static final PalladiumProperty<String> KEY = (new StringProperty("key")).configurable("A unique identifier for the data to be saved under.");

        public Serializer() {
            this.withProperty(KEY, "unique_key");
        }

        public ConditionEnvironment getContextEnvironment() { return ConditionEnvironment.ALL; }

        public Condition make(JsonObject json) {
            return new HappenOnceCondition(this.getProperty(json, KEY));
        }

        public String getDocumentationDescription() {
            return "A condition that makes the ability only fire once, and never again";
        }
    }
}
