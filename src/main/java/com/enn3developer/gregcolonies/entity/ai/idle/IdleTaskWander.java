package com.enn3developer.gregcolonies.entity.ai.idle;

import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public class IdleTaskWander extends AutoTask {

    public static final String ID = "wander";

    private static final double SPEED = 0.5D;

    private static final int RANGE = 10;

    private static final int VERTICAL = 7;

    private static final int START_CHANCE = 60;

    private static final int TIMEOUT = 600;

    private static final double EDGE_FRACTION = 0.66D;

    private double x;

    private double y;

    private double z;

    private int ticks;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        if (citizen.getRNG()
            .nextInt(START_CHANCE) != 0) {
            return false;
        }
        int dimension = citizen.worldObj.provider.dimensionId;
        if (dimension != colony.getDimension() || !colony.isInside(dimension, citizen.posX, citizen.posZ)) {
            return false;
        }

        Vec3 target = pickTarget(citizen, colony, dimension);
        if (target == null || !colony.isInside(dimension, target.xCoord, target.zCoord)) {
            return false;
        }
        x = target.xCoord;
        y = target.yCoord;
        z = target.zCoord;
        return true;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        ticks = 0;
        citizen.getNavigator()
            .tryMoveToXYZ(x, y, z, SPEED);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        return ++ticks <= TIMEOUT && !citizen.getNavigator()
            .noPath();
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
    }

    private static Vec3 pickTarget(EntityCitizen citizen, Colony colony, int dimension) {
        double edge = Config.colonyRadius * EDGE_FRACTION;
        double distanceSq = colony
            .distanceSqTo(dimension, MathHelper.floor_double(citizen.posX), MathHelper.floor_double(citizen.posZ));
        if (distanceSq <= edge * edge) {
            return RandomPositionGenerator.findRandomTarget(citizen, RANGE, VERTICAL);
        }
        return RandomPositionGenerator.findRandomTargetBlockTowards(
            citizen,
            RANGE,
            VERTICAL,
            Vec3.createVectorHelper(colony.getX() + 0.5D, colony.getY(), colony.getZ() + 0.5D));
    }
}
