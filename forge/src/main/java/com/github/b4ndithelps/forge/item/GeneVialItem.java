package com.github.b4ndithelps.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GeneVialItem extends Item {
    public enum Category {
        COSMETIC,
        RESISTANCE,
        BUILDER,
        QUIRK
    }

    private final Category category;

    public GeneVialItem(Properties properties, Category category) {
        super(properties);
        this.category = category;
    }

    public Category getCategory() { return category; }

    @Override
    public Component getName(ItemStack stack) {
        // Single shared display name "Gene Vial" with category-specific color
        ChatFormatting color = switch (category) {
            case COSMETIC -> ChatFormatting.AQUA;
            case RESISTANCE -> ChatFormatting.DARK_GREEN;
            case BUILDER -> ChatFormatting.GOLD;
            case QUIRK -> ChatFormatting.RED;
        };
        return Component.translatable("item.bandits_quirk_lib.gene_vial").withStyle(color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component catText = switch (category) {
            case COSMETIC -> Component.translatable("category.bandits_quirk_lib.cosmetic");
            case RESISTANCE -> Component.translatable("category.bandits_quirk_lib.resistance");
            case BUILDER -> Component.translatable("category.bandits_quirk_lib.builder");
            case QUIRK -> Component.translatable("category.bandits_quirk_lib.quirk");
        };
        tooltip.add(Component.translatable("tooltip.bandits_quirk_lib.gene_vial.category", catText).withStyle(ChatFormatting.GRAY));
    }
}


