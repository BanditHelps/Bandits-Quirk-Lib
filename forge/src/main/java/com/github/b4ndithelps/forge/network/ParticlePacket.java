package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.BanditsQuirkLib;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static dev.kosmx.playerAnim.core.util.Ease.INOUTSINE;


// USE THIS TO PLAY PLAYER !MAIN! ANIMATION
public class ParticlePacket {
    private static final Map<String, SimpleParticleType> particles = new HashMap<String, SimpleParticleType>();

    static {
        particles.put("totem_of_undying", ParticleTypes.TOTEM_OF_UNDYING);
    }

    private final String particle;
    private final double x;
    private final double y;
    private final double z;
    private final double spreadx;
    private final double spready;
    private final double spreadz;
    private final int count;

    public ParticlePacket(String particle, double x, double y, double z, double spreadx, double spready, double spreadz, int count) {
        this.particle = particle;
        this.x = x;
        this.y = y;
        this.z = z;
        this.spreadx = spreadx;
        this.spready = spready;
        this.spreadz = spreadz;
        this.count = count;

    }

    public static void encode(ParticlePacket msg, FriendlyByteBuf buf) {

        buf.writeUtf(msg.particle, 256);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeDouble(msg.spreadx);
        buf.writeDouble(msg.spready);
        buf.writeDouble(msg.spreadz);
        buf.writeInt(msg.count);
    }

    public static ParticlePacket decode(FriendlyByteBuf buf) {
        return new ParticlePacket(
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt());
    }

    public static void handle(ParticlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            RandomSource random = player.level().random;

            for (int i = 0; i < msg.count; i++) {
                double offsetX = (random.nextDouble() - 0.5) * msg.spreadx;   // random spread around x
                double offsetY = (random.nextDouble() - 0.5) * msg.spready;   // random spread around y
                double offsetZ = (random.nextDouble() - 0.5) * msg.spreadz;

                player.level().addParticle(
                        particles.getOrDefault(msg.particle, ParticleTypes.SMOKE), // any particle type
                        msg.x + offsetX,       // X position
                        msg.y + offsetY, // Y position (player’s feet are around getY())
                        msg.z + offsetZ,       // Z position
                        0.0,
                        0.0,
                        0.0               // speed
                );
            }

        });
        ctx.get().setPacketHandled(true);
    }

}