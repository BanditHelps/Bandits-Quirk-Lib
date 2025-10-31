package com.github.b4ndithelps.forge.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.github.b4ndithelps.forge.blocks.BioTerminalRefBlockEntity;

import java.util.HashSet;
import java.util.Set;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.genetics.Gene;

/**
 * Portable database that stores which gene types have been identified.
 * Data is stored on the item NBT under a string list tag "known_genes".
 */
@SuppressWarnings("removal")
public class GeneDatabaseItem extends Item {
    public static final String TAG_KNOWN_GENES = "known_genes";

    public GeneDatabaseItem(Properties properties) {
        super(properties);
    }

    public static boolean isKnown(ItemStack stack, ResourceLocation geneId) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof GeneDatabaseItem)) return false;
        if (geneId == null) return false;
        var tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_KNOWN_GENES, 9)) return false;
        var list = tag.getList(TAG_KNOWN_GENES, 8);
        for (int i = 0; i < list.size(); i++) {
            String stored = list.getString(i);
            if (geneId.toString().equals(stored)) return true;
            try {
                ResourceLocation storedRl = new ResourceLocation(stored);
                if (storedRl.getPath().equals(geneId.getPath())) return true;
            } catch (Exception ignored) {
                // Fallback: compare by path substring
                if (stored.endsWith(geneId.getPath())) return true;
            }
        }
        return false;
    }

    public static void addKnown(ItemStack stack, ResourceLocation geneId) {
        if (stack == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof GeneDatabaseItem)) return;
        if (geneId == null) return;
        // Normalize to a canonical ID if possible (match by path in registry)
        ResourceLocation toStore = geneId;
        try {
            String path = geneId.getPath();
            for (Gene g : GeneRegistry.all()) {
                if (g.getId().getPath().equals(path)) { toStore = g.getId(); break; }
            }
        } catch (Exception ignored) {}
        var tag = stack.getOrCreateTag();
        net.minecraft.nbt.ListTag list = tag.contains(TAG_KNOWN_GENES, 9) ? tag.getList(TAG_KNOWN_GENES, 8) : new net.minecraft.nbt.ListTag();
        boolean present = false;
        for (int i = 0; i < list.size(); i++) {
            if (toStore.toString().equals(list.getString(i))) { present = true; break; }
        }
        if (!present) {
            list.add(net.minecraft.nbt.StringTag.valueOf(toStore.toString()));
            tag.put(TAG_KNOWN_GENES, list);
        }
    }

    public static Set<String> getKnownIds(ItemStack stack) {
        Set<String> out = new HashSet<>();
        if (stack == null || stack.isEmpty()) return out;
        if (!(stack.getItem() instanceof GeneDatabaseItem)) return out;
        var tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_KNOWN_GENES, 9)) return out;
        var list = tag.getList(TAG_KNOWN_GENES, 8);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isCrouching()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BioTerminalRefBlockEntity refTerm) {
            ItemStack slot = refTerm.getItem(BioTerminalRefBlockEntity.SLOT_DISK);
            if (slot.isEmpty()) {
                ItemStack held = context.getItemInHand();
                if (!held.isEmpty() && held.getItem() instanceof GeneDatabaseItem) {
                    ItemStack insert = held.copy();
                    insert.setCount(1);
                    refTerm.setItem(BioTerminalRefBlockEntity.SLOT_DISK, insert);
                    refTerm.setChanged();
                    if (!level.isClientSide) {
                        held.shrink(1);
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }
}


