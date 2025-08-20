package com.github.b4ndithelps.forge.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Used to help send action bars to the player without using any actual commands.
 */
public class ActionBarHelper {

    /**
     * Sends a single text message to the player's action bar.
     */
    public static void sendActionBar(ServerPlayer player, String message) {
        Component component = Component.literal(message);
        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, false);
        player.connection.send(packet);
    }

    /**
     * Sends a pre-formatted message with colors
     */
    public static void sendActionBar(ServerPlayer player, Component component) {
        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(component, true);
        player.connection.send(packet);
    }

    /**
     * Send a colored percentage display to the player
     */
    public static void sendPercentageDisplay(ServerPlayer player, String label, double percentage, ChatFormatting labelColor, ChatFormatting percentColor, String statusLabel) {
        MutableComponent message = Component.literal(label)
                .withStyle(labelColor)
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format("%.0f%% (%s)", percentage, statusLabel)).withStyle(percentColor));

        sendActionBar(player, message);
    }


}
