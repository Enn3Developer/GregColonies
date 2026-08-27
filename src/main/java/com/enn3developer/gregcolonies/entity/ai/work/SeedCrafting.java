package com.enn3developer.gregcolonies.entity.ai.work;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.enn3developer.gregcolonies.entity.CitizenInventory;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public final class SeedCrafting {

    private static final int GRID = 3;

    private static final int SLOTS = GRID * GRID;

    private static final int MAX_CRAFTS = 64;

    private static final Container HOLDER = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    };

    private static List<IRecipe> seedRecipes;

    private static int knownRecipes;

    private SeedCrafting() {}

    public static int craft(EntityCitizen citizen, Block wanted, int x, int y, int z, int target) {
        World world = citizen.worldObj;
        CitizenInventory inventory = citizen.getInventory();
        int made = 0;
        int stored = inventory.countMain(stack -> Crops.isSeedFor(stack, world, x, y, z, wanted));
        for (int pass = 0; pass < MAX_CRAFTS && stored < target; pass++) {
            ItemStack seeds = craftOnce(citizen, wanted, x, y, z);
            if (seeds == null) {
                break;
            }
            made += seeds.stackSize;
            int now = inventory.countMain(stack -> Crops.isSeedFor(stack, world, x, y, z, wanted));
            if (now <= stored) {
                break;
            }
            stored = now;
        }
        return made;
    }

    public static boolean isSeedSource(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || Crops.isSeed(stack)) {
            return false;
        }
        for (IRecipe recipe : recipes()) {
            for (Object pattern : inputs(recipe)) {
                if (pattern != null && matches(pattern, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ItemStack craftOnce(EntityCitizen citizen, Block wanted, int x, int y, int z) {
        ItemStackHandler main = citizen.getInventory()
            .getMain();
        for (IRecipe recipe : recipes()) {
            if (!Crops.isSeedFor(recipe.getRecipeOutput(), citizen.worldObj, x, y, z, wanted)) {
                continue;
            }
            Object[] input = inputs(recipe);
            int[] sources = new int[input.length];
            if (!select(main, input, sources)) {
                continue;
            }
            for (int width = 0; width <= GRID; width++) {
                InventoryCrafting table = new InventoryCrafting(HOLDER, GRID, GRID);
                if (!place(table, main, input, sources, width) || !recipe.matches(table, citizen.worldObj)) {
                    continue;
                }
                ItemStack result = recipe.getCraftingResult(table);
                if (!Crops.isSeedFor(result, citizen.worldObj, x, y, z, wanted)) {
                    break;
                }
                consume(citizen, main, sources);
                store(citizen, result);
                return result;
            }
        }
        return null;
    }

    private static boolean select(ItemStackHandler main, Object[] input, int[] sources) {
        int[] used = new int[main.getSlots()];
        for (int i = 0; i < input.length; i++) {
            sources[i] = -1;
            if (input[i] == null) {
                continue;
            }
            for (int slot = 0; slot < main.getSlots(); slot++) {
                ItemStack stack = main.getStackInSlot(slot);
                if (stack == null || stack.stackSize - used[slot] <= 0 || !matches(input[i], stack)) {
                    continue;
                }
                used[slot]++;
                sources[i] = slot;
                break;
            }
            if (sources[i] < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean place(InventoryCrafting table, ItemStackHandler main, Object[] input, int[] sources,
        int width) {
        int compact = 0;
        for (int i = 0; i < input.length; i++) {
            if (sources[i] < 0) {
                continue;
            }
            int cell;
            if (width == 0) {
                cell = compact++;
            } else {
                int row = i / width;
                int column = i % width;
                if (row >= GRID || column >= GRID) {
                    return false;
                }
                cell = row * GRID + column;
            }
            if (cell >= SLOTS) {
                return false;
            }
            ItemStack one = main.getStackInSlot(sources[i])
                .copy();
            one.stackSize = 1;
            table.setInventorySlotContents(cell, one);
        }
        return true;
    }

    private static void consume(EntityCitizen citizen, ItemStackHandler main, int[] sources) {
        for (int source : sources) {
            if (source < 0) {
                continue;
            }
            ItemStack taken = main.extractItem(source, 1, false);
            if (taken == null) {
                continue;
            }
            if (taken.getItem()
                .hasContainerItem(taken)) {
                ItemStack container = taken.getItem()
                    .getContainerItem(taken);
                if (container != null) {
                    store(citizen, container);
                }
            }
        }
    }

    private static void store(EntityCitizen citizen, ItemStack stack) {
        ItemStack rest = citizen.getInventory()
            .store(stack.copy());
        if (rest != null) {
            citizen.entityDropItem(rest, 0.0F);
        }
    }

    private static boolean matches(Object pattern, ItemStack stack) {
        if (pattern instanceof ItemStack) {
            ItemStack wanted = (ItemStack) pattern;
            return wanted.getItem() == stack.getItem() && (wanted.getItemDamage() == OreDictionary.WILDCARD_VALUE
                || wanted.getItemDamage() == stack.getItemDamage());
        }
        if (pattern instanceof List) {
            for (Object option : (List<?>) pattern) {
                if (option instanceof ItemStack && matches(option, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object[] inputs(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) {
            return ((ShapedRecipes) recipe).recipeItems;
        }
        if (recipe instanceof ShapelessRecipes) {
            return ((ShapelessRecipes) recipe).recipeItems.toArray();
        }
        if (recipe instanceof ShapedOreRecipe) {
            return ((ShapedOreRecipe) recipe).getInput();
        }
        if (recipe instanceof ShapelessOreRecipe) {
            return ((ShapelessOreRecipe) recipe).getInput()
                .toArray();
        }
        return null;
    }

    private static List<IRecipe> recipes() {
        List<IRecipe> all = CraftingManager.getInstance()
            .getRecipeList();
        if (seedRecipes != null && knownRecipes == all.size()) {
            return seedRecipes;
        }
        knownRecipes = all.size();
        List<IRecipe> found = new ArrayList<>();
        for (IRecipe recipe : all) {
            if (!Crops.isSeed(recipe.getRecipeOutput())) {
                continue;
            }
            Object[] input = inputs(recipe);
            if (input == null || input.length == 0 || input.length > SLOTS) {
                continue;
            }
            found.add(recipe);
        }
        seedRecipes = found;
        return seedRecipes;
    }
}
