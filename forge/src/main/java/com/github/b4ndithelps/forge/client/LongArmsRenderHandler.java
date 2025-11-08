package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LongArmsRenderHandler {

    private static final Map<Player, ArmScaleState> PREV_SCALES = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        boolean active = LongArmsController.shouldRenderLongArms(player);
        if (!active) {
            return;
        }

        HumanoidModel<?> model = event.getRenderer().getModel();
        if (model == null) return;

        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        ModelPart rightSleeve = (model instanceof PlayerModel<?> pm) ? pm.rightSleeve : null;
        ModelPart leftSleeve = (model instanceof PlayerModel<?> pm2) ? pm2.leftSleeve : null;

        ArmScaleState prev = new ArmScaleState();
        prev.rightArmVisible = rightArm == null || rightArm.visible;
        prev.leftArmVisible = leftArm == null || leftArm.visible;
        prev.rightSleeveVisible = rightSleeve == null || rightSleeve.visible;
        prev.leftSleeveVisible = leftSleeve == null || leftSleeve.visible;
        PREV_SCALES.put(player, prev);

        if (rightArm != null) rightArm.visible = false;
        if (leftArm != null) leftArm.visible = false;
        if (rightSleeve != null) rightSleeve.visible = false;
        if (leftSleeve != null) leftSleeve.visible = false;

        // Move vanilla arms offscreen to prevent any other layers from drawing them
        if (model instanceof PlayerModel<?> pmAll) {
            LongArmsController.saveAndMoveArmsOffscreen(player, pmAll);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        ArmScaleState prev = PREV_SCALES.remove(player);
        if (prev == null) return;

        HumanoidModel<?> model = event.getRenderer().getModel();
        if (model == null) return;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        if (rightArm != null) rightArm.visible = prev.rightArmVisible;
        if (leftArm != null) leftArm.visible = prev.leftArmVisible;
        if (model instanceof PlayerModel<?> pm) {
            if (pm.rightSleeve != null) pm.rightSleeve.visible = prev.rightSleeveVisible;
            if (pm.leftSleeve != null) pm.leftSleeve.visible = prev.leftSleeveVisible;
            // restore positions moved off-screen
            LongArmsController.restoreArmPositions(player, pm);
            LongArmsController.clearArmPositions(player);
        }
    }

    private static class ArmScaleState {
        boolean rightArmVisible;
        boolean leftArmVisible;
        boolean rightSleeveVisible;
        boolean leftSleeveVisible;

        ArmScaleState() {}
    }
}