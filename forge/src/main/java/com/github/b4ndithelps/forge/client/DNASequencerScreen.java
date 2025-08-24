package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.DNASequencerMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DNASequencerScreen extends AbstractContainerScreen<DNASequencerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/dna_sequencer.png");

    public DNASequencerScreen(DNASequencerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int progress = this.menu.getCarried().getCount(); // placeholder
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
//        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);
//        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}


