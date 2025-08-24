package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.GeneSequencerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings("removal")
public class GeneSequencerScreen extends AbstractContainerScreen<GeneSequencerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/gene_sequencer.png");

    public GeneSequencerScreen(GeneSequencerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}


