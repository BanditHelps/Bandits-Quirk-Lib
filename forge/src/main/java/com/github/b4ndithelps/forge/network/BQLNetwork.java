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

        CHANNEL.messageBuilder(ZoomStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ZoomStatePacket::encode)
                .decoder(ZoomStatePacket::decode)
                .consumerMainThread(ZoomStatePacket::handle)
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

        CHANNEL.messageBuilder(MineHaSlotSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MineHaSlotSyncPacket::encode)
                .decoder(MineHaSlotSyncPacket::decode)
                .consumerMainThread(MineHaSlotSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(DoubleJumpC2SPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DoubleJumpC2SPacket::encode)
                .decoder(DoubleJumpC2SPacket::decode)
                .consumerMainThread(DoubleJumpC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(DoubleJumpS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DoubleJumpS2CPacket::encode)
                .decoder(DoubleJumpS2CPacket::decode)
                .consumerMainThread(DoubleJumpS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(GenomeSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GenomeSyncPacket::encode)
                .decoder(GenomeSyncPacket::decode)
                .consumerMainThread(GenomeSyncPacket::handle)
                .add();

        // Ref-screen program control (client -> server)
        CHANNEL.messageBuilder(RefProgramActionC2SPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RefProgramActionC2SPacket::encode)
                .decoder(RefProgramActionC2SPacket::decode)
                .consumerMainThread(RefProgramActionC2SPacket::handle)
                .add();

        // Sequencer state updates (server -> clients)
        CHANNEL.messageBuilder(SequencerStateS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SequencerStateS2CPacket::encode)
                .decoder(SequencerStateS2CPacket::decode)
                .consumerMainThread(SequencerStateS2CPacket::handle)
                .add();

        // Catalog entries (server -> client)
        CHANNEL.messageBuilder(CatalogEntriesS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CatalogEntriesS2CPacket::encode)
                .decoder(CatalogEntriesS2CPacket::decode)
                .consumerMainThread(CatalogEntriesS2CPacket::handle)
                .add();

        // Slicer state updates (server -> client)
        CHANNEL.messageBuilder(SlicerStateS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SlicerStateS2CPacket::encode)
                .decoder(SlicerStateS2CPacket::decode)
                .consumerMainThread(SlicerStateS2CPacket::handle)
                .add();

        // Combiner result updates (server -> client)
        CHANNEL.messageBuilder(CombinerStateS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CombinerStateS2CPacket::encode)
                .decoder(CombinerStateS2CPacket::decode)
                .consumerMainThread(CombinerStateS2CPacket::handle)
                .add();

        // Printer result updates (server -> client)
        CHANNEL.messageBuilder(PrinterStateS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PrinterStateS2CPacket::encode)
                .decoder(PrinterStateS2CPacket::decode)
                .consumerMainThread(PrinterStateS2CPacket::handle)
                .add();

        // Open gene graph (server -> client)
        CHANNEL.messageBuilder(OpenGeneGraphS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenGeneGraphS2CPacket::encode)
                .decoder(OpenGeneGraphS2CPacket::decode)
                .consumerMainThread(OpenGeneGraphS2CPacket::handle)
                .add();

        // Blackwhip visual updates (server -> clients)
        CHANNEL.messageBuilder(BlackwhipStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BlackwhipStatePacket::encode)
                .decoder(BlackwhipStatePacket::decode)
                .consumerMainThread(BlackwhipStatePacket::handle)
                .add();

        // Blackwhip persistent multi-tethers (server -> clients)
        CHANNEL.messageBuilder(BlackwhipTethersPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BlackwhipTethersPacket::encode)
                .decoder(BlackwhipTethersPacket::decode)
                .consumerMainThread(BlackwhipTethersPacket::handle)
                .add();
    }
}


