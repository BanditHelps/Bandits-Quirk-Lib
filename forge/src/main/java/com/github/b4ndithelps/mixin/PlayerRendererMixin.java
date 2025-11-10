package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.client.LongArmsController;
import com.github.b4ndithelps.forge.client.LongLegsController;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void bql$markHideArmsStart(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        boolean hideArms = LongArmsController.shouldRenderLongArms(player);
        LongArmsController.setHideVanillaArms(hideArms);
        boolean hideLegs = LongLegsController.shouldRenderLongLegs(player);
        LongLegsController.setHideVanillaLegs(hideLegs);

        // Proactively move vanilla legs offscreen so any layer that forces visibility won't draw them
        if (hideLegs) {
            PlayerRenderer self = (PlayerRenderer)(Object)this;
            PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>) self.getModel();
            if (model != null) {
                if (model.rightLeg != null) model.rightLeg.visible = false;
                if (model.leftLeg != null) model.leftLeg.visible = false;
                if (model.rightPants != null) model.rightPants.visible = false;
                if (model.leftPants != null) model.leftPants.visible = false;
                LongLegsController.saveAndMoveLegsOffscreen(player, model);
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void bql$markHideArmsEnd(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        LongArmsController.setHideVanillaArms(false);
        LongLegsController.setHideVanillaLegs(false);

        // Restore any leg part positions moved offscreen
        PlayerRenderer self = (PlayerRenderer)(Object)this;
        PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>) self.getModel();
        if (model != null) {
            LongLegsController.restoreLegPositions(player, model);
            LongLegsController.clearLegPositions(player);
        }
    }

    // After vanilla sets visibilities in setModelProperties, re-hide arms/sleeves if needed
    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void bql$forceHideArmsAfterProperties(AbstractClientPlayer player, CallbackInfo ci) {
        PlayerRenderer self = (PlayerRenderer)(Object)this;
        PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>) self.getModel();
        if (LongArmsController.shouldRenderLongArms(player)) {
            if (model.rightArm != null) model.rightArm.visible = false;
            if (model.leftArm != null) model.leftArm.visible = false;
            if (model.rightSleeve != null) model.rightSleeve.visible = false;
            if (model.leftSleeve != null) model.leftSleeve.visible = false;
        }
        if (LongLegsController.shouldRenderLongLegs(player)) {
            if (model.rightLeg != null) model.rightLeg.visible = false;
            if (model.leftLeg != null) model.leftLeg.visible = false;
            if (model.rightPants != null) model.rightPants.visible = false;
            if (model.leftPants != null) model.leftPants.visible = false;
        }
    }

    // Cancel vanilla first-person arm rendering; long-arms layer will handle custom draw
    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void bql$cancelRightHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (LongArmsController.shouldRenderLongArms(player)) ci.cancel();
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void bql$cancelLeftHand(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, AbstractClientPlayer player, CallbackInfo ci) {
        if (LongArmsController.shouldRenderLongArms(player)) ci.cancel();
    }
}