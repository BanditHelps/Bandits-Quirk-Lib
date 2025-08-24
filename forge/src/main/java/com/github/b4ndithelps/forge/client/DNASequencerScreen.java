package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.DNASequencerMenu;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ConsoleCommandC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DNASequencerScreen extends AbstractContainerScreen<DNASequencerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/dna_sequencer.png");
    private EditBox consoleInput;

    public DNASequencerScreen(DNASequencerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Draw console window text (simple, non-scrollable) over the background
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var be = level.getBlockEntity(this.menu.getBlockPos());
            if (be instanceof com.github.b4ndithelps.forge.blocks.DNASequencerBlockEntity sequencer) {
                String text = sequencer.getConsoleText();
                String[] lines = text.isEmpty() ? new String[0] : text.split("\\n");
                int maxLines = 9;
                int start = Math.max(0, lines.length - maxLines);
                int x = this.leftPos + 38; // left edge of console area
                int y = this.topPos + 14;  // top edge of console area
                for (int i = start; i < lines.length; i++) {
                    graphics.drawString(this.font, lines[i], x, y + (i - start) * 9, 0x00FF00, false);
                }
            }
        }
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw titles if desired on your GUI texture
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + 38;
        int y = this.topPos + 100;
        this.consoleInput = new EditBox(this.font, x, y, 100, 12, Component.literal(""));
        this.consoleInput.setMaxLength(32767);
        this.addRenderableWidget(this.consoleInput);
        setInitialFocus(this.consoleInput);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.consoleInput != null && this.consoleInput.isFocused()) {
            // Consume inventory key to prevent closing while typing
            KeyMapping invKey = this.minecraft.options.keyInventory;
            if (invKey != null && invKey.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                String cmd = this.consoleInput.getValue().trim();
                if (!cmd.isEmpty()) {
                    BQLNetwork.CHANNEL.sendToServer(new ConsoleCommandC2SPacket(this.menu.getBlockPos(), cmd));
                    this.consoleInput.setValue("");
                }
                return true;
            }
            if (this.consoleInput.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.consoleInput != null && this.consoleInput.isFocused()) {
            if (this.consoleInput.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.consoleInput != null) {
            this.setFocused(this.consoleInput);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}


