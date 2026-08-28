package com.enn3developer.gregcolonies.testing;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class FakeInventory implements IInventory {

    private final ItemStack[] slots;

    private int stackLimit = 64;

    private int dirtied;

    public FakeInventory(int size) {
        slots = new ItemStack[size];
    }

    public FakeInventory with(int slot, ItemStack stack) {
        slots[slot] = stack;
        return this;
    }

    public FakeInventory stackLimit(int limit) {
        stackLimit = limit;
        return this;
    }

    public int getDirtied() {
        return dirtied;
    }

    public int total() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack != null) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = slots[slot];
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            slots[slot] = null;
            return stack;
        }
        ItemStack taken = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            slots[slot] = null;
        }
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        slots[slot] = stack;
    }

    @Override
    public String getInventoryName() {
        return "fake";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return stackLimit;
    }

    @Override
    public void markDirty() {
        dirtied++;
    }

    @Override
    public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
