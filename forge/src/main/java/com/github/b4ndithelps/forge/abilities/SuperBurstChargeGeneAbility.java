package com.github.b4ndithelps.forge.abilities;


import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import com.github.b4ndithelps.forge.particle.ModParticles;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.utils.ActionBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

public class SuperBurstChargeGeneAbility extends Ability {
    private static final float FACTOR_SCALING = 10F;
    // Unique properties for tracking state
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public SuperBurstChargeGeneAbility() {
        super();
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
    }

//    @Override
//    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
//        if (enabled && entity instanceof ServerPlayer player) {
//            // Initialize charge ( not needed )
////            entry.setUniqueProperty(CHARGE_TICKS, 0);
////            BodyStatusHelper.setCustomFloat(player, "chest", "burst_gene_charge", 0.0f);
//
//            if (entity.level() instanceof ServerLevel serverLevel) {
//                // Play charging start sound
//                serverLevel.playSound(null, player.blockPosition(),
//                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3f, 1.2f);
//            }
//        }
//    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (!enabled) {
            return;
        }

        player.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), 80, 244));

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "super_burst_charge");
        float factor = (float) (QuirkFactorHelper.getQuirkFactor(player)+1) * charge/20;


        RandomSource random = serverLevel.getRandom();


        double x = player.getX();
        double y = player.getY() +1;
        double z = player.getZ();

        serverLevel.sendParticles(ModParticles.CHARGE_DUST_PARTICLE.get(), x, y, z, 1, 1.5, 1.5, 1.5, 0);

        if (charge == 0) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1, 1);
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PlayerAnimationPacket("core_explosion_burst")
            );
        }

        sendInfoMessage(player, charge, factor);
        if (player.getHealth() - factor*FACTOR_SCALING <= 0) {
            BodyStatusHelper.setCustomFloat(player, "chest", "super_burst_charge", charge-0.5f);
        }
        BodyStatusHelper.setCustomFloat(player, "chest", "super_burst_charge", ++charge);

    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer player && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 2, 1);

            BodyStatusHelper.setCustomFloat(player, "chest", "super_burst_render", 1);

        }
    }

    private void sendInfoMessage(ServerPlayer player, float chargePercent, float factor) {
        ChatFormatting color = ChatFormatting.GREEN;

        if (player.getHealth() - factor*FACTOR_SCALING <= 0) {
            color = ChatFormatting.BLACK;
        } else if (player.getHealth() - factor*FACTOR_SCALING <= player.getHealth() * 0.25f) {
            color = ChatFormatting.DARK_RED;
        } else if (player.getHealth() - factor*FACTOR_SCALING <= player.getHealth() * 0.6f) {
            color = ChatFormatting.YELLOW;
        }
        ActionBarHelper.sendPercentageDisplay(
                player,
                "Super Burst Charge",
                chargePercent,
                ChatFormatting.GRAY,
                color,
                chargePercent >= 400.0f ? "MAX" : "Charging"
        );
    }



    @Override
    public String getDocumentationDescription() {
        return "Burst ability";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}
