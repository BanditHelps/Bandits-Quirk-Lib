package com.github.b4ndithelps.forge.fancymenu.elements.enchant_slider;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.slider.v2.SliderEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.slider.v2.SliderElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("removal")
public class EnchantmentSliderElementBuilder extends ElementBuilder<EnchantmentSliderElement, EnchantmentSliderEditorElement> {

    public EnchantmentSliderElementBuilder() {
        super("enchant_slider");
    }

    public @NotNull EnchantmentSliderElement buildDefaultInstance() {
        EnchantmentSliderElement i = new EnchantmentSliderElement(this);
        i.baseWidth = 100;
        i.baseHeight = 20;
        i.listValues.addAll(List.of("some_value", "another_value", "third_value"));
        i.minRangeValue = 0.0;
        i.enchant = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft:sharpness"));
        assert i.enchant != null;
        i.label = i.enchant.getDescriptionId() + ": $$value";
        i.maxRangeValue = i.enchant.getMaxLevel();
        return i;
    }

    public EnchantmentSliderElement deserializeElement(@NotNull SerializedElement serialized) {
        EnchantmentSliderElement element = this.buildDefaultInstance();
        String sliderTypeString = serialized.getValue("slider_type");
        if (sliderTypeString != null) {
            EnchantmentSliderElement.SliderType t = EnchantmentSliderElement.SliderType.getByName(sliderTypeString);
            if (t != null) {
                element.type = t;
            }
        }

        element.preSelectedValue = serialized.getValue("pre_selected_value");
        element.label = serialized.getValue("slider_label");
        element.minRangeValue = (Double)this.deserializeNumber(Double.class, element.minRangeValue, serialized.getValue("min_range_value"));
        element.maxRangeValue = (Double)this.deserializeNumber(Double.class, element.maxRangeValue, serialized.getValue("max_range_value"));
        element.roundingDecimalPlace = (Integer)this.deserializeNumber(Integer.class, element.roundingDecimalPlace, serialized.getValue("rounding_decimal_place"));
        List<Pair<String, String>> listValueEntries = new ArrayList();
        serialized.getProperties().forEach((key, value) -> {
            if (key.startsWith("slider_list_value_")) {
                listValueEntries.add(Pair.of(key, value));
            }

        });
        listValueEntries.sort(Comparator.comparingInt((value) -> {
            String key = (String)value.getKey();
            key = (new StringBuilder(key)).reverse().toString();
            key = (new StringBuilder(key.split("_", 2)[0])).reverse().toString();
            return MathUtils.isInteger(key) ? Integer.parseInt(key) : 0;
        }));
        if (!listValueEntries.isEmpty()) {
            element.listValues.clear();
        }

        listValueEntries.forEach((pair) -> {
            element.listValues.add((String)pair.getValue());
        });
        if (element.listValues.size() < 2) {
            element.listValues.add("placeholder_value");
        }

        String executableBlockId = serialized.getValue("slider_element_executable_block_identifier");
        if (executableBlockId != null) {
            AbstractExecutableBlock b = ExecutableBlockDeserializer.deserializeWithIdentifier(serialized, executableBlockId);
            if (b instanceof GenericExecutableBlock) {
                GenericExecutableBlock g = (GenericExecutableBlock)b;
                element.executableBlock = g;
            }
        }

        element.tooltip = serialized.getValue("tooltip");
        element.handleTextureNormal = deserializeImageResourceSupplier(serialized.getValue("handle_texture_normal"));
        element.handleTextureHover = deserializeImageResourceSupplier(serialized.getValue("handle_texture_hovered"));
        element.handleTextureInactive = deserializeImageResourceSupplier(serialized.getValue("handle_texture_inactive"));
        element.sliderBackgroundTextureNormal = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_normal"));
        element.sliderBackgroundTextureHighlighted = deserializeImageResourceSupplier(serialized.getValue("slider_background_texture_highlighted"));
        element.restartBackgroundAnimationsOnHover = this.deserializeBoolean(element.restartBackgroundAnimationsOnHover, serialized.getValue("restart_background_animations"));
        element.nineSliceCustomBackground = this.deserializeBoolean(element.nineSliceCustomBackground, serialized.getValue("nine_slice_custom_background"));
        element.nineSliceBorderX = (Integer)this.deserializeNumber(Integer.class, element.nineSliceBorderX, serialized.getValue("nine_slice_border_x"));
        element.nineSliceBorderY = (Integer)this.deserializeNumber(Integer.class, element.nineSliceBorderY, serialized.getValue("nine_slice_border_y"));
        element.nineSliceSliderHandle = this.deserializeBoolean(element.nineSliceSliderHandle, serialized.getValue("nine_slice_slider_handle"));
        element.nineSliceSliderHandleBorderX = (Integer)this.deserializeNumber(Integer.class, element.nineSliceSliderHandleBorderX, serialized.getValue("nine_slice_slider_handle_border_x"));
        element.nineSliceSliderHandleBorderY = (Integer)this.deserializeNumber(Integer.class, element.nineSliceSliderHandleBorderY, serialized.getValue("nine_slice_slider_handle_border_y"));
        element.navigatable = this.deserializeBoolean(element.navigatable, serialized.getValue("navigatable"));
        String activeStateRequirementContainerIdentifier = serialized.getValue("widget_active_state_requirement_container_identifier");
        if (activeStateRequirementContainerIdentifier != null) {
            LoadingRequirementContainer c = LoadingRequirementContainer.deserializeWithIdentifier(activeStateRequirementContainerIdentifier, serialized);
            if (c != null) {
                element.activeStateSupplier = c;
            }
        }

        element.hoverSound = deserializeAudioResourceSupplier(serialized.getValue("hoversound"));
        element.buildSlider();
        element.prepareExecutableBlock();
        return element;
    }

