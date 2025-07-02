package com.github.b4ndithelps.forge.fancymenu.elements.enchant_slider;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.slider.v2.SliderEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class EnchantmentSliderEditorElement extends SliderEditorElement {
    public EnchantmentSliderEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {
        super.init();
        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "enchantment_id", EnchantmentSliderEditorElement.class,
                        consumes -> {
                            EnchantmentSliderElement enchantElement = consumes.getElement();
                            if (enchantElement.enchant != null) {
                                ResourceLocation enchantLoc = ForgeRegistries.ENCHANTMENTS.getKey(enchantElement.enchant);
                                return enchantLoc != null ? enchantLoc.toString() : "";
                            }
                            return "";
                        },
                        (enchantSliderEditorElement, s) -> {
                            EnchantmentSliderElement enchantElement = enchantSliderEditorElement.getElement();
                            if (s != null && !s.trim().isEmpty()) {
                                try {
                                    ResourceLocation enchantLoc = new ResourceLocation(s.trim());
                                    Enchantment enchant = ForgeRegistries.ENCHANTMENTS.getValue(enchantLoc);
                                    if (enchant != null) {
                                        enchantElement.setEnchant(enchant);
                                        // Update the label to reflect the new enchantment
                                        enchantElement.label = Component.translatable(enchant.getDescriptionId()).getString() + ": $$value";
                                        // Rebuild the slider to apply changes
                                        enchantElement.buildSlider();
                                    }
                                } catch (Exception e) {
                                    // Invalid enchantment ID, ignore
                                }
                            }
                        },
                        null, false, true, Component.translatable("banditsquirklib.gui.elements.enchant_slider.enchantment_id"),
                        true, "minecraft:sharpness", null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.enchant_slider.enchantment_id.desc")));

    }

    @Override
    public EnchantmentSliderElement getElement() {
        return (EnchantmentSliderElement) super.getElement();
    }
}
