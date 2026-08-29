package com.enn3developer.gregcolonies.entity.ai.idle;

import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public class IdleTaskBath extends AutoTask {

    public static final String ID = "bath";

    private static final int POOL_RADIUS = 1;

    private static final int POOL_HEIGHT = 2;

    private static final int SEARCH_RADIUS = 12;

    private static final int SEARCH_HEIGHT = 4;

    private static final int BATH_TICKS = 200;

    private static final int BATH_INTERVAL = 6000;

    private static final int BATH_JITTER = 2400;

    private static final int SEARCH_RETRY = 2400;

    private static final int TRAVEL_TIMEOUT = 600;

    private static final int VALIDATE_INTERVAL = 40;

    private static final double SPEED = 0.6D;

    private static final double SPOT_REACH_SQ = 4.0D;

    private static final double SPOT_HEIGHT = 3.0D;

    private static final int MIN_AIR = 150;

    private boolean hasSpot;

    private int spotX;

    private int spotY;

    private int spotZ;

    private int bathTicks;

    private int validateTicks;

    private boolean avoidedWater;

    private boolean navigatorAdjusted;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!ready(world)) {
            return false;
        }

        int dimension = world.provider.dimensionId;
        if (dimension != colony.getDimension() || !colony.isInside(dimension, citizen.posX, citizen.posZ)) {
            return false;
        }

        if (hasSpot && !isPool(world, spotX, spotY, spotZ)) {
            hasSpot = false;
        }
        if (!hasSpot) {
            hasSpot = findPool(citizen, colony, dimension);
        }
        if (!hasSpot) {
            delay(world, SEARCH_RETRY);
            return false;
        }
        return true;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        resetTravel();
        bathTicks = 0;
        validateTicks = 0;
        avoidedWater = citizen.getNavigator()
            .getAvoidsWater();
        navigatorAdjusted = true;
        citizen.setWantsWater(true);
        citizen.getNavigator()
            .setAvoidsWater(false);
        pathTowards(citizen, spotX + 0.5D, spotY, spotZ + 0.5D, SPEED);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (++validateTicks >= VALIDATE_INTERVAL) {
            validateTicks = 0;
            if (!isPool(world, spotX, spotY, spotZ)) {
                hasSpot = false;
                return false;
            }
        }

        if (citizen.getAir() < MIN_AIR) {
            delay(world, SEARCH_RETRY);
            return false;
        }

        if (isBathing(citizen)) {
            citizen.getNavigator()
                .clearPathEntity();
            if (++bathTicks < BATH_TICKS) {
                return true;
            }
            delay(
                world,
                BATH_INTERVAL + citizen.getRNG()
                    .nextInt(BATH_JITTER));
            return false;
        }

        if (!travel(citizen, spotX + 0.5D, spotY, spotZ + 0.5D, SPEED, TRAVEL_TIMEOUT)) {
            hasSpot = false;
            delay(world, SEARCH_RETRY);
            return false;
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.setWantsWater(false);
        if (navigatorAdjusted) {
            navigatorAdjusted = false;
            citizen.getNavigator()
                .setAvoidsWater(avoidedWater);
        }
        citizen.getNavigator()
            .clearPathEntity();
    }

    private boolean isBathing(EntityCitizen citizen) {
        if (!citizen.isInWater()) {
            return false;
        }
        double dx = citizen.posX - (spotX + 0.5D);
        double dz = citizen.posZ - (spotZ + 0.5D);
        return dx * dx + dz * dz <= SPOT_REACH_SQ && Math.abs(citizen.posY - spotY) <= SPOT_HEIGHT;
    }

    private boolean findPool(EntityCitizen citizen, Colony colony, int dimension) {
        World world = citizen.worldObj;
        int cx = MathHelper.floor_double(citizen.posX);
        int cy = MathHelper.floor_double(citizen.posY);
        int cz = MathHelper.floor_double(citizen.posZ);
        int minY = Math.max(cy - SEARCH_HEIGHT, 1);
        int maxY = Math.min(cy + SEARCH_HEIGHT, 253);
        if (!world.checkChunksExist(
            cx - SEARCH_RADIUS - POOL_RADIUS,
            minY,
            cz - SEARCH_RADIUS - POOL_RADIUS,
            cx + SEARCH_RADIUS + POOL_RADIUS,
            maxY,
            cz + SEARCH_RADIUS + POOL_RADIUS)) {
            return false;
        }

        double bestDistanceSq = Double.MAX_VALUE;
        boolean found = false;
        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - SEARCH_RADIUS; x <= cx + SEARCH_RADIUS; x++) {
                for (int z = cz - SEARCH_RADIUS; z <= cz + SEARCH_RADIUS; z++) {
                    if (!isWater(world, x, y, z) || !colony.isInside(dimension, x + 0.5D, z + 0.5D)) {
                        continue;
                    }
                    double distanceSq = citizen.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq >= bestDistanceSq || !isPool(world, x, y, z)) {
                        continue;
                    }
                    bestDistanceSq = distanceSq;
                    spotX = x;
                    spotY = y;
                    spotZ = z;
                    found = true;
                }
            }
        }
        return found;
    }

    private static boolean isPool(World world, int cx, int cy, int cz) {
        if (world.getBlock(cx, cy + POOL_HEIGHT, cz)
            .getMaterial() != Material.air) {
            return false;
        }
        for (int y = cy; y < cy + POOL_HEIGHT; y++) {
            for (int x = cx - POOL_RADIUS; x <= cx + POOL_RADIUS; x++) {
                for (int z = cz - POOL_RADIUS; z <= cz + POOL_RADIUS; z++) {
                    if (!isWater(world, x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isWater(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial() == Material.water;
    }
}
