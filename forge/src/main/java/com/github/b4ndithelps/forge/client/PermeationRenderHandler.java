package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.client.model.animation.AnimationUtil;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PermeationRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        // Read synced body status values (server-authoritative)
        int state = BodyStatusHelper.getCustomStatus(player, "chest", "permeation_state");

        if (state <= 0) {
            return; // not permeating
        }

        float depthPixels = BodyStatusHelper.getCustomFloat(player, "right_leg", "permeation_depth");
        if (depthPixels == 0.0f) {
            return;
        }

        // Convert pixels (16 px per block) to world units. Negative depth sinks downward.
        float yOffsetBlocks = depthPixels / 16.0f;

        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(0.0, yOffsetBlocks, 0.0);
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        float depthPixels = BodyStatusHelper.getCustomFloat(player, "right_leg", "permeation_depth");
        //int state = BodyStatusHelper.getCustomStatus(player, "chest", "permeation_state");
        // -34 is the lowest the sink will go
        if (depthPixels <= -34) {
            event.setContent(Component.empty());
        }
    }
}


