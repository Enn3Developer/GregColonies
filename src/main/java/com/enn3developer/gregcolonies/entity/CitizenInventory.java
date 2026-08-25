package com.enn3developer.gregcolonies.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;

public class CitizenInventory {

    public static final int FOOD_SLOTS = 3;

    public static final int TOOL_SLOTS = 1;

    public static final int MAIN_SLOTS = 9;

    private final ItemStackHandler food = new ItemStackHandler(FOOD_SLOTS) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFood(stack);
        }
    };

    private final ItemStackHandler tool = new ItemStackHandler(TOOL_SLOTS) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isTool(stack);
        }
    };

    private final ItemStackHandler main = new ItemStackHandler(MAIN_SLOTS);

    public ItemStackHandler getFood() {
        return food;
    }

    public ItemStackHandler getTool() {
        return tool;
    }

    public ItemStackHandler getMain() {
        return main;
    }

    public ItemStack getHeldTool() {
        return tool.getStackInSlot(0);
    }

    public static boolean isFood(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFood;
    }

    public static boolean isTool(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemTool || item instanceof ItemSword || item instanceof ItemHoe) {
            return true;
        }
        Set<String> toolClasses = item.getToolClasses(stack);
        return toolClasses != null && !toolClasses.isEmpty();
    }

    public List<ItemStack> takeAll() {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStackHandler handler : new ItemStackHandler[] { food, tool, main }) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack != null) {
                    stacks.add(stack);
                    handler.setStackInSlot(i, null);
                }
            }
        }
        return stacks;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setTag("food", food.serializeNBT());
        tag.setTag("tool", tool.serializeNBT());
        tag.setTag("main", main.serializeNBT());
    }

    public void readFromNBT(NBTTagCompound tag) {
        food.deserializeNBT(tag.getCompoundTag("food"));
        tool.deserializeNBT(tag.getCompoundTag("tool"));
        main.deserializeNBT(tag.getCompoundTag("main"));
    }
}
