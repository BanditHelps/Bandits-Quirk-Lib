package com.github.b4ndithelps.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

public final class BQLNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int index = 0;
        CHANNEL.messageBuilder(NoShadowTagPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NoShadowTagPacket::encode)
                .decoder(NoShadowTagPacket::decode)
                .consumerMainThread(NoShadowTagPacket::handle)
                .add();

        
        CHANNEL.messageBuilder(BodyStatusSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BodyStatusSyncPacket::encode)
                .decoder(BodyStatusSyncPacket::decode)
                .consumerMainThread(BodyStatusSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(StaminaSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StaminaSyncPacket::encode)
                .decoder(StaminaSyncPacket::decode)
                .consumerMainThread(StaminaSyncPacket::handle)
                .add();
    }
}


