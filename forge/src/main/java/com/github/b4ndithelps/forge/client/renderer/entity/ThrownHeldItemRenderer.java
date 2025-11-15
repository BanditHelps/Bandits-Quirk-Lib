package com.github.b4ndithelps.forge.client.renderer.entity;

import com.github.b4ndithelps.forge.entities.ThrownHeldItemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

@SuppressWarnings("removal")
public class ThrownHeldItemRenderer extends EntityRenderer<ThrownHeldItemEntity> {

	public ThrownHeldItemRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ThrownHeldItemEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

		if (entity.getItem().isEmpty()) return;

		poseStack.pushPose();
		// Simple render of the item; leave orientation default
		Minecraft.getInstance().getItemRenderer().renderStatic(
				entity.getItem(),
				ItemDisplayContext.GROUND,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				poseStack,
				buffer,
				entity.level(),
				0
		);
		poseStack.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(ThrownHeldItemEntity entity) {
		return new ResourceLocation("minecraft", "textures/misc/white.png");
	}
}


