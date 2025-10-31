package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.blocks.BioTerminalRefMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import com.github.b4ndithelps.forge.client.refprog.RefAnalyzeProgram;

@SuppressWarnings("removal")
public class BioTerminalRefScreen extends AbstractContainerScreen<BioTerminalRefMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("bandits_quirk_lib", "textures/gui/bio_terminal.png");

    // Display the current active program as text
    private String programText = "Test 1\nTest 2\nTest 3\nTest 4\nTest 5\nTest 6\nTest 7\nTest 8\nTest 9\nTest 10\nTest 11\nTest 12\n";

    // Debug flag to outline the program area using ASCII characters
    private static final boolean DEBUG_PROGRAM_AREA = false;

    // Scrolling state for the program area
    private int programScrollPixels = 0;

	// Centralized geometry controls for the program area
	private static final int PROGRAM_LEFT_MARGIN = 8;           // distance from left edge of panel
	private static final int PROGRAM_RIGHT_MARGIN = 8;          // distance from right edge of panel
	private static final int PROGRAM_TOP_OFFSET_PIXELS = 16;    // base pixels below panel top
	private static final int PROGRAM_TOP_TAB_ROWS = 2;          // tab text rows reserved above
	private static final int PROGRAM_BOTTOM_MARGIN = 2;         // bottom padding inside panel

	private static final class Rect { final int x, y, w, h; Rect(int x, int y, int w, int h){ this.x=x; this.y=y; this.w=w; this.h=h; } }

	private Rect getProgramArea() {
		int x = this.leftPos + PROGRAM_LEFT_MARGIN;
		int y = this.topPos + PROGRAM_TOP_OFFSET_PIXELS + this.font.lineHeight * PROGRAM_TOP_TAB_ROWS;
		int w = this.imageWidth - PROGRAM_LEFT_MARGIN - PROGRAM_RIGHT_MARGIN;
		int usedTop = y - this.topPos;
		int h = this.imageHeight - usedTop - PROGRAM_BOTTOM_MARGIN;
		if (h < this.font.lineHeight) h = this.font.lineHeight;
		return new Rect(x, y, w, h);
	}

    // Track the current tab state
    private final String[] tabs = new String[]{"ANALYZE","CATALOG","SLICE","COMBINE","PRINT"};
    private int activeTabIndex = 0;

    // Client-side program instance for Analyze
    private RefAnalyzeProgram analyzeProgram;
    // Client-side program instance for Catalog
    private com.github.b4ndithelps.forge.client.refprog.RefCatalogProgram catalogProgram;
    // Client-side program instance for Slicer
    private com.github.b4ndithelps.forge.client.refprog.RefSlicerProgram slicerProgram;

    public BioTerminalRefScreen(BioTerminalRefMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Renders the actual image for the background
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        drawTabList(graphics);


		// Program Section - All actual logic for the selected tab goes here
		Rect area = getProgramArea();

        // If on Analyze tab, render the client-side Analyze program into the area
        if (activeTabIndex == 0) {
            if (analyzeProgram == null) {
                analyzeProgram = new com.github.b4ndithelps.forge.client.refprog.RefAnalyzeProgram(this, this.menu.getBlockPos());
            } else if ((this.minecraft != null && this.minecraft.level != null) && (this.minecraft.level.getGameTime() % 20L == 0L)) {
                // Refresh connected devices roughly every second
                analyzeProgram.refreshSequencers();
            }
            analyzeProgram.render(graphics, area.x, area.y, area.w, area.h, this.font);
            // Skip the text scroller when program is active
            drawProgramAreaDebug(graphics, area);
            return;
        }

        // If on Catalog tab, render the client-side Catalog program into the area
        if (activeTabIndex == 1) {
            if (catalogProgram == null) {
                catalogProgram = new com.github.b4ndithelps.forge.client.refprog.RefCatalogProgram(this, this.menu.getBlockPos());
            } else if ((this.minecraft != null && this.minecraft.level != null) && (this.minecraft.level.getGameTime() % 20L == 0L)) {
                // Refresh connected devices roughly every second
                catalogProgram.requestSync();
                catalogProgram.refresh();
            }
            catalogProgram.render(graphics, area.x, area.y, area.w, area.h, this.font);
            // Skip the text scroller when program is active
            drawProgramAreaDebug(graphics, area);
            return;
        }

        // If on Slice tab, render the client-side Slicer program into the area
        if (activeTabIndex == 2) {
            if (slicerProgram == null) {
                slicerProgram = new com.github.b4ndithelps.forge.client.refprog.RefSlicerProgram(this, this.menu.getBlockPos());
            } else if ((this.minecraft != null && this.minecraft.level != null) && (this.minecraft.level.getGameTime() % 20L == 0L)) {
                // Refresh connected devices roughly every second
                slicerProgram.refresh();
            }
            slicerProgram.render(graphics, area.x, area.y, area.w, area.h, this.font);
            // Skip the text scroller when program is active
            drawProgramAreaDebug(graphics, area);
            return;
        }

        // Render program text inside a scrollable area
        String text = this.programText == null ? "" : this.programText;
        var wrapped = this.font.split(Component.literal(text), area.w);
		int contentHeight = wrapped.size() * this.font.lineHeight;
		int maxScroll = Math.max(0, contentHeight - area.h);
        this.programScrollPixels = Mth.clamp(this.programScrollPixels, 0, maxScroll);

        int startPixel = this.programScrollPixels;
        int firstLineIndex = startPixel / this.font.lineHeight;
        int yOffset = -(startPixel % this.font.lineHeight);
		int y = area.y + yOffset;
        for (int i = firstLineIndex; i < wrapped.size(); i++) {
			if (y + this.font.lineHeight > area.y && y < area.y + area.h) {
				graphics.drawString(this.font, wrapped.get(i), area.x, y, 0xFFFFFF, false);
            }
            y += this.font.lineHeight;
			if (y >= area.y + area.h) break;
        }

        // Simple scrollbar on the right side of the program area
        if (maxScroll > 0) {
			int barTrackX = area.x + area.w - 2;
			int barH = Math.max(8, (int)((float)area.h * ((float)area.h / (float)contentHeight)));
			int barMaxY = area.h - barH;
			int barY = area.y + (int)((float)this.programScrollPixels / (float)maxScroll * barMaxY);
			graphics.fill(barTrackX, area.y, barTrackX + 1, area.y + area.h, 0x55000000);
			graphics.fill(barTrackX, barY, barTrackX + 2, barY + barH, 0x88FFFFFF);
        }

        // Debug outline for the program area
        drawProgramAreaDebug(graphics, area);
    }

    private void drawProgramAreaDebug(GuiGraphics graphics, Rect area) {
        if (!DEBUG_PROGRAM_AREA) return;
        int dashWidth = Math.max(1, this.font.width("-"));
        int dashCount = Math.max(1, area.w / dashWidth);
        String dashes = "-".repeat(dashCount);
        int debugColor = 0xFFFF55; // yellow
        graphics.drawString(this.font, dashes, area.x, area.y, debugColor, false);
        graphics.drawString(this.font, dashes, area.x, area.y + Math.max(0, area.h - this.font.lineHeight), debugColor, false);
        int leftX = area.x;
        int rightX = area.x + Math.max(0, area.w - this.font.width("|"));
        int vy = area.y;
        int bottomY = area.y + Math.max(0, area.h - this.font.lineHeight);
        while (vy <= bottomY) {
            graphics.drawString(this.font, "|", leftX, vy, debugColor, false);
            graphics.drawString(this.font, "|", rightX, vy, debugColor, false);
            vy += this.font.lineHeight;
        }
    }

    // Overriding this and leaving it blank removes the titles like "Inventory"
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        // Program tooltips (rendered above everything)
        if (activeTabIndex == 1 && catalogProgram != null) {
            catalogProgram.renderTooltips(graphics, mouseX, mouseY, this.font);
        }
    }

    // Handle when the WASD keys are pressed, and move the appropriate section
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Tab Navigation with the A and D keys
        if (keyCode == 65) { // A
            changeTab(-1);
            return true;
        }

        if (keyCode == 68) { // D
            changeTab(1);
            return true;
        }

        // Analyze tab interactions
        if (activeTabIndex == 0 && analyzeProgram != null) {
            if (keyCode == 87) { // W
                analyzeProgram.moveSelection(-1);
                return true;
            }
            if (keyCode == 83) { // S
                analyzeProgram.moveSelection(1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                analyzeProgram.startSelected();
                return true;
            }
        }

        // Catalog tab interactions
        if (activeTabIndex == 1 && catalogProgram != null) {
            if (keyCode == 87) { // W
                catalogProgram.moveSelection(-1);
                return true;
            }
            if (keyCode == 83) { // S
                catalogProgram.moveSelection(1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                catalogProgram.startSelected();
                return true;
            }
        }

        // Slice tab interactions
        if (activeTabIndex == 2 && slicerProgram != null) {
            if (keyCode == 87) { // W
                slicerProgram.moveSelection(-1);
                return true;
            }
            if (keyCode == 83) { // S
                slicerProgram.moveSelection(1);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                slicerProgram.startSelected();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void changeTab(int change) {
        // Move the index, having it automatically handle the edges
        this.activeTabIndex = (this.activeTabIndex + change + tabs.length) % tabs.length;
    }

    private void drawTabList(GuiGraphics graphics) {
        // Draw the tabs at the top of the screen
		int tabsX = this.leftPos + 12;
		int tabsY = this.topPos + 8;
		int x = tabsX;

		int[] startXs = new int[tabs.length];
		int[] endXs = new int[tabs.length];

		// Draw tabs and record their pixel ranges
		for (int i = 0; i < tabs.length; i++) {
			boolean active = (i == activeTabIndex);
			String wrapped = active ? ("⌈" + tabs[i] + "⌉") : (" " + tabs[i] + " ");
			startXs[i] = x;
			int w = this.font.width(wrapped);
			endXs[i] = x + w;
			graphics.drawString(this.font, wrapped, x, tabsY, 0x55FF55, false);
			x += w + 4; // spacing between tabs
		}

		// Compute underline segments with a gap under the active tab
		int lineY = tabsY + 8;
		int lineStart = tabsX - 6; // left padding for the line
		int lineEnd = x;           // right edge aligned to last tab end
		int gapStart = startXs[activeTabIndex];
		int gapEnd = endXs[activeTabIndex];
		int lineThickness = 1; // set to 2 for a thicker rule
		int lineColor = 0xFF55FF55; // Opaque green (AARRGGBB)

		// Left solid segment
		if (gapStart > lineStart) {
			graphics.fill(lineStart, lineY, gapStart, lineY + lineThickness, lineColor);
		}
		// Right solid segment
		if (lineEnd > gapEnd) {
			graphics.fill(gapEnd, lineY, lineEnd, lineY + lineThickness, lineColor);
		}
    }

	private void drawDashedLinePhased(GuiGraphics graphics, int x1, int x2, int y, int dashLen, int gapLen, int color, int phaseOrigin) {
		if (x2 <= x1 || dashLen <= 0) return;
		int period = dashLen + Math.max(0, gapLen);
		if (period <= 0) return;
		int delta = x1 - phaseOrigin;
		int k = delta <= 0 ? 0 : (delta + period - 1) / period; // ceil(delta/period) for positive delta
		int start = phaseOrigin + k * period;
		for (int sx = start; sx < x2; sx += period) {
			int ex = Math.min(sx + dashLen, x2);
			if (ex > sx) {
				graphics.fill(sx, y, ex, y + 1, color);
			}
		}
	}

	// Method handles the scrolling of just the program area
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		// Program area geometry must match renderBg calculations
		Rect area = getProgramArea();

		boolean overProgramArea = mouseX >= area.x && mouseX <= area.x + area.w
				&& mouseY >= area.y && mouseY <= area.y + area.h;
        if (overProgramArea) {
            String text = this.programText == null ? "" : this.programText;
			var wrapped = this.font.split(Component.literal(text), area.w);
			int contentHeight = wrapped.size() * this.font.lineHeight;
			int maxScroll = Math.max(0, contentHeight - area.h);
            if (maxScroll > 0) {
                int step = this.font.lineHeight * 3;
                this.programScrollPixels = Mth.clamp(this.programScrollPixels - (int)(delta * step), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}


