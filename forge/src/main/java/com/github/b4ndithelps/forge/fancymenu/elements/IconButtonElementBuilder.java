package com.github.b4ndithelps.forge.fancymenu.elements;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IconButtonElementBuilder extends ElementBuilder<IconButtonElement, IconButtonEditorElement> {

    public IconButtonElementBuilder() {
        super("icon_button");
    }

    @Override
    public @NotNull IconButtonElement buildDefaultInstance() {
        IconButtonElement element = new IconButtonElement(this);
        element.baseWidth = 100;
        element.baseHeight = 100;
        element.label = "";
        element.setWidget(new ExtendedButton(0, 0, 0, 0, Component.empty(), (press) -> {
            if ((CustomizationOverlay.getCurrentMenuBarInstance() == null) || !CustomizationOverlay.getCurrentMenuBarInstance().isUserNavigatingInMenuBar()) {
                boolean isMousePressed = MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown();
                element.getExecutableBlock().execute();
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    if (isMousePressed) press.setFocused(false);
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }));
        return element;
    }

    @Override
    public @NotNull IconButtonElement deserializeElement(@NotNull SerializedElement serialized) {
        IconButtonElement element = buildDefaultInstance();

        // Button properties
        element.label = serialized.getValue("label");

        String buttonExecutableBlockId = serialized.getValue("button_element_executable_block_identifier");
        if (buttonExecutableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, buttonExecutableBlockId);
            if (b instanceof GenericExecutableBlock g) {
                element.actionExecutor = g;
            }
        } else {
            GenericExecutableBlock g = new GenericExecutableBlock();
            g.getExecutables().addAll(ActionInstance.deserializeAll(serialized));
            element.actionExecutor = g;
        }

        element.hoverSound = deserializeAudioResourceSupplier(serialized.getValue("hoversound"));
        element.hoverLabel = serialized.getValue("hoverlabel");
        element.tooltip = serialized.getValue("description");
        element.clickSound = deserializeAudioResourceSupplier(serialized.getValue("clicksound"));
        element.backgroundTextureNormal = deserializeImageResourceSupplier(serialized.getValue("backgroundnormal"));
        element.backgroundTextureHover = deserializeImageResourceSupplier(serialized.getValue("backgroundhovered"));
        element.backgroundTextureInactive = deserializeImageResourceSupplier(serialized.getValue("background_texture_inactive"));

        String restartBackAnimationsOnHover = serialized.getValue("restartbackgroundanimations");
        if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
            element.restartBackgroundAnimationsOnHover = false;
        }

        element.nineSliceCustomBackground = deserializeBoolean(element.nineSliceCustomBackground, serialized.getValue("nine_slice_custom_background"));
        element.nineSliceBorderX = deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_border_x"));
        element.nineSliceBorderY = deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_border_y"));
        element.navigatable = deserializeBoolean(element.navigatable, serialized.getValue("navigatable"));

        String activeStateRequirementContainerIdentifier = serialized.getValue("widget_active_state_requirement_container_identifier");
        if (activeStateRequirementContainerIdentifier != null) {
            LoadingRequirementContainer c = LoadingRequirementContainer.deserializeWithIdentifier(activeStateRequirementContainerIdentifier, serialized);
            if (c != null) {
                element.activeStateSupplier = c;
            }
        }

        // Icon properties
        element.itemKey = Objects.requireNonNullElse(serialized.getValue("item_key"), element.itemKey);
        element.itemCount = Objects.requireNonNullElse(serialized.getValue("item_count"), element.itemCount);
        element.enchanted = deserializeBoolean(element.enchanted, serialized.getValue("enchanted"));
        element.itemName = serialized.getValue("item_name");
        element.lore = serialized.getValue("lore");
        element.showIconTooltip = deserializeBoolean(element.showIconTooltip, serialized.getValue("show_icon_tooltip"));
        element.nbtData = serialized.getValue("custom_nbt_data");
        element.iconOffsetX = deserializeNumber(Integer.class, element.iconOffsetX, serialized.getValue("icon_offset_x"));
        element.iconOffsetY = deserializeNumber(Integer.class, element.iconOffsetY, serialized.getValue("icon_offset_y"));
        element.iconScale = deserializeNumber(Float.class, element.iconScale, serialized.getValue("icon_scale"));

        return element;
    }

    @Override
    protected @NotNull SerializedElement serializeElement(@NotNull IconButtonElement element, @NotNull SerializedElement serializeTo) {

        // Button properties
        serializeTo.putProperty("button_element_executable_block_identifier", element.actionExecutor.identifier);
        element.actionExecutor.serializeToExistingPropertyContainer(serializeTo);

        if (element.backgroundTextureNormal != null) {
            serializeTo.putProperty("backgroundnormal", element.backgroundTextureNormal.getSourceWithPrefix());
        }
        if (element.backgroundTextureHover != null) {
            serializeTo.putProperty("backgroundhovered", element.backgroundTextureHover.getSourceWithPrefix());
        }
        if (element.backgroundTextureInactive != null) {
            serializeTo.putProperty("background_texture_inactive", element.backgroundTextureInactive.getSourceWithPrefix());
        }
        serializeTo.putProperty("restartbackgroundanimations", "" + element.restartBackgroundAnimationsOnHover);
        serializeTo.putProperty("nine_slice_custom_background", "" + element.nineSliceCustomBackground);
        serializeTo.putProperty("nine_slice_border_x", "" + element.nineSliceBorderX);
        serializeTo.putProperty("nine_slice_border_y", "" + element.nineSliceBorderY);

        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound.getSourceWithPrefix());
        }
        if (element.hoverLabel != null) {
            serializeTo.putProperty("hoverlabel", element.hoverLabel);
        }
        if (element.clickSound != null) {
            serializeTo.putProperty("clicksound", element.clickSound.getSourceWithPrefix());
        }
        if (element.tooltip != null) {
            serializeTo.putProperty("description", element.tooltip);
        }
        if (element.label != null) {
            serializeTo.putProperty("label", element.label);
        }
        serializeTo.putProperty("navigatable", "" + element.navigatable);

        serializeTo.putProperty("widget_active_state_requirement_container_identifier", element.activeStateSupplier.identifier);
        element.activeStateSupplier.serializeToExistingPropertyContainer(serializeTo);

        // Icon properties
        serializeTo.putProperty("item_key", element.itemKey);
        serializeTo.putProperty("item_count", element.itemCount);
        serializeTo.putProperty("enchanted", "" + element.enchanted);
        if (element.itemName != null) {
            serializeTo.putProperty("item_name", element.itemName);
        }
        if (element.lore != null) {
            serializeTo.putProperty("lore", element.lore);
        }
        serializeTo.putProperty("show_icon_tooltip", "" + element.showIconTooltip);
        if (element.nbtData != null) {
            serializeTo.putProperty("custom_nbt_data", element.nbtData);
        }
        serializeTo.putProperty("icon_offset_x", "" + element.iconOffsetX);
        serializeTo.putProperty("icon_offset_y", "" + element.iconOffsetY);
        serializeTo.putProperty("icon_scale", "" + element.iconScale);

        return serializeTo;
    }

    @Override
    public @NotNull IconButtonEditorElement wrapIntoEditorElement(@NotNull IconButtonElement element, @NotNull LayoutEditorScreen editor) {
        return new IconButtonEditorElement(element, editor);
    }

    @Override
    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        if ((element instanceof IconButtonElement b) && (b.getWidget() != null) && !b.getWidget().getMessage().getString().replace(" ", "").isEmpty()) {
            return b.getWidget().getMessage();
        }
        return Component.translatable("banditsquirklib.gui.editor.add.iconbutton");
    }

    @Override
    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return null;
    }

}
