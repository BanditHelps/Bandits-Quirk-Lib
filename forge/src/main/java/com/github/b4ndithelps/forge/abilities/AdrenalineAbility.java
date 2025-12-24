package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.config.BQLConfig;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackScreenNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

public class AdrenalineAbility extends Ability {

    public AdrenalineAbility() {
        super();
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        long ticks = level.getGameTime();
        long lastUseTick = player.getPersistentData().getLong("lastUseTimeAdrenaline");

        if (ticks - lastUseTick < BQLConfig.INSTANCE.adrenalineCooldown.get() * 20) { // 60 seconds cooldown
            return;
        }

        player.getPersistentData().putLong("lastUseTimeAdrenaline", ticks);
        BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlackScreenNetwork(player.getUUID()));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BQLConfig.INSTANCE.adrenalineDurationSeconds.get(), 1));

    }
}
