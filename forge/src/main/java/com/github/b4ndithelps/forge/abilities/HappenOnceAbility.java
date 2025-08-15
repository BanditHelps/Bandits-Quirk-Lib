package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import dev.latvian.mods.kubejs.server.tag.TagEventFilter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.CommandAbility;
import net.threetag.palladium.util.property.ComponentProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HappenOnceAbility extends CommandAbility {

    public static final PalladiumProperty<Component> IDENTIFIER = (new ComponentProperty("key")).configurable("The key to uniquely identify the ability.");
    private static final Map<UUID, Set<String>> executedAbilities = new ConcurrentHashMap<>();

    public HappenOnceAbility() {
        super();
        this.withProperty(IDENTIFIER, Component.literal("unique_key"));
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entry.isEnabled()) {
            if (hasAlreadyRan(entity, entry)) return;
            super.tick(entity, entry, holder, enabled);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
    }

    private boolean hasAlreadyRan(LivingEntity entity, AbilityInstance entry) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        UUID playerId = player.getUUID();

        if (entry.getProperty(IDENTIFIER) != null) {
            String uniqueKey = "BQL.HappenOnce." + entry.getProperty(IDENTIFIER).getString();
            //BanditsQuirkLibForge.LOGGER.info("Key is " + uniqueKey);

            // Get or create the set of executed abilities for this player
            Set<String> playerExecuted = executedAbilities.computeIfAbsent(playerId, k -> {
                Set<String> newSet = ConcurrentHashMap.newKeySet();
                // Load from persistent data on first access
                loadFromPersistentData(player, newSet);
                return newSet;
            });

            if (playerExecuted.contains(uniqueKey)) {
                return true;
            }

            playerExecuted.add(uniqueKey);
            saveToPersistentData(player, uniqueKey);
            return false;
        }

        return true;
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

    @Override
    public String getDocumentationDescription() {
        return "Executes all of the commands a single time. Saves through server restart.";
    }

    // Clean up data when player leaves (optional, call this from a player logout event)
    public static void cleanupPlayerData(UUID playerId) {
        executedAbilities.remove(playerId);
    }

}
