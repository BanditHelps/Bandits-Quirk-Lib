package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A simple class that renders a black screen overlay and fades it out.
 * Register this class to the Forge event bus from your client setup.
 */
@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, value = Dist.CLIENT)
public class ScreenFadeOverlay {

    private static boolean blackoutActive = false;
    private static float alpha = 0f;        // current transparency (0 = invisible, 1 = black)
    private static float fadeSpeed = 0.03f; // how fast to fade each tick

    /**
     * Call this method when you want the fade-out effect to begin.
     * For example, trigger this from a network packet sent from the server.
     */
    public static void startFadeOut() {
        blackoutActive = true;
        alpha = 0.55f; // start fully black
    }

    /**
     * Ticks the fade every client tick.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.ClientTickEvent.Phase.END) return;

        if (blackoutActive) {
            alpha -= fadeSpeed;
            if (alpha <= 0f) {
                alpha = 0f;
                blackoutActive = false; // stop once finished
            }
        }
    }

    /**
     * Renders the black overlay each frame.
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // We want to render after everything else, so Post is fine.
        if (!blackoutActive && alpha <= 0f) return;
//        Player player = Minecraft.getInstance().player;
        Minecraft mc = Minecraft.getInstance();
//        if (player == null || Minecraft.getInstance().level == null) return;
//        if (mc.player != null) {
//            mc.player.playSound(
//                    SoundEvents.EXPERIENCE_ORB_PICKUP, // example sound
//                    1.0F, // volume
//                    1.0F  // pitch
//            );
//        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(0, height, 0).color(0f, 0f, 0f, alpha).endVertex();
        buffer.vertex(width, height, 0).color(0f, 0f, 0f, alpha).endVertex();
        buffer.vertex(width, 0, 0).color(0f, 0f, 0f, alpha).endVertex();
        buffer.vertex(0, 0, 0).color(0f, 0f, 0f, alpha).endVertex();
        tessellator.end();

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }
}