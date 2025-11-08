package com.github.b4ndithelps.forge.client.renderer.entity;

import com.github.b4ndithelps.forge.entities.BlockStackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("removal")
public class BlockStackEntityRenderer extends EntityRenderer<BlockStackEntity> {

	public BlockStackEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(BlockStackEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

		poseStack.pushPose();
		// Render bottom, middle, top as full blocks stacked; align bottom on entity Y
		BlockState bottom = entity.getBottom();
		BlockState middle = entity.getMiddle();
		BlockState top = entity.getTop();

		// Slightly raise to avoid z-fighting with the ground when hovering
		poseStack.translate(-0.5, 0.0, -0.5);
		dispatcher.renderSingleBlock(bottom, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.translate(0.0, 1.0, 0.0);
		dispatcher.renderSingleBlock(middle, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.translate(0.0, 1.0, 0.0);
		dispatcher.renderSingleBlock(top, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

		poseStack.popPose();

		// Optional: render outline when targeted
		if (this.entityRenderDispatcher.shouldRenderHitBoxes()) {
			LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ()), 0f, 0.8f, 1f, 1f);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(BlockStackEntity entity) {
		// Block models provide their own textures
		return new ResourceLocation("minecraft", "textures/misc/white.png");
	}
}