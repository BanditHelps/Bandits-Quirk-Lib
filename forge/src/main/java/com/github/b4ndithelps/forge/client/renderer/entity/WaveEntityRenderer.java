package com.github.b4ndithelps.forge.client.renderer.entity;

import com.github.b4ndithelps.forge.entities.WaveProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class WaveEntityRenderer extends EntityRenderer<WaveProjectileEntity> {

    public WaveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaveProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Apply entity rotation to match the direction it's facing
        // The wave should face the same direction as the player who spawned it
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-entity.getXRot()));

        // Get wave dimensions
        float width = entity.getWaveWidth();
        float height = entity.getWaveHeight();

        // Create a translucent blue effect for the wave
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));

        // Render a translucent box representing the wave
        renderWaveBox(poseStack, vertexConsumer, width, height, packedLight);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private void renderWaveBox(PoseStack poseStack, VertexConsumer vertexConsumer,
                               float width, float height, int packedLight) {

        Matrix4f matrix4f = poseStack.last().pose();

        // Define the box vertices based on width and height
        float halfWidth = width / 2.0F;
        float depth = 0.5F; // Thin depth for wave effect

        // Colors (RGBA) - translucent blue
        int red = 100;
        int green = 150;
        int blue = 255;
        int alpha = 100; // Translucent

        // Front face
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, -depth, 0, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, -depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, height, -depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, -depth, 0, 1, red, green, blue, alpha, packedLight);

        // Back face
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, depth, 0, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, height, depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, depth, 0, 0, red, green, blue, alpha, packedLight);

        // Left face
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, depth, 0, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, -depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, -depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, depth, 0, 1, red, green, blue, alpha, packedLight);

        // Right face
        addVertex(matrix4f, vertexConsumer, halfWidth, height, depth, 0, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, height, -depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, -depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, depth, 0, 0, red, green, blue, alpha, packedLight);

        // Top face
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, -depth, 0, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, height, -depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, height, depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, height, depth, 0, 1, red, green, blue, alpha, packedLight);

        // Bottom face
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, depth, 0, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, depth, 1, 1, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, halfWidth, 0, -depth, 1, 0, red, green, blue, alpha, packedLight);
        addVertex(matrix4f, vertexConsumer, -halfWidth, 0, -depth, 0, 0, red, green, blue, alpha, packedLight);
    }

    private void addVertex(Matrix4f matrix, VertexConsumer vertexConsumer,
                           float x, float y, float z, float u, float v,
                           int red, int green, int blue, int alpha, int packedLight) {
        vertexConsumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(0, 10)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(WaveProjectileEntity entity) {
        // You can create a custom texture or use a simple white texture
        // For now, using the white texture from vanilla
        return new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    }
}
