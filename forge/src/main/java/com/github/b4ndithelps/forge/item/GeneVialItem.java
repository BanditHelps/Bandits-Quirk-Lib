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
        LOWEND,
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
        // Base display name with category-specific color, plus cryptic label if available
        ChatFormatting color = switch (category) {
            case COSMETIC -> ChatFormatting.AQUA;
            case RESISTANCE -> ChatFormatting.DARK_GREEN;
            case BUILDER -> ChatFormatting.GOLD;
            case LOWEND -> ChatFormatting.LIGHT_PURPLE;
            case QUIRK -> ChatFormatting.RED;
        };
        Component base = Component.translatable("item.bandits_quirk_lib.gene_vial").withStyle(color);
        String label = null;
        var tag = stack.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            var g = tag.getCompound("gene");
            label = g.contains("name", 8) ? g.getString("name") : "";
            if (label == null || label.isEmpty()) {
                String id = g.contains("id", 8) ? g.getString("id") : "";
                int q = g.contains("quality", 3) ? g.getInt("quality") : 0;
                if (!id.isEmpty()) label = compactLabelFromId(id, q);
            }
        }
        if (label != null && !label.isEmpty()) {
            return base.copy().append(Component.literal(" (" + label + ")"));
        }
        return base;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component catText = switch (category) {
            case COSMETIC -> Component.translatable("category.bandits_quirk_lib.cosmetic");
            case RESISTANCE -> Component.translatable("category.bandits_quirk_lib.resistance");
            case BUILDER -> Component.translatable("category.bandits_quirk_lib.builder");
            case LOWEND -> Component.translatable("category.bandits_quirk_lib.lowend");
            case QUIRK -> Component.translatable("category.bandits_quirk_lib.quirk");
        };
        tooltip.add(Component.translatable("tooltip.bandits_quirk_lib.gene_vial.category", catText).withStyle(ChatFormatting.GRAY));

        // Show cryptic gene label if present in NBT (e.g., gene_21f5)
        var tag = stack.getTag();
        if (tag != null && tag.contains("gene", 10)) {
            var g = tag.getCompound("gene");
            String label = g.contains("name", 8) ? g.getString("name") : "";
            if (label == null || label.isEmpty()) {
                String id = g.contains("id", 8) ? g.getString("id") : "";
                int q = g.contains("quality", 3) ? g.getInt("quality") : 0;
                if (!id.isEmpty()) label = compactLabelFromId(id, q);
            }
            if (label != null && !label.isEmpty()) {
                tooltip.add(Component.literal(label).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private String compactLabelFromId(String id, int quality) {
        return "gene_" + String.format("%04x", Math.abs((id + "_" + quality).hashCode()) & 0xFFFF);
    }
}


