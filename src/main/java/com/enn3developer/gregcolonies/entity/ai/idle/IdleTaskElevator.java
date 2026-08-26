package com.enn3developer.gregcolonies.entity.ai.idle;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.compat.Elevators;
import com.enn3developer.gregcolonies.compat.Mods;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public class IdleTaskElevator extends AutoTask {

    public static final String ID = "elevator";

    private static final int START_CHANCE = 2400;

    private static final int RIDE_INTERVAL = 48000;

    private static final int RIDE_JITTER = 24000;

    private static final int SEARCH_RETRY = 6000;

    private static final int SEARCH_RADIUS = 12;

    private static final int SEARCH_HEIGHT = 5;

    private static final int MIN_RIDES = 2;

    private static final int EXTRA_RIDES = 4;

    private static final int RIDE_PAUSE = 30;

    private static final int TRAVEL_TIMEOUT = 600;

    private static final int REPATH_INTERVAL = 10;

    private static final double SPEED = 0.6D;

    private long nextRide;

    private boolean hasSpot;

    private int spotX;

    private int spotY;

    private int spotZ;

    private boolean up;

    private int rides;

    private int pauseTicks;

    private int travelTicks;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String describe() {
        return "ride the elevator";
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        long time = world.getTotalWorldTime();
        if (!Mods.openBlocks() || time < nextRide) {
            return false;
        }
        if (citizen.getRNG()
            .nextInt(START_CHANCE) != 0) {
            return false;
        }

        int dimension = world.provider.dimensionId;
        if (dimension != colony.getDimension() || !colony.isInside(dimension, citizen.posX, citizen.posZ)) {
            return false;
        }

        if (hasSpot && !Elevators.isElevator(world, spotX, spotY, spotZ)) {
            hasSpot = false;
        }
        if (!hasSpot) {
            int[] spot = Elevators.findNearest(citizen, SEARCH_RADIUS, SEARCH_HEIGHT);
            if (spot != null && colony.isInside(dimension, spot[0] + 0.5D, spot[2] + 0.5D)) {
                spotX = spot[0];
                spotY = spot[1];
                spotZ = spot[2];
                hasSpot = true;
            }
        }
        if (!hasSpot) {
            nextRide = time + SEARCH_RETRY;
            return false;
        }
        return true;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        travelTicks = 0;
        pauseTicks = 0;
        rides = MIN_RIDES + citizen.getRNG()
            .nextInt(EXTRA_RIDES);
        up = Elevators.findLevel(citizen, spotX, spotY, spotZ, true) != Elevators.NO_LEVEL;
        pathTowards(citizen, spotX + 0.5D, spotY + 1, spotZ + 0.5D, SPEED);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!Elevators.isElevator(world, spotX, spotY, spotZ)) {
            hasSpot = false;
            return false;
        }
        if (pauseTicks > 0) {
            pauseTicks--;
            return true;
        }

        if (isOnSpot(citizen)) {
            return ride(citizen);
        }

        if (++travelTicks > TRAVEL_TIMEOUT) {
            hasSpot = false;
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 0 && citizen.getNavigator()
            .noPath()) {
            pathTowards(citizen, spotX + 0.5D, spotY + 1, spotZ + 0.5D, SPEED);
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        nextRide = citizen.worldObj.getTotalWorldTime() + RIDE_INTERVAL
            + citizen.getRNG()
                .nextInt(RIDE_JITTER);
        citizen.getNavigator()
            .clearPathEntity();
    }

    private boolean ride(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
        int level = Elevators.ride(citizen, spotX, spotY, spotZ, up);
        if (level == Elevators.NO_LEVEL) {
            up = !up;
            level = Elevators.ride(citizen, spotX, spotY, spotZ, up);
        }
        if (level == Elevators.NO_LEVEL) {
            return false;
        }

        spotY = level;
        up = !up;
        pauseTicks = RIDE_PAUSE;
        travelTicks = 0;
        return --rides > 0;
    }

    private boolean isOnSpot(EntityCitizen citizen) {
        return MathHelper.floor_double(citizen.posX) == spotX && MathHelper.floor_double(citizen.posZ) == spotZ
            && MathHelper.floor_double(citizen.boundingBox.minY) - 1 == spotY;
    }
}
