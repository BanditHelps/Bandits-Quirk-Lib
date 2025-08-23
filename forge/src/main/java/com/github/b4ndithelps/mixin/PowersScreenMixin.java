package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.threetag.palladium.client.screen.power.PowersScreen;

/**
 * Injects a small command-block icon and the player's stamina upgrade points
 * into the Powers screen title bar.
 */
@Mixin(value = PowersScreen.class, remap = false)
public abstract class PowersScreenMixin {

    @Inject(method = "renderWindow", at = @At("TAIL"))
    private void bql$renderUpgradePoints(GuiGraphics guiGraphics, int offsetX, int offsetY, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        // Compute right-aligned position near the window's right edge
        int rightEdge = offsetX + PowersScreen.WINDOW_WIDTH - 8; // 8px inner margin
        int points = StaminaHelper.getUpgradePoints(minecraft.player);
        Component pointsText = Component.literal(String.valueOf(points));
        int textWidth = minecraft.font.width(pointsText);

        // Layout: [icon][gap][text] aligned to rightEdge
        int iconSize = 12; // target render size (scaled from 16)
        int gap = 4;
        int textX = rightEdge - textWidth; // right-align text
        int iconX = textX - gap - iconSize;
        int iconY = offsetY + 4; // align with title row

        // Draw command block item scaled down from 16x16 to iconSize
        float scale = iconSize / 16.0f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX, iconY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(new ItemStack(Blocks.COMMAND_BLOCK), 0, 0);
        guiGraphics.pose().popPose();

        // Draw upgrade points text next to the icon (right-aligned overall)
        int textY = offsetY + 6; // match title baseline
        guiGraphics.drawString(minecraft.font, pointsText, textX, textY, 0x404040, false);
    }
}


