package com.github.b4ndithelps.forge.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleHelper {

    public static void sendTitle(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        if (subtitle != null) player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void clear(ServerPlayer player, boolean resetTimes) {
        player.connection.send(new ClientboundClearTitlesPacket(resetTimes));
    }

    public static void sendSubtitle(ServerPlayer player, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    public static void sendChargingPercentage(ServerPlayer player, String label, double percentage, ChatFormatting labelColor, ChatFormatting percentColor, String statusLabel) {
        MutableComponent subtitle = Component.literal(label + ": ")
                .withStyle(labelColor)
                .append(Component.literal(String.format("%.0f%% (%s)", percentage, statusLabel)).withStyle(percentColor));
        sendSubtitle(player, subtitle, 2, 10, 2);
    }
}


