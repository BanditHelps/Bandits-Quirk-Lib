package com.github.b4ndithelps.forge.fancymenu.elements.enchant_slider;

import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.slider.v2.SliderElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class EnchantmentSliderElement extends SliderElement {

    public Enchantment enchant;

    public EnchantmentSliderElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.maxRangeValue = 15.0;
        this.enchant = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft:sharpness"));
    }

    public void setEnchant(Enchantment enchant) {
        this.enchant = enchant;
        this.maxRangeValue = enchant.getMaxLevel();
    }
}