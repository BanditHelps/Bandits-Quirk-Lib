package com.github.b4ndithelps.forge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TissueSampleItem extends Item {
    public TissueSampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag()) {
            var tag = stack.getTag();
            if (tag != null) {
                String name = tag.getString("EntityName");
                int quality = tag.getInt("Quality");
                if (!name.isEmpty()) {
                    tooltip.add(Component.literal("Tissue Sample: " + name));
                }
                if (quality > 0) {
                    tooltip.add(Component.literal("Quality: " + quality + "%"));
                }
                tooltip.add(Component.literal("Source Genome Stable"));
            }
        }
    }
}


