package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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

public class LongArmsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final String LONG_ARMS_GENE_ID = "bandits_quirk_lib:gene.long_arms";
    private static final String LONG_ARMS_GENE_ID_ALT = "bandits_quirk_lib:long_arms";

    public LongArmsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       AbstractClientPlayer player,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTicks,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {
        int quality = getGeneQualityClient(player, LONG_ARMS_GENE_ID);
        if (quality < 0) {
            quality = getGeneQualityClient(player, LONG_ARMS_GENE_ID_ALT);
        }
        boolean debugForce = player.isCrouching();
        if (quality < 0 && !debugForce) {
            return;
        }

        float factor = Math.max(0F, Math.min(1F, (quality < 0 ? 100 : quality) / 100F));
        float yScale = debugForce ? 2.0F : (1.6F + (factor * 0.9F));

        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        ModelPart rightSleeve = model.rightSleeve;
        ModelPart leftSleeve = model.leftSleeve;

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));

        // Ensure vanilla arms are hidden; draw our scaled arms regardless of their visibility
        if (rightArm != null) {
            poseStack.pushPose();
            poseStack.scale(1.0F, yScale, 1.0F);
            rightArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        if (leftArm != null) {
            poseStack.pushPose();
            poseStack.scale(1.0F, yScale, 1.0F);
            leftArm.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        if (rightSleeve != null) {
            poseStack.pushPose();
            poseStack.scale(1.0F, yScale, 1.0F);
            rightSleeve.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        if (leftSleeve != null) {
            poseStack.pushPose();
            poseStack.scale(1.0F, yScale, 1.0F);
            leftSleeve.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
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


