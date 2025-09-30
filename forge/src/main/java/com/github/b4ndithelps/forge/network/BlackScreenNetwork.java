package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.capabilities.StaminaDataProvider;
import com.github.b4ndithelps.forge.client.ScreenFadeOverlay;
import com.github.b4ndithelps.forge.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet for synchronizing Stamina capability data (including upgrade points) from server to client.
 */
public class BlackScreenNetwork {
    private final UUID playerUUID;

    public BlackScreenNetwork(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUUID);
    }

    public static BlackScreenNetwork decode(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        return new BlackScreenNetwork(id);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide()));
        return true;
    }

    private void handleClientSide() {
        Player player = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getPlayerByUUID(playerUUID) : null;
        if (player == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(
                    ModSounds.HEARTBEAT.get(), // example sound
                    1.0F, // volume
                    1.0F  // pitch
            );
        }

        ScreenFadeOverlay.startFadeOut();
    }

    public static BlackScreenNetwork fullSync(Player player) {
        CompoundTag tag = new CompoundTag();
        return new BlackScreenNetwork(player.getUUID());
    }
}


