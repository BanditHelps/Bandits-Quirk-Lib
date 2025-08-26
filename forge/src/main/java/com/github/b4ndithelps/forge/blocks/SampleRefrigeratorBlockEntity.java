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
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SampleRefrigeratorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, Container {
    public static final int SLOT_COUNT = 18;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SampleRefrigeratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAMPLE_REFRIGERATOR.get(), pos, state);
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
        return Component.translatable("block.bandits_quirk_lib.sample_refrigerator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // Custom menu so we can hard-enforce filtering at the Slot level visually and logically.
        return new AbstractContainerMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x2, id) {
            @Override
            public boolean stillValid(Player player) {
                return SampleRefrigeratorBlockEntity.this.stillValid(player);
            }

            {
                // Fridge inventory 2 rows x 9 (0..17)
                for (int row = 0; row < 2; ++row) {
                    for (int col = 0; col < 9; ++col) {
                        int slotIndex = col + row * 9;
                        this.addSlot(new Slot(SampleRefrigeratorBlockEntity.this, slotIndex, 8 + col * 18, 18 + row * 18) {
                            @Override
                            public boolean mayPlace(ItemStack stack) {
                                return isGeneVial(stack);
                            }
                        });
                    }
                }

                // Player inventory
                for (int row = 0; row < 3; ++row) {
                    for (int col = 0; col < 9; ++col) {
                        this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 50 + row * 18));
                    }
                }
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(inv, col, 8 + col * 18, 108));
                }
            }

            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                ItemStack itemstack = ItemStack.EMPTY;
                Slot slot = this.slots.get(index);
                if (slot != null && slot.hasItem()) {
                    ItemStack stackInSlot = slot.getItem();
                    itemstack = stackInSlot.copy();

                    int fridgeEnd = SLOT_COUNT; // 0..17 are fridge slots
                    int playerStart = fridgeEnd; // start of player inventory in this menu
                    int playerEnd = this.slots.size();

                    if (index < fridgeEnd) {
                        // From fridge to player
                        if (!this.moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // From player to fridge (only gene vials allowed)
                        if (!isGeneVial(stackInSlot)) {
                            return ItemStack.EMPTY;
                        }
                        if (!this.moveItemStackTo(stackInSlot, 0, fridgeEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }

                    if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
                }
                return itemstack;
            }
        };
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
    public boolean canPlaceItem(int index, ItemStack stack) { return isGeneVial(stack); }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) { return isGeneVial(stack); }
    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) { return true; }
}


