package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.network.NoShadowTagPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {

    @Inject(method = "renderShadow",
            at = @At("HEAD"), cancellable = true)
    private static <E extends Entity> void cancelShadowRender(PoseStack poseStack, MultiBufferSource buffer, Entity entity, float weight, float partialTicks, LevelReader level, float size, CallbackInfo ci) {
        if (!shouldRenderShadowForEntity(entity)) {
            ci.cancel();
        }
    }

    private static boolean shouldRenderShadowForEntity(Entity entity) {
        // Client-side check using synced flag; fallback to local tags for singleplayer integrated server mirroring
        return !NoShadowTagPacket.isNoShadowEntity(entity) && !entity.getTags().contains("Bql.NoShadow");
    }
}
