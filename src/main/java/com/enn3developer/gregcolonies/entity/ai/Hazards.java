package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public final class Hazards {

    private static final int MARGIN = 1;

    private static final int DROP_SCAN = 8;

    private Hazards() {}

    public static boolean isDeadly(World world, int x, int y, int z) {
        Material material = world.getBlock(x, y, z)
            .getMaterial();
        return material == Material.lava || material == Material.fire;
    }

    public static boolean isDeadlyStep(World world, int x, int y, int z) {
        return isDeadly(world, x, y, z) || isDeadly(world, x, y + 1, z) || isDeadly(world, x, y - 1, z);
    }

    public static boolean isDeadlyDrop(World world, int x, int y, int z) {
        for (int depth = 1; depth <= DROP_SCAN; depth++) {
            int below = y - depth;
            if (below < 1) {
                return false;
            }
            Material material = world.getBlock(x, below, z)
                .getMaterial();
            if (material == Material.lava || material == Material.fire) {
                return true;
            }
            if (material.isSolid() || material.isLiquid()) {
                return false;
            }
        }
        return false;
    }

    public static boolean isNearDeadly(World world, int x, int y, int z) {
        for (int dy = -MARGIN; dy <= MARGIN; dy++) {
            for (int dx = -MARGIN; dx <= MARGIN; dx++) {
                for (int dz = -MARGIN; dz <= MARGIN; dz++) {
                    if (isDeadly(world, x + dx, y + dy, z + dz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isBesideDeadly(World world, int x, int y, int z) {
        for (int dx = -MARGIN; dx <= MARGIN; dx++) {
            for (int dz = -MARGIN; dz <= MARGIN; dz++) {
                if ((dx != 0 || dz != 0)
                    && (isDeadly(world, x + dx, y, z + dz) || isDeadly(world, x + dx, y - 1, z + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInDanger(EntityLivingBase entity) {
        if (entity.handleLavaMovement()) {
            return true;
        }
        return isDeadlyStep(
            entity.worldObj,
            MathHelper.floor_double(entity.posX),
            MathHelper.floor_double(entity.boundingBox.minY),
            MathHelper.floor_double(entity.posZ));
    }

    public static int[] findWater(EntityLivingBase entity, int radius, int height) {
        World world = entity.worldObj;
        int cx = MathHelper.floor_double(entity.posX);
        int cy = MathHelper.floor_double(entity.boundingBox.minY);
        int cz = MathHelper.floor_double(entity.posZ);
        int minY = Math.max(cy - height, 1);
        int maxY = Math.min(cy + height, 253);
        if (!world.checkChunksExist(cx - radius, minY, cz - radius, cx + radius, maxY, cz + radius)) {
            return null;
        }

        int[] spot = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distanceSq = entity.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq >= bestDistanceSq || !isWater(world, x, y, z)) {
                        continue;
                    }
                    bestDistanceSq = distanceSq;
                    spot = new int[] { x, y, z };
                }
            }
        }
        return spot;
    }

    private static boolean isWater(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial() == Material.water;
    }

    public static int[] findSafeSpot(EntityLivingBase entity, int radius, int height) {
        World world = entity.worldObj;
        int cx = MathHelper.floor_double(entity.posX);
        int cy = MathHelper.floor_double(entity.boundingBox.minY);
        int cz = MathHelper.floor_double(entity.posZ);
        int minY = Math.max(cy - height, 1);
        int maxY = Math.min(cy + height, 253);
        if (!world.checkChunksExist(cx - radius, minY, cz - radius, cx + radius, maxY, cz + radius)) {
            return null;
        }

        int[] spot = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distanceSq = entity.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq >= bestDistanceSq || !isSafeSpot(world, x, y, z)) {
                        continue;
                    }
                    bestDistanceSq = distanceSq;
                    spot = new int[] { x, y, z };
                }
            }
        }
        return spot;
    }

    private static boolean isSafeSpot(World world, int x, int y, int z) {
        return isStandable(world, x, y, z) && !isNearDeadly(world, x, y, z);
    }

    private static boolean isStandable(World world, int x, int y, int z) {
        Block floor = world.getBlock(x, y - 1, z);
        if (!floor.getMaterial()
            .isSolid()) {
            return false;
        }
        return isClear(world, x, y, z) && isClear(world, x, y + 1, z);
    }

    public static int[] findEscapeSpot(EntityLivingBase entity, int radius, int height) {
        World world = entity.worldObj;
        int cx = MathHelper.floor_double(entity.posX);
        int cy = MathHelper.floor_double(entity.boundingBox.minY);
        int cz = MathHelper.floor_double(entity.posZ);
        int minY = Math.max(cy - height, 1);
        int maxY = Math.min(cy + height, 253);
        if (!world.checkChunksExist(cx - radius, minY, cz - radius, cx + radius, maxY, cz + radius)) {
            return null;
        }

        int[] safe = null;
        int[] clear = null;
        double bestSafeSq = Double.MAX_VALUE;
        double bestClearSq = Double.MAX_VALUE;
        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    if (!isStandable(world, x, y, z) || isDeadlyStep(world, x, y, z)) {
                        continue;
                    }
                    double distanceSq = entity.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq < bestClearSq) {
                        bestClearSq = distanceSq;
                        clear = new int[] { x, y, z };
                    }
                    if (distanceSq < bestSafeSq && !isNearDeadly(world, x, y, z)) {
                        bestSafeSq = distanceSq;
                        safe = new int[] { x, y, z };
                    }
                }
            }
        }
        return safe != null ? safe : clear;
    }

    private static boolean isClear(World world, int x, int y, int z) {
        Material material = world.getBlock(x, y, z)
            .getMaterial();
        return !material.isSolid() && !material.isLiquid();
    }
}
