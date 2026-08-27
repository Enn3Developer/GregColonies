package com.enn3developer.gregcolonies.colony;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStairs;
import net.minecraftforge.common.util.ForgeDirection;

public final class BlockRotation {

    private static final int PILLAR_AXIS_MASK = 12;

    private static final int PILLAR_AXIS_X = 4;

    private static final int PILLAR_AXIS_Z = 8;

    private static final int QUARTZ_AXIS_X = 3;

    private static final int QUARTZ_AXIS_Z = 4;

    private static final int PUMPKIN_MASK = 3;

    private static final int FACING_MASK = 7;

    private static final int STAIR_FACING_MASK = 3;

    private static final int[] STAIR_FACING = { 5, 4, 3, 2 };

    private BlockRotation() {}

    public static int transform(Block block, int meta, int steps, boolean mirror) {
        if (block == null || (steps == 0 && !mirror)) {
            return meta;
        }
        if (block instanceof BlockRotatedPillar) {
            return pillar(meta, steps);
        }
        if (block instanceof BlockQuartz) {
            return quartz(meta, steps);
        }
        if (block instanceof BlockPumpkin) {
            return pumpkin(meta, steps, mirror);
        }
        if (block instanceof BlockStairs) {
            return stairs(meta, steps, mirror);
        }
        if (block instanceof BlockPistonBase || block instanceof BlockDispenser) {
            int rest = meta & ~FACING_MASK;
            return rest | direction(meta & FACING_MASK, steps, mirror);
        }
        if (block instanceof BlockFurnace || block instanceof BlockChest
            || block instanceof BlockEnderChest
            || block instanceof BlockLadder) {
            return direction(meta, steps, mirror);
        }
        return meta;
    }

    private static int pillar(int meta, int steps) {
        if (steps % 2 == 0) {
            return meta;
        }
        int axis = meta & PILLAR_AXIS_MASK;
        if (axis == PILLAR_AXIS_X) {
            return (meta & ~PILLAR_AXIS_MASK) | PILLAR_AXIS_Z;
        }
        if (axis == PILLAR_AXIS_Z) {
            return (meta & ~PILLAR_AXIS_MASK) | PILLAR_AXIS_X;
        }
        return meta;
    }

    private static int quartz(int meta, int steps) {
        if (steps % 2 == 0) {
            return meta;
        }
        if (meta == QUARTZ_AXIS_X) {
            return QUARTZ_AXIS_Z;
        }
        return meta == QUARTZ_AXIS_Z ? QUARTZ_AXIS_X : meta;
    }

    private static int pumpkin(int meta, int steps, boolean mirror) {
        int facing = meta & PUMPKIN_MASK;
        if (mirror && (facing & 1) == 1) {
            facing ^= 2;
        }
        return (meta & ~PUMPKIN_MASK) | ((facing + steps) & PUMPKIN_MASK);
    }

    private static int stairs(int meta, int steps, boolean mirror) {
        int facing = direction(STAIR_FACING[meta & STAIR_FACING_MASK], steps, mirror);
        for (int i = 0; i < STAIR_FACING.length; i++) {
            if (STAIR_FACING[i] == facing) {
                return (meta & ~STAIR_FACING_MASK) | i;
            }
        }
        return meta;
    }

    private static int direction(int facing, int steps, boolean mirror) {
        ForgeDirection side = ForgeDirection.getOrientation(facing);
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN || side == ForgeDirection.UNKNOWN) {
            return facing;
        }
        if (mirror) {
            if (side == ForgeDirection.EAST) {
                side = ForgeDirection.WEST;
            } else if (side == ForgeDirection.WEST) {
                side = ForgeDirection.EAST;
            }
        }
        for (int i = 0; i < steps; i++) {
            side = clockwise(side);
        }
        return side.ordinal();
    }

    private static ForgeDirection clockwise(ForgeDirection side) {
        if (side == ForgeDirection.NORTH) {
            return ForgeDirection.EAST;
        }
        if (side == ForgeDirection.EAST) {
            return ForgeDirection.SOUTH;
        }
        if (side == ForgeDirection.SOUTH) {
            return ForgeDirection.WEST;
        }
        return ForgeDirection.NORTH;
    }
}
