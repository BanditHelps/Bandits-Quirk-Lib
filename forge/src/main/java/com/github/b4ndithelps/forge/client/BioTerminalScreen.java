package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.BioTerminalMenu;
import com.github.b4ndithelps.forge.blocks.BioTerminalBlockEntity;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.ConsoleCommandC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

@SuppressWarnings("removal")
public class BioTerminalScreen extends AbstractContainerScreen<BioTerminalMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/bio_terminal.png");
    private EditBox input;
    private String consoleText = "";
    private String programText = "";
    private int consoleScrollPixels = 0;
    private boolean stickToBottom = true;
    private boolean programCapturesInput = false;

    public BioTerminalScreen(BioTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int textAreaX = this.leftPos + 8;
        int textAreaY = this.topPos + 16;
        boolean showProgram = this.programText != null && !this.programText.isEmpty();
        int textAreaWidth = this.imageWidth - 10;
        int inputHeight = 14;
        int gapBetween = 8;
        int bottomMargin = 6;
        int usedTop = textAreaY - this.topPos;
        int textAreaHeight = this.imageHeight - usedTop - gapBetween - inputHeight - bottomMargin;
        if (textAreaHeight < this.font.lineHeight) {
            textAreaHeight = this.font.lineHeight;
        }
        var wrapped = this.font.split(Component.literal(showProgram ? "" : this.consoleText), textAreaWidth);
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

        if (!showProgram && maxScroll > 0) {
            int barTrackX = textAreaX + textAreaWidth - 2;
            int barTrackY = textAreaY;
            int barTrackH = textAreaHeight;
            int barH = Math.max(8, (int)((float)textAreaHeight * ((float)textAreaHeight / (float)contentHeight)));
            int barMaxY = barTrackH - barH;
            int barY = barTrackY + (int)((float)this.consoleScrollPixels / (float)maxScroll * barMaxY);
            graphics.fill(barTrackX, barTrackY, barTrackX + 1, barTrackY + barTrackH, 0x55000000);
            graphics.fill(barTrackX, barY, barTrackX + 2, barY + barH, 0x88FFFFFF);
        }
        // Program screen full panel
        if (showProgram) {
            int progX = textAreaX;
            int progY = textAreaY;
            int progW = textAreaWidth;
            int progH = textAreaHeight;
            int py = progY;
            String[] lines = this.programText.split("\\r?\\n", -1);
            // Skip leading non-rendering marker lines like [READOUT]
            int startLine = 0;
            while (startLine < lines.length && lines[startLine].startsWith("[READOUT]")) startLine++;
            for (int li = startLine; li < lines.length; li++) {
                if (py + this.font.lineHeight > progY + progH) break;
                String raw = lines[li];
                int color = 0xFFFFFF;
                boolean alignCenter = false;
                boolean alignRight = false;
                // Parse multiple tags in any order at the start of the line
                boolean parsing = true;
                while (parsing && raw.startsWith("[")) {
                    int end = raw.indexOf(']');
                    if (end <= 0) break;
                    String tag = raw.substring(0, end + 1);
                    if ("[RED]".equals(tag)) { color = 0xFF5555; }
                    else if ("[GREEN]".equals(tag)) { color = 0x55FF55; }
                    else if ("[YELLOW]".equals(tag)) { color = 0xFFFF55; }
                    else if ("[AQUA]".equals(tag)) { color = 0x55FFFF; }
                    else if ("[GRAY]".equals(tag)) { color = 0xAAAAAA; }
                    else if ("[CENTER]".equals(tag)) { alignCenter = true; alignRight = false; }
                    else if ("[RIGHT]".equals(tag)) { alignRight = true; alignCenter = false; }
                    else if ("[READOUT]".equals(tag)) { /* marker to toggle input; do not render */ }
                    else { parsing = false; break; }
                    raw = raw.substring(end + 1);
                }
                // Extract optional [CORE]...[/CORE] markers
                int coreStartIdx = raw.indexOf("[CORE]");
                int coreEndIdx = coreStartIdx >= 0 ? raw.indexOf("[/CORE]", coreStartIdx + 6) : -1;
                boolean hasCore = coreStartIdx >= 0 && coreEndIdx > coreStartIdx;
                String displayText = hasCore ? raw.replace("[CORE]", "").replace("[/CORE]", "") : raw;

                var wrappedSeqs = this.font.split(Component.literal(displayText), progW);
                for (int wi = 0; wi < wrappedSeqs.size(); wi++) {
                    if (py + this.font.lineHeight > progY + progH) break;
                    var comp = wrappedSeqs.get(wi);
                    int drawX = progX;
                    if (alignCenter) {
                        if (hasCore) {
                            // Compute core-centered X
                            // Recompute left/core/right from original raw
                            String left = raw.substring(0, coreStartIdx);
                            String core = raw.substring(coreStartIdx + 6, coreEndIdx);
                            String right = raw.substring(coreEndIdx + 7);
                            // Strip tags from left/right segments too
                            left = left.replace("[CENTER]", "").replace("[RIGHT]", "").replace("[RED]", "").replace("[GREEN]", "").replace("[YELLOW]", "").replace("[AQUA]", "").replace("[GRAY]", "");
                            int leftWidth = this.font.width(left);
                            int coreWidth = this.font.width(core);
                            drawX = progX + Math.max(0, (progW - coreWidth) / 2) - leftWidth;
                        } else {
                            int w = this.font.width(comp);
                            drawX = progX + Math.max(0, (progW - w) / 2);
                        }
                    } else if (alignRight) {
                        int w = this.font.width(comp);
                        drawX = progX + Math.max(0, progW - w);
                    }
                    graphics.drawString(this.font, comp, drawX, py, color, false);
                    py += this.font.lineHeight;
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (!this.programCapturesInput) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                submitCommand();
                return true;
            }
        } else {
            // Program capture: map keys to actions
            String action = null;
            if (keyCode == 87) action = "up";          // W
            else if (keyCode == 83) action = "down";   // S
            else if (keyCode == 257 || keyCode == 335) action = "enter"; // Enter
            else if ((modifiers & 0x2) != 0 && keyCode == 67) action = "interrupt"; // Ctrl+C
            if (action != null) {
                var pos = this.menu.getBlockPos();
                com.github.b4ndithelps.forge.network.BQLNetwork.CHANNEL.sendToServer(
                        new com.github.b4ndithelps.forge.network.ProgramInputC2SPacket(pos, action)
                );
                return true;
            }
        }

        if (keyCode == 265) {
            BioTerminalBlockEntity be = getTerminalBE();
            if (be != null) {
                String prev = be.historyPrev();
                this.input.setValue(prev);
                return true;
            }
        }

        if (keyCode == 264) {
            BioTerminalBlockEntity be = getTerminalBE();
            if (be != null) {
                String next = be.historyNext();
                this.input.setValue(next);
                return true;
            }
        }
        if (!this.programCapturesInput && this.input.keyPressed(keyCode, scanCode, modifiers)) {
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
        int textAreaWidth = this.imageWidth - 10;
        int inputHeight = 14;
        int gapBetween = 8;
        int bottomMargin = 6;
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
        var pos = this.menu.getBlockPos();
        var be = this.minecraft.level.getBlockEntity(pos);
        if (be instanceof BioTerminalBlockEntity terminal) {
            this.consoleText = terminal.getConsoleText();
            this.programText = terminal.getProgramScreenTextClient();
            // Heuristic: when a readout screen is shown, capture input and hide text box
            this.programCapturesInput = this.programText != null && !this.programText.isEmpty() && this.programText.contains("[READOUT]");
            this.input.setVisible(!this.programCapturesInput);
            this.input.setEditable(!this.programCapturesInput);
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

    private BioTerminalBlockEntity getTerminalBE() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        var pos = this.menu.getBlockPos();
        var be = this.minecraft.level.getBlockEntity(pos);
        if (be instanceof BioTerminalBlockEntity) {
            return (BioTerminalBlockEntity) be;
        }
        return null;
    }
}


