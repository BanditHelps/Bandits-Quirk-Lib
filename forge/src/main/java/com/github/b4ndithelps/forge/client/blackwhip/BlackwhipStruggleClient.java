package com.github.b4ndithelps.forge.client.blackwhip;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipStruggleTapC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side state and HUD for Blackwhip struggle.
 */
@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackwhipStruggleClient {

	private static boolean active = false;
	private static int taps = 0;
	private static int threshold = 0;

	private BlackwhipStruggleClient() {}

	public static boolean isActive() {
		return active;
	}

	public static void applyStatus(boolean isActive, int newTaps, int newThreshold) {
		active = isActive;
		taps = Math.max(0, newTaps);
		threshold = Math.max(1, newThreshold);
	}

	public static void sendTapIfActiveOnJumpEdge(boolean jumpDown, boolean lastJumpDown) {
		if (!active) return;
		if (jumpDown && !lastJumpDown) {
			BQLNetwork.CHANNEL.sendToServer(new BlackwhipStruggleTapC2SPacket());
		}
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!active) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		GuiGraphics g = event.getGuiGraphics();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		int barW = Math.min(180, Math.max(120, w / 4));
		int barH = 10;
		int x = (w - barW) / 2;
		int y = h - 40;

		float progress = threshold <= 0 ? 0f : Math.min(1f, (float) taps / (float) threshold);

		// Background
		int bg = 0xAA0E1117;
		g.fill(x - 2, y - 18, x + barW + 2, y + barH + 2, bg);

		// Border
		int border = 0xFF2A3140;
		g.fill(x - 2, y - 18, x + barW + 2, y - 17, border);
		g.fill(x - 2, y + barH + 1, x + barW + 2, y + barH + 2, border);
		g.fill(x - 2, y - 18, x - 1, y + barH + 2, border);
		g.fill(x + barW + 1, y - 18, x + barW + 2, y + barH + 2, border);

		// Title
		Font font = mc.font;
		Component title = Component.literal("Break free! Press Jump");
		int titleX = x + (barW - font.width(title)) / 2;
		int titleY = y - 14;
		g.drawString(font, title, Math.max(x, titleX), titleY, 0xFFBFD7FF, false);

		// Bar background
		int barBg = 0xFF202531;
		g.fill(x, y, x + barW, y + barH, barBg);
		// Bar fill
		int fillW = Math.round(barW * progress);
		int barFill = 0xFF5EC26A; // green
		g.fill(x, y, x + fillW, y + barH, barFill);

		// Progress text
		String pct = Math.round(progress * 100f) + "%";
		int px = x + (barW - font.width(pct)) / 2;
		int py = y + (barH - font.lineHeight) / 2;
		g.drawString(font, pct, px, py, 0xFF0D0F14, false);
	}
}