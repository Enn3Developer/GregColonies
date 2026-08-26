package com.enn3developer.gregcolonies.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import openblocks.Config;
import openblocks.api.IElevatorBlock;
import openblocks.common.ElevatorBlockRules;

public final class Elevators {

    public static final int NO_LEVEL = Integer.MIN_VALUE;

    private static final String RIDE_SOUND = "openblocks:elevator.activate";

    private static final double PASSABLE_EDGE = 0.7D;

    private static final double LANDING_HEIGHT = 1.1D;

    private Elevators() {}

    public static boolean isElevator(World world, int x, int y, int z) {
        return world.blockExists(x, y, z) && world.getBlock(x, y, z) instanceof IElevatorBlock;
    }

    public static int findLevel(EntityLivingBase entity, int x, int y, int z, boolean up) {
        World world = entity.worldObj;
        if (!isElevator(world, x, y, z)) {
            return NO_LEVEL;
        }

        IElevatorBlock elevator = (IElevatorBlock) world.getBlock(x, y, z);
        int color = elevator.getColor(world, x, y, z);
        int delta = up ? 1 : -1;
        int blocked = 0;
        int level = y;

        for (int i = 0; i < Config.elevatorTravelDistance; i++) {
            level += delta;
            if (!world.blockExists(x, level, z)) {
                return NO_LEVEL;
            }
            if (world.isAirBlock(x, level, z)) {
                continue;
            }

            Block block = world.getBlock(x, level, z);
            if (block instanceof IElevatorBlock && ((IElevatorBlock) block).getColor(world, x, level, z) == color
                && isFree(entity, world, x, level + 1, z)) {
                return level;
            }
            if (Config.elevatorIgnoreBlocks) {
                continue;
            }

            ElevatorBlockRules.Action action = ElevatorBlockRules.instance.getActionForBlock(block);
            if (action == ElevatorBlockRules.Action.ABORT) {
                return NO_LEVEL;
            }
            if (action != ElevatorBlockRules.Action.IGNORE && ++blocked > Config.elevatorMaxBlockPassCount) {
                return NO_LEVEL;
            }
        }
        return NO_LEVEL;
    }

    public static int ride(EntityLivingBase entity, int x, int y, int z, boolean up) {
        int level = findLevel(entity, x, y, z, up);
        if (level == NO_LEVEL) {
            return NO_LEVEL;
        }

        entity.fallDistance = 0.0F;
        entity.setPositionAndUpdate(x + 0.5D, level + LANDING_HEIGHT, z + 0.5D);
        entity.worldObj.playSoundAtEntity(entity, RIDE_SOUND, 1.0F, 1.0F);
        return level;
    }

    public static int[] findRide(EntityLivingBase entity, double targetY, double minGain, int radius, int height) {
        boolean up = targetY > entity.posY;
        double current = Math.abs(targetY - entity.posY);
        int[] best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int[] spot : nearby(entity, radius, height)) {
            int level = findLevel(entity, spot[0], spot[1], spot[2], up);
            if (level == NO_LEVEL || current - Math.abs(targetY - (level + 1)) < minGain) {
                continue;
            }
            double distanceSq = entity.getDistanceSq(spot[0] + 0.5D, spot[1] + 1, spot[2] + 0.5D);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = spot;
            }
        }
        return best;
    }

    public static int[] findNearest(EntityLivingBase entity, int radius, int height) {
        int[] best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int[] spot : nearby(entity, radius, height)) {
            if (findLevel(entity, spot[0], spot[1], spot[2], true) == NO_LEVEL
                && findLevel(entity, spot[0], spot[1], spot[2], false) == NO_LEVEL) {
                continue;
            }
            double distanceSq = entity.getDistanceSq(spot[0] + 0.5D, spot[1] + 1, spot[2] + 0.5D);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = spot;
            }
        }
        return best;
    }

    private static List<int[]> nearby(EntityLivingBase entity, int radius, int height) {
        World world = entity.worldObj;
        List<int[]> spots = new ArrayList<>();
        int cx = MathHelper.floor_double(entity.posX);
        int cy = MathHelper.floor_double(entity.boundingBox.minY);
        int cz = MathHelper.floor_double(entity.posZ);
        int minY = Math.max(cy - height, 1);
        int maxY = Math.min(cy + height, 253);
        if (!world.checkChunksExist(cx - radius, minY, cz - radius, cx + radius, maxY, cz + radius)) {
            return spots;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    if (world.getBlock(x, y, z) instanceof IElevatorBlock && isFree(entity, world, x, y + 1, z)) {
                        spots.add(new int[] { x, y, z });
                    }
                }
            }
        }
        return spots;
    }

    private static boolean isFree(EntityLivingBase entity, World world, int x, int y, int z) {
        int blocks = Math.max(1, MathHelper.ceiling_double_int(entity.height));
        for (int dy = 0; dy < blocks; dy++) {
            if (!isPassable(world, x, y + dy, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPassable(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return true;
        }
        if (!Config.irregularBlocksArePassable) {
            return false;
        }
        AxisAlignedBB box = block.getCollisionBoundingBoxFromPool(world, x, y, z);
        return box == null || box.getAverageEdgeLength() < PASSABLE_EDGE;
    }
}
