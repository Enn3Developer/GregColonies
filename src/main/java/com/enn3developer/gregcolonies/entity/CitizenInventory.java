package com.enn3developer.gregcolonies.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.enn3developer.gregcolonies.compat.Mods;
import com.enn3developer.gregcolonies.compat.TinkersTools;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenInventory {

    public static final int ARMOR_SLOTS = 4;

    public static final int FOOD_SLOTS = 3;

    public static final int TOOL_SLOTS = 1;

    public static final int MAIN_SLOTS = 9;

    private final Entity owner;

    private final ItemStackHandler armor = new ItemStackHandler(ARMOR_SLOTS) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isArmor(stack, slot, owner);
        }
    };

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

    public CitizenInventory(Entity owner) {
        this.owner = owner;
    }

    public ItemStackHandler getArmor() {
        return armor;
    }

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

    public static boolean isArmor(ItemStack stack, int slot, Entity wearer) {
        if (stack == null) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemArmor) {
            return ((ItemArmor) item).armorType == slot;
        }
        return item.isValidArmor(stack, slot, wearer);
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
        if (Mods.tinkers() && TinkersTools.isTool(stack)) {
            return !TinkersTools.isBroken(stack);
        }
        Set<String> toolClasses = item.getToolClasses(stack);
        return toolClasses != null && !toolClasses.isEmpty();
    }

    public boolean canStore(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return true;
        }
        ItemStackHandler probe = new ItemStackHandler(MAIN_SLOTS);
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            probe.setStackInSlot(i, stack == null ? null : stack.copy());
        }
        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            ItemStack rest = stack.copy();
            for (int i = 0; i < MAIN_SLOTS && rest != null; i++) {
                rest = probe.insertItem(i, rest, false);
            }
            if (rest != null) {
                return false;
            }
        }
        return true;
    }

    public ItemStack takeScaffold() {
        return takeMain(WorkBlocks::isScaffold);
    }

    public ItemStack takeMain(Predicate<ItemStack> filter) {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (!filter.test(stack)) {
                continue;
            }
            ItemStack taken = main.extractItem(i, 1, false);
            if (taken != null && taken.stackSize > 0) {
                return taken;
            }
        }
        return null;
    }

    public ItemStack peekMain(Predicate<ItemStack> filter) {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack != null && filter.test(stack)) {
                return stack.copy();
            }
        }
        return null;
    }

    public int countMain(Predicate<ItemStack> filter) {
        int count = 0;
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack != null && filter.test(stack)) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public boolean hasMain(Predicate<ItemStack> filter) {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            if (filter.test(main.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    public int countFood() {
        int count = 0;
        for (int i = 0; i < FOOD_SLOTS; i++) {
            ItemStack stack = food.getStackInSlot(i);
            if (stack != null) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public int stockFood(IInventory source, int target) {
        int moved = 0;
        for (int pull = 0; pull < FOOD_SLOTS * 2; pull++) {
            int missing = target - countFood();
            if (missing <= 0) {
                break;
            }
            ItemStack taken = Inventories.extract(source, CitizenInventory::isFood, missing);
            if (taken == null) {
                break;
            }
            int size = taken.stackSize;
            ItemStack rest = taken;
            for (int i = 0; i < FOOD_SLOTS && rest != null; i++) {
                rest = food.insertItem(i, rest, false);
            }
            moved += size - (rest == null ? 0 : rest.stackSize);
            if (rest != null) {
                rest = Inventories.insert(source, rest);
                if (rest != null) {
                    owner.entityDropItem(rest, 0.0F);
                }
                break;
            }
        }
        if (moved > 0) {
            source.markDirty();
        }
        return moved;
    }

    public boolean hasFreeMainSlot() {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            if (main.getStackInSlot(i) == null) {
                return true;
            }
        }
        return false;
    }

    public int deposit(IInventory target) {
        int moved = 0;
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            int before = stack.stackSize;
            ItemStack rest = Inventories.insert(target, stack);
            if (rest == null) {
                main.setStackInSlot(i, null);
                moved += before;
            } else {
                main.setStackInSlot(i, rest);
                moved += before - rest.stackSize;
            }
        }
        if (moved > 0) {
            target.markDirty();
        }
        return moved;
    }

    public int deposit(IInventory target, Predicate<ItemStack> keep, int reserve) {
        int moved = 0;
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            int held = keep.test(stack) ? Math.min(Math.max(reserve - countBefore(i, stack), 0), stack.stackSize) : 0;
            if (held >= stack.stackSize) {
                continue;
            }
            int before = stack.stackSize;
            int size = before - held;
            ItemStack out = stack.copy();
            out.stackSize = size;
            ItemStack rest = Inventories.insert(target, out);
            int sent = size - (rest == null ? 0 : rest.stackSize);
            if (sent <= 0) {
                continue;
            }
            moved += sent;
            stack.stackSize = before - sent;
            main.setStackInSlot(i, stack.stackSize <= 0 ? null : stack);
        }
        if (moved > 0) {
            target.markDirty();
        }
        return moved;
    }

    public boolean hasExcess(Predicate<ItemStack> keep, int reserve) {
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            if (!keep.test(stack) || countBefore(i, stack) + stack.stackSize > reserve) {
                return true;
            }
        }
        return false;
    }

    private int countBefore(int slot, ItemStack like) {
        int count = 0;
        for (int i = 0; i < slot; i++) {
            ItemStack stack = main.getStackInSlot(i);
            if (stack != null && stack.getItem() == like.getItem() && stack.getItemDamage() == like.getItemDamage()) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public ItemStack store(ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < MAIN_SLOTS && rest != null; i++) {
            rest = main.insertItem(i, rest, false);
        }
        return rest;
    }

    public List<ItemStack> takeAll() {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStackHandler handler : new ItemStackHandler[] { armor, food, tool, main }) {
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
        tag.setTag("armor", armor.serializeNBT());
        tag.setTag("food", food.serializeNBT());
        tag.setTag("tool", tool.serializeNBT());
        tag.setTag("main", main.serializeNBT());
    }

    public void readFromNBT(NBTTagCompound tag) {
        armor.deserializeNBT(tag.getCompoundTag("armor"));
        food.deserializeNBT(tag.getCompoundTag("food"));
        tool.deserializeNBT(tag.getCompoundTag("tool"));
        main.deserializeNBT(tag.getCompoundTag("main"));
    }
}
