package com.github.b4ndithelps.forge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SequencedSampleItem extends Item {
    public SequencedSampleItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var tag = stack.getTag();
        if (tag != null) {
            tooltip.add(Component.literal("Sequenced Sample"));
            int q = tag.getInt("Quality");
            if (q > 0) tooltip.add(Component.literal("Quality: " + q + "%"));
        }
        tooltip.add(Component.literal("Break down DNA samples into usable genetic data."));
    }
}


