package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackScreenNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

public class EnderGeneTP extends Ability {

    public EnderGeneTP() {
        super();
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        double startX = entity.getX();
        double startY = entity.getY();
        double startZ = entity.getZ();

        for (int i = 0; i < 16; ++i) { // 16 attempts, just like vanilla
            double targetX = startX + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;
            double targetY = startY + (entity.getRandom().nextInt(16) - 8);
            double targetZ = startZ + (entity.getRandom().nextDouble() - 0.5D) * 16.0D;

            if (entity.isPassenger()) entity.stopRiding();

            Vec3 oldPos = entity.position();
            if (entity.randomTeleport(targetX, targetY, targetZ, true)) {
                level.playSound(null, oldPos.x, oldPos.y, oldPos.z,
                        SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                entity.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }

    }
}