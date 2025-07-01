package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.fancymenu.elements.IconButtonElementBuilder;
import com.github.b4ndithelps.forge.fancymenu.elements.enchant_slider.EnchantmentSliderElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin( Elements.class)
public class ElementMixin {

    // Create static instances of the new icon element
    private static final IconButtonElementBuilder ICON_BUTTON = new IconButtonElementBuilder();
    private static final EnchantmentSliderElementBuilder ENCHANT_SLIDER = new EnchantmentSliderElementBuilder();

    // Literal voodoo is holding this together... DON'T TOUCH IT!
    @Inject(method = "registerAll()V", at = @At("TAIL"), remap = false)
    private static void onRegisterAll(CallbackInfo info) {
        System.out.println("RUNNING THE REGISTER ALL MIXIN!");
        try {
            // Custom button element
            ElementRegistry.register(ICON_BUTTON);
            ElementRegistry.register(ENCHANT_SLIDER);

            BanditsQuirkLibForge.LOGGER.info("MIXIN: Elements Registered");

        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to register custom FancyMenu element: " + e.getMessage());
        }
    }

}
