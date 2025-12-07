package com.github.b4ndithelps.forge.abilities.frog;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipAuraPacket;
import com.github.b4ndithelps.forge.network.ParticlePacket;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.entity.PalladiumAttributes;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;

import java.util.UUID;

public class JumperAbility extends Ability {
    private static final int MAX_CHARGE = 20;
    private static final UUID JUMP_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public JumperAbility() {
        super();
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;

        AttributeInstance jumpAttribute = player.getAttribute(PalladiumAttributes.JUMP_POWER.get());

        AttributeModifier existingModifier = jumpAttribute.getModifier(JUMP_MODIFIER_UUID);
        if (existingModifier != null) {
            jumpAttribute.removeModifier(JUMP_MODIFIER_UUID);
        }

        if (BodyStatusHelper.isPartDestroyed(player, "leg")) {BodyStatusHelper.setCustomFloat(player, "chest", "frogjumper", 0); return;}

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "frogjumper");

        AttributeModifier modifier = new AttributeModifier(
                JUMP_MODIFIER_UUID,
                "frog_jumper",
                charge/20,
                AttributeModifier.Operation.ADDITION
        );
        jumpAttribute.addPermanentModifier(modifier);

        if (!enabled) {
            BodyStatusHelper.setCustomFloat(player, "chest", "frogjumper", Math.max(0, --charge));
        } else {
            if (BodyStatusHelper.getCustomFloat(player, "chest", "frog_jumper_toggle") % 2 == 0) return;
            if (charge != MAX_CHARGE) {
                BodyStatusHelper.setCustomFloat(player, "chest", "frogjumper",  ++charge);
            } else {
                BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new ParticlePacket(
                                "totem_of_undying",
                                 player.getX(),
                                 player.getY() + 0.4,
                                 player.getZ(),
                                1,0.5,1,
                                1
                        ));
            }

        }

    }
}
