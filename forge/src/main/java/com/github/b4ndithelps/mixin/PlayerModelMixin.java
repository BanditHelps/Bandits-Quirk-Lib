package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.client.LongArmsController;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {

    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightSleeve;
    @Shadow public ModelPart leftSleeve;

    @Inject(method = "bodyParts", at = @At("RETURN"), cancellable = true)
    private void bql$filterBodyParts(CallbackInfoReturnable<Iterable<ModelPart>> cir) {
        if (!LongArmsController.isHideVanillaArms()) return;
        Iterable<ModelPart> orig = cir.getReturnValue();
        List<ModelPart> filtered = new ArrayList<>();
        for (ModelPart part : orig) {
            if (part == rightArm || part == leftArm || part == rightSleeve || part == leftSleeve) {
                continue;
            }
            filtered.add(part);
        }
        cir.setReturnValue(filtered);
    }

    @Inject(method = "setAllVisible", at = @At("RETURN"))
    private void bql$hideArmsAfterSetAllVisible(boolean visible, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!LongArmsController.isHideVanillaArms()) return;
        if (rightArm != null) rightArm.visible = false;
        if (leftArm != null) leftArm.visible = false;
        if (rightSleeve != null) rightSleeve.visible = false;
        if (leftSleeve != null) leftSleeve.visible = false;
    }
}


