package com.enn3developer.gregcolonies.entity.ai.command;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;

public class CitizenCommandMoveTo extends CitizenCommand {

    public static final String ID = "move_to";

    private static final int DEFAULT_STALL_TICKS = 600;

    private static final int MAX_PATH_FAILURES = 20;

    private static final double ARRIVED_DISTANCE_SQ = 4.0D;

    private static final double PROGRESS_EPSILON = 1.0D;

    private static final double CLAIM_DISTANCE_SQ = 96.0D * 96.0D;

    private int x;
    private int y;
    private int z;
    private double speed = 0.6D;
    private int stallTicks = DEFAULT_STALL_TICKS;
    private int ticksWithoutProgress;
    private int pathFailures;
    private double bestDistanceSq = Double.MAX_VALUE;

    public CitizenCommandMoveTo() {}

    public CitizenCommandMoveTo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean canBeTakenBy(EntityCitizen citizen) {
        return citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) < CLAIM_DISTANCE_SQ;
    }

    @Override
    public void start(EntityCitizen citizen) {
        ticksWithoutProgress = 0;
        pathFailures = 0;
        bestDistanceSq = Double.MAX_VALUE;
        pathTowards(citizen);
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        double distanceSq = citizen.getDistanceSq(x + 0.5D, y, z + 0.5D);
        if (distanceSq < ARRIVED_DISTANCE_SQ) {
            return CitizenCommandResult.DONE;
        }

        if (distanceSq < bestDistanceSq - PROGRESS_EPSILON) {
            bestDistanceSq = distanceSq;
            ticksWithoutProgress = 0;
        } else {
            ticksWithoutProgress++;
        }
        if (ticksWithoutProgress > stallTicks) {
            return CitizenCommandResult.FAILED;
        }

        if (citizen.getNavigator()
            .noPath()) {
            if (pathTowards(citizen)) {
                pathFailures = 0;
            } else if (++pathFailures > MAX_PATH_FAILURES) {
                return CitizenCommandResult.FAILED;
            }
        }
        return CitizenCommandResult.RUNNING;
    }

    private boolean pathTowards(EntityCitizen citizen) {
        return citizen.travelTo(x + 0.5D, y, z + 0.5D, speed);
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        x = tag.getInteger("x");
        y = tag.getInteger("y");
        z = tag.getInteger("z");
        speed = tag.hasKey("speed") ? tag.getDouble("speed") : speed;
        stallTicks = tag.hasKey("stallTicks") ? tag.getInteger("stallTicks") : DEFAULT_STALL_TICKS;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setDouble("speed", speed);
        tag.setInteger("stallTicks", stallTicks);
    }

    @Override
    public String describe() {
        return ID + " " + x + "/" + y + "/" + z;
    }
}
