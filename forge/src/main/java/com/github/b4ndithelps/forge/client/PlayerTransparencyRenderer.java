package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.utils.TransparencyManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, value = Dist.CLIENT)
public class PlayerTransparencyRenderer {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        // Check if the entity being rendered is a player
        if (event.getEntity() instanceof Player player) {
            // Enable blending (required for transparency)
            RenderSystem.enableBlend();

            // Set the blend function for proper alpha blending
            // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA is the standard formula
            RenderSystem.defaultBlendFunc();

            float PLAYER_ALPHA = TransparencyManager.getTransparency(player.getUUID());




            // Set the color with alpha channel
            // Parameters: Red, Green, Blue, Alpha (all 0.0F to 1.0F)
            // RGB at 1.0F means no color tint, just transparency
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, PLAYER_ALPHA);
        }
    }

    /**
     * Called AFTER the player entity is rendered.
     * This resets the rendering state to prevent affecting other entities.
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        // Check if the entity being rendered is a player
        if (event.getEntity() instanceof Player) {
            // Reset the alpha back to fully opaque
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Disable blending to restore normal rendering
            RenderSystem.disableBlend();
        }
    }

//    @SubscribeEvent
//    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
//        float alpha = TransparencyManager.getTransparency(event.getEntity().getUUID());
//
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
//
//        /*
//         * VERY IMPORTANT:
//         * Prevent Minecraft from resetting shader color between render layers.
//         * This makes armor, hand items, elytra, cape, etc. inherit the alpha.
//         */
//        event.getRenderer().layers.forEach(layer -> layer.setRenderAlpha(alpha));
//    }
//
//    @SubscribeEvent
//    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
//        // Reset everything
//        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        RenderSystem.disableBlend();
//    }

}