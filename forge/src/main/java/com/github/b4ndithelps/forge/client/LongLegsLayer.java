package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

public class LongLegsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final String LONG_LEGS_GENE_ID = "bandits_quirk_lib:gene.long_legs";
    private static final String LONG_LEGS_GENE_ID_ALT = "bandits_quirk_lib:long_legs";

    public LongLegsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!LongLegsController.shouldRenderLongLegs(player)) return;

        int quality = getGeneQualityClient(player, LONG_LEGS_GENE_ID);
        if (quality < 0) quality = getGeneQualityClient(player, LONG_LEGS_GENE_ID_ALT);
        boolean debugForce = player.isCrouching();

        float factor = Math.max(0F, Math.min(1F, (quality < 0 ? 100 : quality) / 100F));
        float eased = factor * factor; // quadratic ease-in
        float baseScale = 1.05F; // subtle at low quality
        float maxScale = 1.80F;  // tuned to avoid world clipping
        float yScale = debugForce ? 1.8F : (baseScale + (maxScale - baseScale) * eased);

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        ModelPart rightLeg = model.rightLeg;
        ModelPart leftLeg = model.leftLeg;
        ModelPart rightPants = model.rightPants;
        ModelPart leftPants = model.leftPants;

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer vcSolid = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));

        if (rightLeg != null) {
            boolean prev = rightLeg.visible;
            rightLeg.visible = true;
            poseStack.pushPose();
            float pivotYPx = rightLeg.y;
            float offsetY = -(pivotYPx * (yScale - 1.0F)) / 16.0F;
            poseStack.translate(0.0F, offsetY, 0.0F);
            poseStack.scale(1.0F, yScale, 1.0F);
            rightLeg.render(poseStack, vcSolid, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            rightLeg.visible = prev;
        }
        if (leftLeg != null) {
            boolean prev = leftLeg.visible;
            leftLeg.visible = true;
            poseStack.pushPose();
            float pivotYPx = leftLeg.y;
            float offsetY = -(pivotYPx * (yScale - 1.0F)) / 16.0F;
            poseStack.translate(0.0F, offsetY, 0.0F);
            poseStack.scale(1.0F, yScale, 1.0F);
            leftLeg.render(poseStack, vcSolid, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            leftLeg.visible = prev;
        }
        if (rightPants != null) {
            boolean prev = rightPants.visible;
            rightPants.visible = true;
            poseStack.pushPose();
            float pivotYPx = rightPants.y;
            float offsetY = -(pivotYPx * (yScale - 1.0F)) / 16.0F;
            poseStack.translate(0.0F, offsetY, 0.0F);
            poseStack.scale(1.0F, yScale, 1.0F);
            rightPants.render(poseStack, vcSolid, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            rightPants.visible = prev;
        }
        if (leftPants != null) {
            boolean prev = leftPants.visible;
            leftPants.visible = true;
            poseStack.pushPose();
            float pivotYPx = leftPants.y;
            float offsetY = -(pivotYPx * (yScale - 1.0F)) / 16.0F;
            poseStack.translate(0.0F, offsetY, 0.0F);
            poseStack.scale(1.0F, yScale, 1.0F);
            leftPants.render(poseStack, vcSolid, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            leftPants.visible = prev;
        }
    }

    private static int getGeneQualityClient(AbstractClientPlayer player, String geneId) {
        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id"))) {
                return gene.contains("quality", 3) ? gene.getInt("quality") : 100;
            }
        }
        return -1;
    }
}