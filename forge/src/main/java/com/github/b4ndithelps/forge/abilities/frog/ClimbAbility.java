package com.github.b4ndithelps.forge.abilities.frog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

public class ClimbAbility extends Ability {
    public ClimbAbility() {
        super();
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (entity instanceof ServerPlayer) return;

        if (entity.horizontalCollision) {
            if (entity.isCrouching()) {
                entity.setDeltaMovement(entity.getDeltaMovement().x, 0.0, entity.getDeltaMovement().z);
            } else entity.setDeltaMovement(entity.getDeltaMovement().x, 0.2, entity.getDeltaMovement().z);
        }
    }


}
