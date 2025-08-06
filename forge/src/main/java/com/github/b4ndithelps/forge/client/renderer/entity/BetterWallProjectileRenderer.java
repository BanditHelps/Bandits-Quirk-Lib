package com.github.b4ndithelps.forge.client.renderer.entity;

import com.github.b4ndithelps.forge.entities.BetterWallProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BetterWallProjectileRenderer extends EntityRenderer<BetterWallProjectileEntity> {

    public BetterWallProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BetterWallProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BetterWallProjectileEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
