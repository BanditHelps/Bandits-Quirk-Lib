package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
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

        int tier = 0;
        int factor = 0;

        // Used ampules is stored in binary, each digit is a tier (001 - tier 1, 010 - tier 2, 100 - tier 3, 110 - tier 2 and 3, etc)
        float usage = BodyStatusHelper.getCustomFloat(player, "chest", "usedAmpules");
        // By dividing binary 110 by ten we get 11 ( removing digit from right ). By using % 10 we get that digit ( 11 - 1, 10 - 0, 110 - 0)
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


        if (cd > 20) {
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", cd- 20);
            player.addEffect(new MobEffectInstance(ModEffects.AMPULE_EFFECT.get(), (int) cd, tier-1, false, false));
        } else if (cd == 20) {
            player.removeEffect(ModEffects.AMPULE_EFFECT.get());
            BodyStatusHelper.setCustomFloat(player, "chest", "ampuleCd", cd- 20);
            player.displayClientMessage(Component.literal("As the effects of ampule fade, your body feels weaker"), true);


            double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
            QuirkFactorHelper.setQuirkFactor(player,  quirkFactor-factor - tier);

            BodyStatusHelper.setCustomFloat(player, "head", "ampuleAddictionCD", 300+ 100*tier);
            player.server.getCommands().performPrefixedCommand(
                    player.server.createCommandSourceStack()
                            .withSuppressedOutput(),
                    "bql Dev set exhaust " + tier
            );
//            StaminaHelper.getStaminaDataSafe(player).setExhaustionLevel(tier);
            BodyStatusHelper.setCustomFloat(player, "chest", "usedAmpules", 0);
            SuperpowerUtil.removeSuperpower(player, ResourceLocation.parse("bql:ampule_use"));

        }
    }
}
