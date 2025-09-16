package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

public class AmpuleSelfRemove extends Ability {

    public AmpuleSelfRemove() {
        super();
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        float cd = BodyStatusHelper.getCustomFloat(player, "chest", "ampuleCd");
        BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddictionCD", 300);


        if (cd > 1) {
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", --cd);
        } else if (cd == 1) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective objective = scoreboard.getObjective("MineHa.QuirkFactor");
            Score quirkFactor = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
            int tier = 0;
            int factor = 0;

            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", --cd);
            player.displayClientMessage(Component.literal("As the effects of ampule fade, your body feels weaker"), true);
            float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");
            if ((usage / 100) % 10 >= 1) {
                tier++;
                factor++;
            }
            if ((usage / 10) % 10 >= 1) {
                tier++;
            }
            if (usage % 10 >= 1) {
                tier++;
            }
            quirkFactor.setScore(quirkFactor.getScore() - factor - tier);
            BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddictionCD", 300+ 100*tier);
            player.server.getCommands().performPrefixedCommand(
                    player.server.createCommandSourceStack()
                            .withSuppressedOutput(),
                    "bql Dev set exhaust " + tier
            );
//            StaminaHelper.getStaminaDataSafe(player).setExhaustionLevel(tier);
            BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", 0);
            SuperpowerUtil.removeSuperpower(player, ResourceLocation.read("banditsquirklib:ampule_use").get().orThrow());

        }
    }
}
