package com.enn3developer.gregcolonies.entity.ai.work;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;

public final class WorkBlocks {

    private static final float MIN_DIG_SPEED = 0.05F;

    private static final float DIG_TICK_SCALE = 30.0F;

    private static final int MAX_DIG_TICKS = 400;

    private WorkBlocks() {}

    public static boolean isBigOre(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return false;
        }
        if (!OreManager.isOre(block, world.getBlockMetadata(x, y, z))
            .orElse(false)) {
            return false;
        }
        OreInfo<?> info = OreManager.getOreInfo(world, x, y, z);
        if (info == null) {
            return false;
        }
        boolean big = !info.isSmall;
        info.release();
        return big;
    }

    public static boolean isLog(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block != null && !block.isAir(world, x, y, z) && block.isWood(world, x, y, z);
    }

    public static boolean isLeaves(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block != null && !block.isAir(world, x, y, z) && block.isLeaves(world, x, y, z);
    }

    public static boolean isTreeSoil(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block != null && block.canSustainPlant(world, x, y, z, ForgeDirection.UP, (IPlantable) Blocks.sapling);
    }

    public static boolean blocksMovement(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block != null && !block.isAir(world, x, y, z)
            && block.getMaterial()
                .blocksMovement();
    }

    public static boolean isLiquid(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block != null && block.getMaterial()
            .isLiquid();
    }

    public static boolean canHarvest(ItemStack tool, Block block, int meta) {
        if (block.getMaterial()
            .isToolNotRequired()) {
            return true;
        }
        String harvestTool = block.getHarvestTool(meta);
        if (harvestTool == null) {
            return tool != null && tool.getItem()
                .getDigSpeed(tool, block, meta) > 1.0F;
        }
        return tool != null && tool.getItem()
            .getHarvestLevel(tool, harvestTool) >= block.getHarvestLevel(meta);
    }

    public static boolean isEffectiveOn(ItemStack tool, Block block) {
        return tool != null && tool.getItem()
            .getDigSpeed(tool, block, 0) > 1.0F;
    }

    public static int digTicks(ItemStack tool, World world, Block block, int meta, int x, int y, int z) {
        float hardness = block.getBlockHardness(world, x, y, z);
        if (hardness < 0.0F) {
            return -1;
        }
        float speed = tool == null ? 1.0F
            : tool.getItem()
                .getDigSpeed(tool, block, meta);
        if (speed < MIN_DIG_SPEED) {
            speed = MIN_DIG_SPEED;
        }
        int ticks = MathHelper.ceiling_float_int(hardness * DIG_TICK_SCALE / speed);
        return Math.max(1, Math.min(MAX_DIG_TICKS, ticks));
    }

    public static int[] takeNearest(double x, double y, double z, List<int[]> positions) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            int[] position = positions.get(i);
            double dx = position[0] + 0.5D - x;
            double dy = position[1] + 0.5D - y;
            double dz = position[2] + 0.5D - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best < 0 ? null : positions.remove(best);
    }

    public static boolean isScaffold(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return false;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null || block == Blocks.air || block instanceof BlockFalling) {
            return false;
        }
        if (!block.renderAsNormalBlock() || !block.getMaterial()
            .blocksMovement()) {
            return false;
        }
        return !OreManager.isOre(block, stack.getItemDamage())
            .orElse(false);
    }
}
