package com.github.b4ndithelps.forge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A screen that displays information about the options.txt file being replaced
 * Similar to the settings screens but with specific information about customization bar removal
 */
public class OptionsReplacedInfoScreen extends Screen {
    private static final Component TITLE = Component.literal("Welcome to the Mine Hero Addon!");
    private static final Component INFO_TEXT = Component.literal("In order to get some cool GUI's, we use FancyMenu created by Keksuccino! To make it a little easier for a normal player, Bandit's Quirk Library has automatically updated your FancyMenu options.txt file to remove the customization bar (seen below). This change will take effect on the next game restart, so we recommend just doing that now! You won't see this message again.");
    private static final Component BUTTON_TEXT = Component.literal("I Understand!");
    
    // Image resource location - you'll place this in resources/assets/bql/textures/gui/
    private static final ResourceLocation OPTIONS_INFO_IMAGE = new ResourceLocation("bql", "textures/gui/options_info.png");
    
    // Image dimensions (adjust these based on your actual image)
    private static final int IMAGE_WIDTH = 400;
    private static final int IMAGE_HEIGHT = 80;

    private final Screen previousScreen;
    
    public OptionsReplacedInfoScreen(Screen previousScreen) {
        super(TITLE);
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        super.init();
        
        // Add "I Understand" button near the bottom
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = this.height - 115; // Close to the middle
        
        this.addRenderableWidget(Button.builder(BUTTON_TEXT, (button) -> {
            // Close this screen and return to the previous screen
            this.onClose();
        }).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBackground(graphics);
        
        // Calculate positions
        int centerX = this.width / 2;
        int startY = 20; // Start near the top
        
        // Render title at the top (normal text)
        graphics.drawCenteredString(this.font, TITLE, centerX, startY, 0xFFFFFF);
        
        // Render info text below the title (with word wrapping)
        List<FormattedCharSequence> wrappedText = this.font.split(INFO_TEXT, this.width - 40);
        int textY = startY + 30; // Space below title
        for (FormattedCharSequence line : wrappedText) {
            graphics.drawCenteredString(this.font, line, centerX, textY, 0xCCCCCC);
            textY += this.font.lineHeight + 2;
        }
        
        // Render image below the text
        int imageX = centerX - IMAGE_WIDTH / 2;
        int imageY = textY + 40; // Space below text
        graphics.blit(OPTIONS_INFO_IMAGE, imageX, imageY, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // Render widgets (button at bottom)
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }
}
