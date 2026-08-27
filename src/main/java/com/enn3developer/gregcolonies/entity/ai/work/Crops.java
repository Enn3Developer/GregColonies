package com.enn3developer.gregcolonies.entity.ai.work;

import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStem;
import net.minecraft.block.IGrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

public final class Crops {

    private static final int BONEMEAL_META = 15;

    private static final int BONEMEAL_EFFECT = 2005;

    private Crops() {}

    public static boolean isSoil(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return false;
        }
        return block.canSustainPlant(world, x, y, z, ForgeDirection.UP, (IPlantable) Items.wheat_seeds);
    }

    public static boolean isTillable(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block == Blocks.grass || block == Blocks.dirt) && world.isAirBlock(x, y + 1, z);
    }

    public static boolean isHoe(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof ItemHoe) {
            return true;
        }
        Set<String> classes = item.getToolClasses(stack);
        return classes != null && classes.contains("hoe");
    }

    public static boolean till(World world, int x, int y, int z) {
        if (!isTillable(world, x, y, z)) {
            return false;
        }
        world.setBlock(x, y, z, Blocks.farmland, 0, 3);
        world.playSoundEffect(
            x + 0.5D,
            y + 0.5D,
            z + 0.5D,
            Blocks.farmland.stepSound.func_150496_b(),
            (Blocks.farmland.stepSound.getVolume() + 1.0F) / 2.0F,
            Blocks.farmland.stepSound.getPitch() * 0.8F);
        return true;
    }

    public static boolean isCrop(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block instanceof IGrowable && !(block instanceof BlockStem) && isSoil(world, x, y - 1, z);
    }

    public static boolean isMature(World world, int x, int y, int z) {
        return isCrop(world, x, y, z) && !((IGrowable) world.getBlock(x, y, z)).func_149851_a(world, x, y, z, false);
    }

    public static boolean canGrow(World world, int x, int y, int z) {
        return isCrop(world, x, y, z) && ((IGrowable) world.getBlock(x, y, z)).func_149851_a(world, x, y, z, false);
    }

    public static boolean isProduce(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == Blocks.melon_block || block == Blocks.pumpkin;
    }

    public static boolean isSeed(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() instanceof IPlantable;
    }

    public static Block plantOf(ItemStack stack, World world, int x, int y, int z) {
        if (!isSeed(stack)) {
            return null;
        }
        Block plant = ((IPlantable) stack.getItem()).getPlant(world, x, y, z);
        return plant == null || plant == Blocks.air ? null : plant;
    }

    public static boolean isSeedFor(ItemStack stack, World world, int x, int y, int z, Block wanted) {
        Block plant = plantOf(stack, world, x, y, z);
        return plant != null && (wanted == null || plant == wanted);
    }

    public static boolean isBonemeal(ItemStack stack) {
        return stack != null && stack.stackSize > 0
            && stack.getItem() == Items.dye
            && stack.getItemDamage() == BONEMEAL_META;
    }

    public static boolean plant(World world, int x, int y, int z, ItemStack seed) {
        if (!isSeed(seed) || !world.isAirBlock(x, y, z)) {
            return false;
        }
        IPlantable plantable = (IPlantable) seed.getItem();
        Block soil = world.getBlock(x, y - 1, z);
        if (soil == null || !soil.canSustainPlant(world, x, y - 1, z, ForgeDirection.UP, plantable)) {
            return false;
        }
        Block plant = plantable.getPlant(world, x, y, z);
        if (plant == null || plant == Blocks.air) {
            return false;
        }
        world.setBlock(x, y, z, plant, plantable.getPlantMetadata(world, x, y, z), 3);
        world.playSoundEffect(
            x + 0.5D,
            y + 0.5D,
            z + 0.5D,
            plant.stepSound.func_150496_b(),
            (plant.stepSound.getVolume() + 1.0F) / 2.0F,
            plant.stepSound.getPitch() * 0.8F);
        seed.stackSize--;
        return true;
    }

    public static boolean fertilize(World world, Random random, int x, int y, int z) {
        if (!canGrow(world, x, y, z)) {
            return false;
        }
        IGrowable growable = (IGrowable) world.getBlock(x, y, z);
        if (growable.func_149852_a(world, random, x, y, z)) {
            growable.func_149853_b(world, random, x, y, z);
        }
        world.playAuxSFX(BONEMEAL_EFFECT, x, y, z, 0);
        return true;
    }
}
