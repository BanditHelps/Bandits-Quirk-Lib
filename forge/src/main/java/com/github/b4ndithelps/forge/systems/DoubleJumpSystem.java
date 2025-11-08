package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.DoubleJumpS2CPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side rules and helpers for performing double jumps based on genome gene presence.
 */
public final class DoubleJumpSystem {
    private DoubleJumpSystem() {}

    private static final String TAG_CAN_DOUBLE_JUMP = "Bql.CanDoubleJump";
    private static final String TAG_DJ_COOLDOWN_TICKS = "Bql.DoubleJumpCooldown";

    // Gene id expected in genome list
    private static final String GENE_DOUBLE_JUMP = "bandits_quirk_lib:gene.double_jump";

    /** Called each server tick from player tick handler to refresh jump availability and manage cooldown. */
    public static void serverTick(ServerPlayer player) {
        // Cooldown countdown
        int cd = player.getPersistentData().getInt(TAG_DJ_COOLDOWN_TICKS);
        if (cd > 0) player.getPersistentData().putInt(TAG_DJ_COOLDOWN_TICKS, cd - 1);

        // If on ground or in water/lava, refresh ability to double jump
        if (player.onGround() || player.isInWater() || player.isInLava()) {
            boolean hasGene = hasDoubleJumpGene(player);
            player.getPersistentData().putBoolean(TAG_CAN_DOUBLE_JUMP, hasGene);
        }
    }

    /** Attempt to perform the double jump now; validates state and applies motion. */
    public static void tryDoubleJump(ServerPlayer player) {
        if (!hasDoubleJumpGene(player)) return;

        // Must not be on the ground and must have availability flag
        if (player.onGround()) return;
        if (!player.getPersistentData().getBoolean(TAG_CAN_DOUBLE_JUMP)) return;
        if (player.getPersistentData().getInt(TAG_DJ_COOLDOWN_TICKS) > 0) return;

        // Apply upward boost, preserve horizontal velocity
        Vec3 motion = player.getDeltaMovement();
        double verticalBoost = 0.6D; // roughly between jump and jump with effects
        // If falling too fast, clamp downward speed first
        double newY = Math.max(-0.2D, motion.y) + verticalBoost;
        player.setDeltaMovement(motion.x, newY, motion.z);
        player.hasImpulse = true;

        // Spend the double jump and set brief cooldown to prevent spamming
        player.getPersistentData().putBoolean(TAG_CAN_DOUBLE_JUMP, false);
        player.getPersistentData().putInt(TAG_DJ_COOLDOWN_TICKS, 6);

        player.fallDistance = 0.0F;
        // Play a subtle poof-like sound and spawn small cloud at feet
        ServerLevel serverLevel = player.serverLevel();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.15f);
        double fxX = player.getX();
        double fxY = player.getY();
        double fxZ = player.getZ();
        serverLevel.sendParticles(ParticleTypes.POOF, fxX, fxY + 0.05, fxZ, 8, 0.2, 0.05, 0.2, 0.01);
        serverLevel.sendParticles(ParticleTypes.CLOUD, fxX, fxY + 0.05, fxZ, 6, 0.15, 0.02, 0.15, 0.0);

        // Also inform the client to apply the same boost immediately to avoid interpolation delays
        BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DoubleJumpS2CPacket((float) verticalBoost));
    }

    private static boolean hasDoubleJumpGene(Player player) {
        // Quick scan of genome list for gene id
        var genes = GenomeHelper.getGenome(player);
        for (int i = 0; i < genes.size(); i++) {
            var g = genes.getCompound(i);
            if (GENE_DOUBLE_JUMP.equals(g.getString("id"))) return true;
        }
        return false;
    }
}