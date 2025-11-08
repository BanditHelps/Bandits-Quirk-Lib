package com.github.b4ndithelps.forge.item;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Injector item that stores a composed genome built from multiple gene vials.
 * NBT structure:
 *  - genome: ListTag of CompoundTag entries with keys: id (String), quality (int), name (String, optional)
 *  - source: optional metadata (e.g., entity_name, entity_uuid)
 */
public class InjectorItem extends Item {
    public InjectorItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        tooltip.add(Component.translatable("item.bandits_quirk_lib.injector.desc").withStyle(ChatFormatting.GRAY));
        if (tag == null) return;
        if (tag.contains("genome", 9)) {
            ListTag genome = tag.getList("genome", 10);
            tooltip.add(Component.literal("Genome: " + genome.size() + " gene(s)").withStyle(ChatFormatting.WHITE));
            int shown = 0;
            for (int i = 0; i < genome.size(); i++) {
                if (shown >= 6) { // avoid overly long tooltips
                    tooltip.add(Component.literal("…"));
                    break;
                }
                CompoundTag g = genome.getCompound(i);
                String name = g.contains("name", 8) ? g.getString("name") : g.getString("id");
                int q = g.contains("quality", 3) ? g.getInt("quality") : 0;
                tooltip.add(Component.literal(" - " + name + (q > 0 ? " (" + q + "%)" : ""))
                        .withStyle(ChatFormatting.YELLOW));
                shown++;
            }
        }
        if (tag.contains("entity_name", 8)) {
            tooltip.add(Component.literal("From: " + tag.getString("entity_name")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getTag();
        if (!level.isClientSide && tag != null && tag.contains("genome", 9)) {
            ListTag genome = tag.getList("genome", 10);
            // Apply genes to player's genome capability
            for (int i = 0; i < genome.size(); i++) {
                net.minecraft.nbt.CompoundTag g = genome.getCompound(i);
                GenomeHelper.addGene(player, g);
            }
            // Consume injector
            stack.shrink(1);
            if (player instanceof ServerPlayer sp) {
                player.displayClientMessage(Component.literal("Injector applied: " + genome.size() + " genes"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}



