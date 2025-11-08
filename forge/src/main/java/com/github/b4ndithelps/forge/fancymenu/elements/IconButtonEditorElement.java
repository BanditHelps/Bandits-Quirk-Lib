package com.github.b4ndithelps.forge.fancymenu.elements;

import com.github.b4ndithelps.forge.fancymenu.IconKeyScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class IconButtonEditorElement extends AbstractEditorElement {
    public IconButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        // Action management
        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
                    ManageActionsScreen s = new ManageActionsScreen(this.getElement().getExecutableBlock(), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().actionExecutor = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.button.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        // Active state controller
        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Component.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(this.getElement().activeStateSupplier.copy(false), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.getElement().activeStateSupplier = call;
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("bql_separator_1");

        // Button properties
        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                        IconButtonEditorElement.class,
                        consumes -> consumes.getElement().label,
                        (element, s) -> element.getElement().label = s,
                        null, false, true, Component.translatable("fancymenu.editor.items.button.editlabel"),
                        true, null, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                        IconButtonEditorElement.class,
                        consumes -> consumes.getElement().hoverLabel,
                        (element, s) -> element.getElement().hoverLabel = s,
                        null, false, true, Component.translatable("fancymenu.editor.items.button.hoverlabel"),
                        true, null, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        consumes -> (consumes instanceof IconButtonEditorElement),
                        consumes -> {
                            String t = ((IconButtonEditorElement) consumes).getElement().tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            ((IconButtonEditorElement) element).getElement().tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.editor.items.button.btndescription"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        this.rightClickMenu.addSeparatorEntry("bql_separator_2");

        // Icon properties
        ConsumingSupplier<IconButtonEditorElement, String> itemKeyTargetFieldGetter = consumes -> consumes.getElement().itemKey;
        BiConsumer<IconButtonEditorElement, String> itemKeyTargetFieldSetter = (iconButtonEditorElement, s) -> iconButtonEditorElement.getElement().itemKey = s;

        ContextMenu.ClickableContextMenuEntry<?> itemKeyEntry = this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_key", IconButtonEditorElement.class,
                        itemKeyTargetFieldGetter,
                        itemKeyTargetFieldSetter,
                        null, false, true, Component.translatable("fancymenu.elements.item.key"),
                        true, "" + BuiltInRegistries.ITEM.getKey(Items.BARRIER), null, null)
                .setStackable(false);

        if (itemKeyEntry instanceof ContextMenu.SubMenuContextMenuEntry subMenuEntry) {
            subMenuEntry.getSubContextMenu().removeEntry("input_value");
            subMenuEntry.getSubContextMenu().addClickableEntryAt(0, "input_value", Component.translatable("fancymenu.guicomponents.set"), (menu, entry) -> {
                if (entry.getStackMeta().isFirstInStack()) {
                    Screen inputScreen = new IconKeyScreen(itemKeyTargetFieldGetter.get(this), callback -> {
                        if (callback != null) {
                            this.editor.history.saveSnapshot();
                            itemKeyTargetFieldSetter.accept(this, callback);
                        }
                        menu.closeMenu();
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(inputScreen);
                }
            }).setStackable(false);
        }

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_name", IconButtonEditorElement.class,
                consumes -> consumes.getElement().itemName,
                (iconButtonEditorElement, s) -> iconButtonEditorElement.getElement().itemName = s,
                null, false, true, Component.translatable("fancymenu.elements.item.name"),
                true, null, null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_count", IconButtonEditorElement.class,
                consumes -> consumes.getElement().itemCount,
                (iconButtonEditorElement, s) -> iconButtonEditorElement.getElement().itemCount = s,
                null, false, true, Component.translatable("fancymenu.elements.item.item_count"),
                true, "1", null, null);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "item_lore", IconButtonEditorElement.class, consumes -> {
                    if (consumes.getElement().lore != null) return consumes.getElement().lore.replace("%n%", "\n");
                    return "";
                }, (iconButtonEditorElement, s) -> {
                    if (s != null) {
                        iconButtonEditorElement.getElement().lore = s.replace("\n", "%n%");
                        if (iconButtonEditorElement.getElement().lore.isBlank()) iconButtonEditorElement.getElement().lore = null;
                    } else {
                        iconButtonEditorElement.getElement().lore = null;
                    }
                }, null, true, true, Component.translatable("fancymenu.elements.item.lore"), true, null, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.lore.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "enchanted", IconButtonEditorElement.class,
                consumes -> consumes.getElement().enchanted,
                (iconButtonEditorElement, aBoolean) -> iconButtonEditorElement.getElement().enchanted = aBoolean,
                "fancymenu.elements.item.enchanted");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "show_icon_tooltip", IconButtonEditorElement.class,
                consumes -> consumes.getElement().showIconTooltip,
                (iconButtonEditorElement, aBoolean) -> iconButtonEditorElement.getElement().showIconTooltip = aBoolean,
                "fancymenu.elements.item.show_tooltip");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "nbt_data", IconButtonEditorElement.class,
                        consumes -> consumes.getElement().nbtData,
                        (iconButtonEditorElement, s) -> iconButtonEditorElement.getElement().nbtData = s,
                        null, false, true, Component.translatable("fancymenu.elements.item.nbt"),
                        true, null, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.item.nbt.desc")));

        this.rightClickMenu.addSeparatorEntry("bql_separator_3");

        // Icon positioning and scaling
        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "icon_offset_x", IconButtonEditorElement.class,
                consumes -> consumes.getElement().iconOffsetX,
                (iconButtonEditorElement, integer) -> iconButtonEditorElement.getElement().iconOffsetX = integer,
                Component.translatable("banditsquirklib.gui.elements.iconbutton.icon_offset_x"), true, 0, null, null);

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "icon_offset_y", IconButtonEditorElement.class,
                consumes -> consumes.getElement().iconOffsetY,
                (iconButtonEditorElement, integer) -> iconButtonEditorElement.getElement().iconOffsetY = integer,
                Component.translatable("banditsquirklib.gui.elements.iconbutton.icon_offset_y"), true, 0, null, null);

        this.addFloatInputContextMenuEntryTo(this.rightClickMenu, "icon_scale", IconButtonEditorElement.class,
                consumes -> consumes.getElement().iconScale,
                (iconButtonEditorElement, aFloat) -> iconButtonEditorElement.getElement().iconScale = aFloat,
                Component.translatable("banditsquirklib.gui.elements.iconbutton.icon_scale"), true, 1.0f, null, null);

        this.rightClickMenu.addSeparatorEntry("bql_separator_4");

        // Audio settings
        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        IconButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().hoverSound,
                        (iconButtonEditorElement, supplier) -> iconButtonEditorElement.getElement().hoverSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.hoversound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        IconButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (iconButtonEditorElement, supplier) -> iconButtonEditorElement.getElement().clickSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.clicksound"), true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("bql_separator_5");

        // Background textures
        this.addBackgroundTextureOptions();

        this.rightClickMenu.addSeparatorEntry("bql_separator_6");

        // Navigation
        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", IconButtonEditorElement.class,
                        consumes -> consumes.getElement().navigatable,
                        (iconButtonEditorElement, aBoolean) -> iconButtonEditorElement.getElement().navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")));

    }

    protected void addBackgroundTextureOptions() {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("button_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground"), buttonBackgroundMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setStackable(true);

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                IconButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureNormal,
                (iconButtonEditorElement, iTextureResourceSupplier) -> {
                    iconButtonEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                IconButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureHover,
                (iconButtonEditorElement, iTextureResourceSupplier) -> {
                    iconButtonEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover"), true, null, true, true, true);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                IconButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureInactive,
                (iconButtonEditorElement, iTextureResourceSupplier) -> {
                    iconButtonEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive"), true, null, true, true, true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_background").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                        IconButtonEditorElement.class,
                        consumes -> consumes.getElement().restartBackgroundAnimationsOnHover,
                        (iconButtonEditorElement, aBoolean) -> iconButtonEditorElement.getElement().restartBackgroundAnimationsOnHover = aBoolean,
                        "fancymenu.helper.editor.items.buttons.textures.restart_animated_on_hover")
                .setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", IconButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceCustomBackground,
                (iconButtonEditorElement, aBoolean) -> iconButtonEditorElement.getElement().nineSliceCustomBackground = aBoolean,
                "fancymenu.helper.editor.items.buttons.textures.nine_slice");

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", IconButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderX,
                (iconButtonEditorElement, integer) -> iconButtonEditorElement.getElement().nineSliceBorderX = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_x"), true, 5, null, null);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", IconButtonEditorElement.class,
                consumes -> consumes.getElement().nineSliceBorderY,
                (iconButtonEditorElement, integer) -> iconButtonEditorElement.getElement().nineSliceBorderY = integer,
                Component.translatable("fancymenu.helper.editor.items.buttons.textures.nine_slice.border_y"), true, 5, null, null);

    }

    public IconButtonElement getElement() {
        return (IconButtonElement) this.element;
    }
}