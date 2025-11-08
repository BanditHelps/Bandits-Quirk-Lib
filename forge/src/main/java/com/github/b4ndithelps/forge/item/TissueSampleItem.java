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
        if (!stack.hasTag()) return;
        var tag = stack.getTag();
        if (tag == null) return;

        String name = tag.getString("entity_name");
        String uuid = tag.getString("entity_uuid");
        if (!name.isEmpty()) {
            tooltip.add(Component.literal("Sample from " + name + (uuid.isEmpty() ? "" : " [#" + uuid + "]")));
        }
        tooltip.add(Component.literal("Genetic Material Stored"));

        if (flag.isAdvanced()) {
            var list = tag.getList("genes", 10); // 10 = CompoundTag
            int max = Math.min(4, list.size());
            for (int i = 0; i < max; i++) {
                var g = list.getCompound(i);
                String id = g.getString("id");
                int q = g.getInt("quality");
                tooltip.add(Component.literal(" - " + id + " (" + q + ")"));
            }
            if (list.size() > max) {
                tooltip.add(Component.literal(" - ..."));
            }
        }
    }
}