    public @Nullable EnchantmentSliderElement deserializeElementInternal(@NotNull SerializedElement serialized) {
        EnchantmentSliderElement element = (EnchantmentSliderElement)super.deserializeElementInternal(serialized);
        if (element != null) {
            element.prepareLoadingRequirementContainer();
        }

        return element;
    }

    protected SerializedElement serializeElement(@NotNull EnchantmentSliderElement element, @NotNull SerializedElement serializeTo) {
        serializeTo.putProperty("slider_type", element.type.getName());
        serializeTo.putProperty("pre_selected_value", element.preSelectedValue);
        serializeTo.putProperty("slider_label", element.label);
        serializeTo.putProperty("min_range_value", "" + element.minRangeValue);
        serializeTo.putProperty("max_range_value", "" + element.maxRangeValue);
        serializeTo.putProperty("rounding_decimal_place", "" + element.roundingDecimalPlace);
        int i = 0;

        for(Iterator var4 = element.listValues.iterator(); var4.hasNext(); ++i) {
            String s = (String)var4.next();
            serializeTo.putProperty("slider_list_value_" + i, s);
        }

        serializeTo.putProperty("slider_element_executable_block_identifier", element.executableBlock.getIdentifier());
        element.executableBlock.serializeToExistingPropertyContainer(serializeTo);
        serializeTo.putProperty("tooltip", element.tooltip);
        if (element.handleTextureNormal != null) {
            serializeTo.putProperty("handle_texture_normal", element.handleTextureNormal.getSourceWithPrefix());
        }

        if (element.handleTextureHover != null) {
            serializeTo.putProperty("handle_texture_hovered", element.handleTextureHover.getSourceWithPrefix());
        }

        if (element.handleTextureInactive != null) {
            serializeTo.putProperty("handle_texture_inactive", element.handleTextureInactive.getSourceWithPrefix());
        }

        serializeTo.putProperty("restart_background_animations", "" + element.restartBackgroundAnimationsOnHover);
        if (element.sliderBackgroundTextureNormal != null) {
            serializeTo.putProperty("slider_background_texture_normal", element.sliderBackgroundTextureNormal.getSourceWithPrefix());
        }

        if (element.sliderBackgroundTextureHighlighted != null) {
            serializeTo.putProperty("slider_background_texture_highlighted", element.sliderBackgroundTextureHighlighted.getSourceWithPrefix());
        }

        serializeTo.putProperty("nine_slice_custom_background", "" + element.nineSliceCustomBackground);
        serializeTo.putProperty("nine_slice_border_x", "" + element.nineSliceBorderX);
        serializeTo.putProperty("nine_slice_border_y", "" + element.nineSliceBorderY);
        serializeTo.putProperty("nine_slice_slider_handle", "" + element.nineSliceSliderHandle);
        serializeTo.putProperty("nine_slice_slider_handle_border_x", "" + element.nineSliceSliderHandleBorderX);
        serializeTo.putProperty("nine_slice_slider_handle_border_y", "" + element.nineSliceSliderHandleBorderY);
        serializeTo.putProperty("navigatable", "" + element.navigatable);
        serializeTo.putProperty("widget_active_state_requirement_container_identifier", element.activeStateSupplier.identifier);
        element.activeStateSupplier.serializeToExistingPropertyContainer(serializeTo);
        if (element.hoverSound != null) {
            serializeTo.putProperty("hoversound", element.hoverSound.getSourceWithPrefix());
        }

        return serializeTo;
    }

    public @NotNull EnchantmentSliderEditorElement wrapIntoEditorElement(@NotNull EnchantmentSliderElement element, @NotNull LayoutEditorScreen editor) {
        return new EnchantmentSliderEditorElement(element, editor);
    }

    public @NotNull Component getDisplayName(@Nullable AbstractElement element) {
        return Component.translatable("fancymenu.elements.slider.v2");
    }

    public @Nullable Component[] getDescription(@Nullable AbstractElement element) {
        return LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.desc", new String[0]);
    }
}
