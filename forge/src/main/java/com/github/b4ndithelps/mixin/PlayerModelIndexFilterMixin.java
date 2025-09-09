package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.client.LongArmsController;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerModel.class)
public abstract class PlayerModelIndexFilterMixin<T extends LivingEntity> {

    // Avoid shadowed methods/fields due to mapping variance. We'll use a position heuristic instead.

    @Inject(method = "bodyParts", at = @At("RETURN"), cancellable = true)
    private void bql$filterArmsByOrder(CallbackInfoReturnable<Iterable<ModelPart>> cir) {
        if (!LongArmsController.isHideVanillaArms()) return;
        Iterable<ModelPart> orig = cir.getReturnValue();
        List<ModelPart> filtered = new ArrayList<>();
        for (ModelPart part : orig) {
            float ax = Math.abs(part.x);
            float ay = part.y;
            boolean looksLikeArmOrSleeve = (LongArmsController.isHideVanillaArms() && ax >= 4.0F && ay <= 6.0F);
            boolean looksLikeLegOrPant = (com.github.b4ndithelps.forge.client.LongLegsController.isHideVanillaLegs() && ay >= 10.0F && ax >= 1.5F && ax <= 2.3F);
            if (looksLikeArmOrSleeve || looksLikeLegOrPant) continue;
            filtered.add(part);
        }
        cir.setReturnValue(filtered);
    }
}


