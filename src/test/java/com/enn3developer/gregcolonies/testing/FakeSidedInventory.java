package com.enn3developer.gregcolonies.testing;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

public class FakeSidedInventory extends FakeInventory implements ISidedInventory {

    private final int[][] slotsPerSide = new int[6][];

    private boolean allowExtract = true;

    private boolean allowInsert = true;

    public FakeSidedInventory(int size) {
        super(size);
        for (int side = 0; side < 6; side++) {
            slotsPerSide[side] = new int[0];
        }
    }

    public FakeSidedInventory expose(int side, int... slots) {
        slotsPerSide[side] = slots;
        return this;
    }

    public FakeSidedInventory noExtract() {
        allowExtract = false;
        return this;
    }

    public FakeSidedInventory noInsert() {
        allowInsert = false;
        return this;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return slotsPerSide[side];
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return allowInsert;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return allowExtract;
    }
}
