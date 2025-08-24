package com.github.b4ndithelps.forge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReadoutItem extends Item {
    public ReadoutItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var tag = stack.getTag();
        if (tag != null) {
            String s = tag.getString("EncodedSequence");
            if (!s.isEmpty()) tooltip.add(Component.literal("Readout: " + s));
            int q = tag.getInt("Quality");
            tooltip.add(Component.literal("Quality Offset: " + (q - 76) + "%"));
        }
        tooltip.add(Component.literal("Complex readouts require translation."));
    }
}


