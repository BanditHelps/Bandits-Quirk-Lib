package com.github.b4ndithelps.forge.fancymenu.elements;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ExecutableElement;
import de.keksuccino.fancymenu.customization.element.elements.item.NBTBuilder;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IconButtonElement extends AbstractElement implements ExecutableElement {

    @Nullable
    private AbstractWidget widget;
    @Nullable
    private ItemStack cachedIcon = null;

    // Button properties
    public ResourceSupplier<IAudio> clickSound;
    public ResourceSupplier<IAudio> hoverSound;
    @Nullable
    public String label;
    @Nullable
    public String hoverLabel;
    public String tooltip;
    public ResourceSupplier<ITexture> backgroundTextureNormal;
    public ResourceSupplier<ITexture> backgroundTextureHover;
    public ResourceSupplier<ITexture> backgroundTextureInactive;
    public boolean restartBackgroundAnimationsOnHover = true;
    public boolean nineSliceCustomBackground = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    public boolean navigatable = true;
    @NotNull
    public GenericExecutableBlock actionExecutor = new GenericExecutableBlock();
    @NotNull
    public LoadingRequirementContainer activeStateSupplier = new LoadingRequirementContainer();

    // Icon properties
    @NotNull
    public String itemKey = "" + BuiltInRegistries.ITEM.getKey(Items.BARRIER);
    public boolean enchanted = false;
    @NotNull
    public String itemCount = "1";
    @Nullable
    public String lore = null;
    @Nullable
    public String itemName = null;
    public boolean showIconTooltip = false; // Separate from button tooltip
    @Nullable
    public String nbtData = null;
    public int iconOffsetX = 0;
    public int iconOffsetY = 0;
    public float iconScale = 1.0f;

    // Cached values for icon updates
    private String lastItemKey = null;
    private boolean lastEnchanted = false;
    private String lastLore = null;
    private String lastItemName = null;
    private String lastNbtData = null;
    private final Font font = Minecraft.getInstance().font;

    public IconButtonElement(ElementBuilder<IconButtonElement, ?> builder) {
        super(builder);
    }

    @Override
    public void tick() {
        if (this.widget != null) {
            this.updateWidget();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.widget == null || !this.shouldRender()) return;

        this.updateWidget();

        if (isEditor()) {
            net.minecraft.client.gui.components.Tooltip cachedVanillaTooltip = this.widget.getTooltip();
            boolean cachedVisible = this.widget.visible;
            boolean cachedActive = this.widget.active;
            this.widget.visible = true;
            this.widget.active = true;
            this.widget.setTooltip(null);
            MainThreadTaskExecutor.executeInMainThread(() -> {
                this.widget.visible = cachedVisible;
                this.widget.active = cachedActive;
                this.widget.setTooltip(cachedVanillaTooltip);
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }

        // Render button
        if (this.widget.getHeight() > 0 && this.widget.getWidth() > 0) {
            this.widget.render(graphics, mouseX, mouseY, partial);
        }

        // Render icon on top
        this.updateCachedIcon();
        if (this.cachedIcon != null) {
            this.renderIcon(graphics, mouseX, mouseY);
        }

        RenderingUtils.resetShaderColor(graphics);
    }

    private void renderIcon(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        int count = SerializationUtils.deserializeNumber(Integer.class, 1, PlaceholderParser.replacePlaceholders(this.itemCount));

        int x = this.getAbsoluteX() + iconOffsetX;
        int y = this.getAbsoluteY() + iconOffsetY;
        int size = (int)(Math.min(this.getAbsoluteWidth(), this.getAbsoluteHeight()) * iconScale);

        // Center the icon in the button
        x += (this.getAbsoluteWidth() - size) / 2;
        y += (this.getAbsoluteHeight() - size) / 2;

        RenderSystem.enableBlend();
        this.renderScaledItem(graphics, this.cachedIcon, x, y, size, size);

        if (count > 1) {
            this.renderItemCount(graphics, this.font, x, y, size, count);
        }

        if (!isEditor() && this.showIconTooltip && UIBase.isXYInArea(mouseX, mouseY, x, y, size, size)) {
            ScreenRenderUtils.postPostRenderTask((graphics1, mouseX1, mouseY1, partial) ->
                    graphics1.renderTooltip(this.font, Screen.getTooltipFromItem(Minecraft.getInstance(), this.cachedIcon),
                            this.cachedIcon.getTooltipImage(), mouseX, mouseY));
        }
        RenderSystem.disableBlend();
    }

    private void updateCachedIcon() {
        String keyFinal = PlaceholderParser.replacePlaceholders(this.itemKey);
        String loreFinal = (this.lore == null) ? null : PlaceholderParser.replacePlaceholders(this.lore);
        String nameFinal = (this.itemName == null) ? null : PlaceholderParser.replacePlaceholders(this.itemName);
        String nbtFinal = (this.nbtData == null) ? null : PlaceholderParser.replacePlaceholders(this.nbtData);

        try {
            if ((this.cachedIcon == null) || !keyFinal.equals(this.lastItemKey) ||
                    (this.enchanted != this.lastEnchanted) || !Objects.equals(loreFinal, this.lastLore) ||
                    !Objects.equals(nameFinal, this.lastItemName) || !Objects.equals(nbtFinal, this.lastNbtData)) {

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(keyFinal));
                this.cachedIcon = new ItemStack(item);

                if (nbtFinal != null) {
                    CompoundTag nbt = NBTBuilder.buildNbtFromString(this.cachedIcon, nbtFinal);
                    if (nbt != null) {
                        this.cachedIcon.setTag(nbt);
                    }
                }

                if (this.enchanted) this.cachedIcon.enchant(Enchantments.AQUA_AFFINITY, 1);

                if ((loreFinal != null) && !loreFinal.isBlank()) {
                    List<Component> lines = new ArrayList<>();
                    for (String line : StringUtils.splitLines(loreFinal.replace("%n%", "\n"), "\n")) {
                        lines.add(buildComponent(line));
                    }
                    setLore(this.cachedIcon, lines);
                }

                if (nameFinal != null) {
                    this.cachedIcon.setHoverName(buildComponent(nameFinal));
                }
            }
        } catch (Exception ex) {
            BanditsQuirkLibForge.LOGGER.error("[FANCYMENU] Failed to create ItemStack for IconButton element!", ex);
            this.cachedIcon = new ItemStack(Items.BARRIER);
        }

        this.lastItemKey = keyFinal;
        this.lastEnchanted = this.enchanted;
        this.lastLore = loreFinal;
        this.lastItemName = nameFinal;
        this.lastNbtData = nbtFinal;
    }

    private void renderScaledItem(@NotNull GuiGraphics graphics, @NotNull ItemStack stack, int x, int y, int width, int height) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        float scale = Math.min(width, height) / 16.0F;
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    private void renderItemCount(@NotNull GuiGraphics graphics, @NotNull Font font, int x, int y, int size, int count) {
        PoseStack pose = graphics.pose();
        String text = String.valueOf(count);
        float scaleFactor = size / 16.0F;

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 200.0F);
        pose.pushPose();
        pose.scale(scaleFactor, scaleFactor, 1.0F);

        int scaledX = (int)((x / scaleFactor) + 19 - 2 - font.width(text));
        int scaledY = (int)((y / scaleFactor) + 6 + 3);

        graphics.drawString(font, text, scaledX, scaledY, -1, true);
        pose.popPose();
        pose.popPose();
    }

    private static void setLore(@NotNull ItemStack stack, @NotNull List<Component> loreLines) {
        CompoundTag displayTag = stack.getOrCreateTagElement("display");
        ListTag loreList = new ListTag();
        for (Component line : loreLines) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        displayTag.put("Lore", loreList);
    }

    public void updateWidget() {
        if (this.widget == null) return;

        this.widget.active = this.activeStateSupplier.requirementsMet();
        this.widget.setAlpha(this.opacity);
        this.widget.setX(this.getAbsoluteX());
        this.widget.setY(this.getAbsoluteY());
        this.widget.setWidth(this.getAbsoluteWidth());
        ((IMixinAbstractWidget) this.widget).setHeightFancyMenu(this.getAbsoluteHeight());

        // Update labels
        String l = this.label;
        String h = this.hoverLabel;
        if (l != null) {
            this.widget.setMessage(buildComponent(l));
        } else {
            this.widget.setMessage(Component.empty());
        }
        if ((h != null) && this.widget.isHoveredOrFocused() && this.widget.active) {
            this.widget.setMessage(buildComponent(h));
        }

        // Update tooltip
        if ((this.tooltip != null) && this.widget.isHovered() && this.widget.visible && this.shouldRender() && !isEditor()) {
            String tooltip = this.tooltip.replace("%n%", "\n");
            TooltipHandler.INSTANCE.addWidgetTooltip(this.widget, Tooltip.of(StringUtils.splitLines(PlaceholderParser.replacePlaceholders(tooltip), "\n")), false, true);
        }

        // Update customizable widget properties
        if (this.widget instanceof CustomizableWidget w) {
            w.setHiddenFancyMenu(!this.shouldRender());
            w.setHoverSoundFancyMenu((this.hoverSound != null) ? this.hoverSound.get() : null);
            w.setCustomClickSoundFancyMenu((this.clickSound != null) ? this.clickSound.get() : null);
            w.setNineSliceCustomBackground_FancyMenu(this.nineSliceCustomBackground);
            w.setNineSliceBorderX_FancyMenu(this.nineSliceBorderX);
            w.setNineSliceBorderY_FancyMenu(this.nineSliceBorderY);
            w.setCustomBackgroundNormalFancyMenu((this.backgroundTextureNormal != null) ? this.backgroundTextureNormal.get() : null);
            w.setCustomBackgroundHoverFancyMenu((this.backgroundTextureHover != null) ? this.backgroundTextureHover.get() : null);
            w.setCustomBackgroundInactiveFancyMenu((this.backgroundTextureInactive != null) ? this.backgroundTextureInactive.get() : null);
            w.setCustomBackgroundResetBehaviorFancyMenu(this.restartBackgroundAnimationsOnHover ? CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER : CustomizableWidget.CustomBackgroundResetBehavior.RESET_NEVER);
        }

        if (this.widget instanceof NavigatableWidget w) {
            w.setNavigatable(this.navigatable);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return (this.widget == null) ? null : List.of(this.widget);
    }

    @Nullable
    public AbstractWidget getWidget() {
        return this.widget;
    }

    public void setWidget(@Nullable AbstractWidget widget) {
        this.widget = widget;
    }

    @Override
    public @NotNull GenericExecutableBlock getExecutableBlock() {
        return this.actionExecutor;
    }

}
