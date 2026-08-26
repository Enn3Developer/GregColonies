package com.enn3developer.gregcolonies.entity.ai.idle;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class IdleTaskReturnHome extends IdleTask {

    public static final String ID = "return_home";

    private static final double SPEED = 0.7D;

    private static final int TIMEOUT = 1200;

    private static final int RETRY_DELAY = 200;

    private static final int MAX_PATH_FAILURES = 20;

    private static final double MARGIN = 8.0D;

    private long nextAttempt;

    private int ticks;

    private int pathFailures;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String describe() {
        return "return home";
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (world.getTotalWorldTime() < nextAttempt) {
            return false;
        }
        int dimension = world.provider.dimensionId;
        return dimension == colony.getDimension() && !colony.isInside(dimension, citizen.posX, citizen.posZ);
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        ticks = 0;
        pathFailures = 0;
        pathHome(citizen, colony);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        int dimension = citizen.worldObj.provider.dimensionId;
        if (dimension != colony.getDimension()) {
            delay(citizen);
            return false;
        }

        if (isHome(citizen, colony, dimension)) {
            return false;
        }

        if (++ticks > TIMEOUT) {
            delay(citizen);
            return false;
        }

        if (citizen.getNavigator()
            .noPath()) {
            if (pathHome(citizen, colony)) {
                pathFailures = 0;
            } else if (++pathFailures > MAX_PATH_FAILURES) {
                delay(citizen);
                return false;
            }
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
    }

    private static boolean isHome(EntityCitizen citizen, Colony colony, int dimension) {
        double limit = Math.max(Config.colonyRadius - MARGIN, Config.colonyRadius * 0.5D);
        double distanceSq = colony
            .distanceSqTo(dimension, MathHelper.floor_double(citizen.posX), MathHelper.floor_double(citizen.posZ));
        return distanceSq <= limit * limit;
    }

    private static boolean pathHome(EntityCitizen citizen, Colony colony) {
        return pathTowards(citizen, colony.getX() + 0.5D, colony.getY(), colony.getZ() + 0.5D, SPEED);
    }

    private void delay(EntityCitizen citizen) {
        nextAttempt = citizen.worldObj.getTotalWorldTime() + RETRY_DELAY;
    }
}
