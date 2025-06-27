package com.github.b4ndithelps.forge.fancymenu;

import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This code was taken and only SLIGHTLY modified from the original FancyMenu implementation of ItemKeyScreen.java
 * The reason I did that was because the constructor was protected.
 *
 * Credit goes to Keksuccino for this method
 */
public class IconKeyScreen extends StringBuilderScreen {
    protected @NotNull String itemKey;
    protected CellScreen.TextInputCell itemKeyCell;
    protected EditBoxSuggestions itemKeySuggestions;

    public IconKeyScreen(@NotNull String value, @NotNull Consumer<String> callback) {
        super(Component.translatable("fancymenu.elements.item.key"), callback);
        this.itemKey = value;
    }

    protected void initCells() {
        this.addSpacerCell(20);
        String key = this.getItemKeyString();
        this.addLabelCell(Component.translatable("fancymenu.elements.item.key.screen.key"));
        this.itemKeyCell = this.addTextInputCell((CharacterFilter)null, true, true).setText(key);
        this.addCellGroupEndSpacerCell();
        this.itemKeySuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.itemKeyCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, this.getItemKeys());
        UIBase.applyDefaultWidgetSkinTo(this.itemKeySuggestions);
        this.itemKeyCell.editBox.setResponder((s) -> {
            this.itemKeySuggestions.updateCommandInfo();
        });
        this.addSpacerCell(20);
    }

    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        this.itemKeySuggestions.render(graphics, mouseX, mouseY);
    }

    public boolean keyPressed(int $$0, int $$1, int $$2) {
        return this.itemKeySuggestions.keyPressed($$0, $$1, $$2) ? true : super.keyPressed($$0, $$1, $$2);
    }

    public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaY) {
        return this.itemKeySuggestions.mouseScrolled(scrollDeltaY) ? true : super.mouseScrolled($$0, $$1, scrollDeltaY);
    }

    public boolean mouseClicked(double $$0, double $$1, int $$2) {
        return this.itemKeySuggestions.mouseClicked($$0, $$1, $$2) ? true : super.mouseClicked($$0, $$1, $$2);
    }

    public @NotNull String buildString() {
        return this.getItemKeyString();
    }

    protected @NotNull String getItemKeyString() {
        return this.itemKeyCell != null ? this.itemKeyCell.getText() : this.itemKey;
    }

    protected @NotNull List<String> getItemKeys() {
        List<String> keys = new ArrayList();
        BuiltInRegistries.ITEM.keySet().forEach((location) -> {
            keys.add("" + String.valueOf(location));
        });
        return keys;
    }
}
