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

        CHANNEL.messageBuilder(ConsoleSyncS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConsoleSyncS2CPacket::encode)
                .decoder(ConsoleSyncS2CPacket::decode)
                .consumerMainThread(ConsoleSyncS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(ConsoleHistorySyncS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConsoleHistorySyncS2CPacket::encode)
                .decoder(ConsoleHistorySyncS2CPacket::decode)
                .consumerMainThread(ConsoleHistorySyncS2CPacket::handle)
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

        CHANNEL.messageBuilder(ConsoleCommandC2SPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConsoleCommandC2SPacket::encode)
                .decoder(ConsoleCommandC2SPacket::decode)
                .consumerMainThread(ConsoleCommandC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(ProgramInputC2SPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProgramInputC2SPacket::encode)
                .decoder(ProgramInputC2SPacket::decode)
                .consumerMainThread(ProgramInputC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SwitchProgramC2SPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SwitchProgramC2SPacket::encode)
                .decoder(SwitchProgramC2SPacket::decode)
                .consumerMainThread(SwitchProgramC2SPacket::handle)
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

        CHANNEL.messageBuilder(ProgramScreenSyncS2CPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProgramScreenSyncS2CPacket::encode)
                .decoder(ProgramScreenSyncS2CPacket::decode)
                .consumerMainThread(ProgramScreenSyncS2CPacket::handle)
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
    }
}


