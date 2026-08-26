package com.enn3developer.gregcolonies.entity.ai.work;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class Inventories {

    private static final int SIDES = 6;

    private Inventories() {}

    public static IInventory at(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return null;
        }
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockChest) {
            IInventory chest = ((BlockChest) block).func_149951_m(world, x, y, z);
            if (chest != null) {
                return chest;
            }
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof IInventory ? (IInventory) tile : null;
    }

    public static ItemStack extract(IInventory inventory, Predicate<ItemStack> filter, int amount) {
        if (inventory == null || amount <= 0) {
            return null;
        }
        if (inventory instanceof ISidedInventory) {
            return extractSided((ISidedInventory) inventory, filter, amount);
        }
        int size = inventory.getSizeInventory();
        for (int slot = 0; slot < size; slot++) {
            ItemStack taken = extractFrom(inventory, slot, filter, amount);
            if (taken != null) {
                return taken;
            }
        }
        return null;
    }

    private static ItemStack extractSided(ISidedInventory inventory, Predicate<ItemStack> filter, int amount) {
        for (int side = 0; side < SIDES; side++) {
            int[] slots = inventory.getAccessibleSlotsFromSide(side);
            if (slots == null) {
                continue;
            }
            for (int slot : slots) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack == null || !inventory.canExtractItem(slot, stack, side)) {
                    continue;
                }
                ItemStack taken = extractFrom(inventory, slot, filter, amount);
                if (taken != null) {
                    return taken;
                }
            }
        }
        return null;
    }

    private static ItemStack extractFrom(IInventory inventory, int slot, Predicate<ItemStack> filter, int amount) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack == null || stack.stackSize <= 0 || !filter.test(stack)) {
            return null;
        }
        ItemStack taken = inventory.decrStackSize(slot, Math.min(amount, stack.stackSize));
        return taken == null || taken.stackSize <= 0 ? null : taken;
    }

    public static ItemStack insert(IInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.stackSize <= 0) {
            return stack;
        }
        if (inventory instanceof ISidedInventory) {
            return insertSided((ISidedInventory) inventory, stack);
        }
        int size = inventory.getSizeInventory();
        for (int pass = 0; pass < 2 && stack != null; pass++) {
            for (int slot = 0; slot < size && stack != null; slot++) {
                stack = insertInto(inventory, slot, stack, pass == 0);
            }
        }
        return stack;
    }

    private static ItemStack insertSided(ISidedInventory inventory, ItemStack stack) {
        for (int side = 0; side < SIDES && stack != null; side++) {
            int[] slots = inventory.getAccessibleSlotsFromSide(side);
            if (slots == null) {
                continue;
            }
            for (int pass = 0; pass < 2 && stack != null; pass++) {
                for (int slot : slots) {
                    if (!inventory.canInsertItem(slot, stack, side)) {
                        continue;
                    }
                    stack = insertInto(inventory, slot, stack, pass == 0);
                    if (stack == null) {
                        break;
                    }
                }
            }
        }
        return stack;
    }

    private static ItemStack insertInto(IInventory inventory, int slot, ItemStack stack, boolean mergeOnly) {
        if (!inventory.isItemValidForSlot(slot, stack)) {
            return stack;
        }
        int limit = Math.min(inventory.getInventoryStackLimit(), stack.getMaxStackSize());
        ItemStack existing = inventory.getStackInSlot(slot);

        if (existing == null) {
            if (mergeOnly) {
                return stack;
            }
            ItemStack moved = stack.copy();
            moved.stackSize = Math.min(limit, stack.stackSize);
            inventory.setInventorySlotContents(slot, moved);
            stack.stackSize -= moved.stackSize;
            return stack.stackSize <= 0 ? null : stack;
        }

        if (!canMerge(existing, stack)) {
            return stack;
        }
        int space = limit - existing.stackSize;
        if (space <= 0) {
            return stack;
        }
        int moved = Math.min(space, stack.stackSize);
        existing.stackSize += moved;
        stack.stackSize -= moved;
        return stack.stackSize <= 0 ? null : stack;
    }

    private static boolean canMerge(ItemStack existing, ItemStack stack) {
        return existing.isStackable() && existing.getItem() == stack.getItem()
            && existing.getItemDamage() == stack.getItemDamage()
            && ItemStack.areItemStackTagsEqual(existing, stack);
    }
}
