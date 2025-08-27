package com.github.b4ndithelps.forge.blocks;

import com.github.b4ndithelps.forge.item.GeneVialItem;
import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BioPrinterBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, Container {
    public static final int SLOT_INPUT_COUNT = 12;
    public static final int SLOT_OUTPUT = 12; // index for output
    public static final int SLOT_COUNT = SLOT_INPUT_COUNT + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public BioPrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_PRINTER.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bandits_quirk_lib.bio_printer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BioPrinterMenu(id, inv, this);
    }

    private static boolean isGeneVial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        return item instanceof GeneVialItem
                || item == ModItems.GENE_VIAL_COSMETIC.get()
                || item == ModItems.GENE_VIAL_RESISTANCE.get()
                || item == ModItems.GENE_VIAL_BUILDER.get()
                || item == ModItems.GENE_VIAL_QUIRK.get();
    }

    // Container impl
    @Override
    public int getContainerSize() { return items.size(); }
    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }
    @Override
    public ItemStack removeItem(int slot, int amount) { ItemStack r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); }
    @Override
    public boolean stillValid(Player player) { return player.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64.0; }
    @Override
    public void clearContent() { items.clear(); }

    // WorldlyContainer filtering for hoppers etc.
    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] arr = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) arr[i] = i;
        return arr;
    }
    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == SLOT_OUTPUT) return false;
        return isGeneVial(stack);
    }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        if (index == SLOT_OUTPUT) return false;
        return isGeneVial(stack);
    }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return true; }
}


