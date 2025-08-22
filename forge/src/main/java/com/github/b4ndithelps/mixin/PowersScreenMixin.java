package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
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

        // Compute position to the right of the localized title "gui.palladium.powers"
        Component title = Component.translatable("gui.palladium.powers");
        int titleWidth = minecraft.font.width(title);
        int iconX = offsetX + 8 + titleWidth + 6;
        int iconY = offsetY + 2; // align visually with title row

        // Draw command block item icon (16x16)
        guiGraphics.renderItem(new ItemStack(Blocks.COMMAND_BLOCK), iconX, iconY);

        // Draw upgrade points text next to the icon
        int points = StaminaHelper.getUpgradePoints(minecraft.player);
        Component pointsText = Component.literal(String.valueOf(points));
        int textX = iconX + 18;
        int textY = offsetY + 6; // match title baseline
        guiGraphics.drawString(minecraft.font, pointsText, textX, textY, 0x404040, false);
    }
}


