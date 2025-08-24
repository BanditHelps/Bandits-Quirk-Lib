package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.DNASequencerMenu;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ConsoleCommandC2SPacket;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;

public class DNASequencerScreen extends AbstractContainerScreen<DNASequencerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/dna_sequencer.png");
    private EditBox input;
    private String consoleText = "";

    public DNASequencerScreen(DNASequencerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Draw console text area (simple text dump for now)
        int x = this.leftPos + 8;
        int y = this.topPos + 16;
        var lines = consoleText.split("\\n");
        int maxLines = 8;
        int start = Math.max(0, lines.length - maxLines);
        for (int i = start; i < lines.length; i++) {
            graphics.drawString(this.font, lines[i], x, y, 0x00FF00, false);
            y += 10;
        }
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        // Player inventory title intentionally not drawn (console-only UI)
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


