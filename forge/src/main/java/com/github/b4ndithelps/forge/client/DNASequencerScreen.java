package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.DNASequencerMenu;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ConsoleCommandC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraft.util.Mth;

@SuppressWarnings("removal")
public class DNASequencerScreen extends AbstractContainerScreen<DNASequencerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/dna_sequencer.png");
    private EditBox input;
    private String consoleText = "";
    private int consoleScrollPixels = 0;
    private boolean stickToBottom = true;

    public DNASequencerScreen(DNASequencerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int textAreaX = this.leftPos + 8;
        int textAreaY = this.topPos + 16;
        int textAreaWidth = this.imageWidth - 16;
        int inputHeight = 14;
        int gapBetween = 8;
        int bottomMargin = 8;
        int usedTop = textAreaY - this.topPos;
        int textAreaHeight = this.imageHeight - usedTop - gapBetween - inputHeight - bottomMargin;
        if (textAreaHeight < this.font.lineHeight) {
            textAreaHeight = this.font.lineHeight;
        }
        var wrapped = this.font.split(Component.literal(this.consoleText), textAreaWidth);
        int maxLines = Math.max(1, textAreaHeight / this.font.lineHeight);
        int contentHeight = wrapped.size() * this.font.lineHeight;
        int maxScroll = Math.max(0, contentHeight - textAreaHeight);
        if (this.stickToBottom) {
            this.consoleScrollPixels = maxScroll;
        }
        this.consoleScrollPixels = Mth.clamp(this.consoleScrollPixels, 0, maxScroll);

        int startPixel = this.consoleScrollPixels;
        int firstLineIndex = startPixel / this.font.lineHeight;
        int yOffset = -(startPixel % this.font.lineHeight);
        int y = textAreaY + yOffset;
        for (int i = firstLineIndex; i < wrapped.size(); i++) {
            if (y + this.font.lineHeight > textAreaY && y < textAreaY + textAreaHeight) {
                graphics.drawString(this.font, wrapped.get(i), textAreaX, y, 0x00FF00, false);
            }
            y += this.font.lineHeight;
            if (y >= textAreaY + textAreaHeight) break;
        }

        // Optional simple scrollbar (right-side, 2px wide)
        if (maxScroll > 0) {
            int barTrackX = textAreaX + textAreaWidth - 2;
            int barTrackY = textAreaY;
            int barTrackH = textAreaHeight;
            int barH = Math.max(8, (int)((float)textAreaHeight * ((float)textAreaHeight / (float)contentHeight)));
            int barMaxY = barTrackH - barH;
            int barY = barTrackY + (int)((float)this.consoleScrollPixels / (float)maxScroll * barMaxY);
            graphics.fill(barTrackX, barTrackY, barTrackX + 1, barTrackY + barTrackH, 0x55000000);
            graphics.fill(barTrackX, barY, barTrackX + 2, barY + barH, 0x88FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // This is the title of the page. Don't need it in the computer screen
        //        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.input = new EditBox(this.font, this.leftPos + 8, this.topPos + this.imageHeight - 22, this.imageWidth - 16, 14, Component.literal("console_input"));
        this.input.setMaxLength(32767);
        this.input.setBordered(true);
        this.input.setFocused(true);
        this.addRenderableWidget(this.input);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Consume inventory toggle to prevent closing while typing
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        // Enter submits
        if (keyCode == 257 || keyCode == 335) {
            submitCommand();
            return true;
        }
        // Let the input consume keys first
        if (this.input.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.input.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int textAreaX = this.leftPos + 8;
        int textAreaY = this.topPos + 16;
        int textAreaWidth = this.imageWidth - 16;
        int inputHeight = 14;
        int gapBetween = 8;
        int bottomMargin = 8;
        int usedTop = textAreaY - this.topPos;
        int textAreaHeight = this.imageHeight - usedTop - gapBetween - inputHeight - bottomMargin;
        if (textAreaHeight < this.font.lineHeight) {
            textAreaHeight = this.font.lineHeight;
        }

        boolean overTextArea = mouseX >= textAreaX && mouseX <= textAreaX + textAreaWidth && mouseY >= textAreaY && mouseY <= textAreaY + textAreaHeight;
        if (overTextArea) {
            var wrapped = this.font.split(Component.literal(this.consoleText), textAreaWidth);
            int contentHeight = wrapped.size() * this.font.lineHeight;
            int maxScroll = Math.max(0, contentHeight - textAreaHeight);
            if (maxScroll > 0) {
                int step = this.font.lineHeight * 3;
                this.consoleScrollPixels = Mth.clamp(this.consoleScrollPixels - (int)(delta * step), 0, maxScroll);
                this.stickToBottom = this.consoleScrollPixels >= maxScroll;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Pull text from BE each tick via update tag; in a full implementation, the S2C packet already keeps it updated
        var pos = this.menu.getBlockPos();
        var be = this.minecraft.level.getBlockEntity(pos);
        if (be instanceof com.github.b4ndithelps.forge.blocks.DNASequencerBlockEntity sequencer) {
            this.consoleText = sequencer.getConsoleText();
        }
        this.input.tick();
    }

    private void submitCommand() {
        String cmd = this.input.getValue();
        if (cmd == null || cmd.isBlank()) return;
        BlockPos pos = this.menu.getBlockPos();
        BQLNetwork.CHANNEL.sendToServer(new ConsoleCommandC2SPacket(pos, cmd));
        this.input.setValue("");
    }

    
}


