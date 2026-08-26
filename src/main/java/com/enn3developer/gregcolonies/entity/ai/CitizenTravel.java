package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.compat.Elevators;
import com.enn3developer.gregcolonies.compat.Mods;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class CitizenTravel {

    private static final double MIN_CLIMB = 5.0D;

    private static final double MIN_GAIN = 3.0D;

    private static final int SEARCH_RADIUS = 12;

    private static final int SEARCH_HEIGHT = 4;

    private static final int SEARCH_DELAY = 100;

    private static final int RIDE_TIMEOUT = 200;

    private static final int RIDE_DELAY = 40;

    private final EntityCitizen citizen;

    private boolean riding;

    private int rideX;

    private int rideY;

    private int rideZ;

    private boolean rideUp;

    private double rideSpeed;

    private int rideTicks;

    private long nextRide;

    public CitizenTravel(EntityCitizen citizen) {
        this.citizen = citizen;
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        if (riding) {
            return pathToRide();
        }
        if (shouldRide(y) && startRide(y, speed)) {
            return true;
        }
        return path(x, y, z, speed);
    }

    public void update() {
        if (!riding) {
            return;
        }
        World world = citizen.worldObj;
        if (++rideTicks > RIDE_TIMEOUT || !Elevators.isElevator(world, rideX, rideY, rideZ)) {
            stop();
            return;
        }
        if (!isOnRide()) {
            return;
        }

        int level = Elevators.ride(citizen, rideX, rideY, rideZ, rideUp);
        stop();
        if (level != Elevators.NO_LEVEL) {
            citizen.getNavigator()
                .clearPathEntity();
            nextRide = world.getTotalWorldTime() + RIDE_DELAY;
        }
    }

    public void stop() {
        riding = false;
        rideTicks = 0;
    }

    private boolean shouldRide(double targetY) {
        return Mods.openBlocks() && Math.abs(targetY - citizen.posY) >= MIN_CLIMB
            && citizen.worldObj.getTotalWorldTime() >= nextRide;
    }

    private boolean startRide(double targetY, double speed) {
        int[] spot = Elevators.findRide(citizen, targetY, MIN_GAIN, SEARCH_RADIUS, SEARCH_HEIGHT);
        if (spot == null) {
            delay();
            return false;
        }

        riding = true;
        rideTicks = 0;
        rideX = spot[0];
        rideY = spot[1];
        rideZ = spot[2];
        rideUp = targetY > citizen.posY;
        rideSpeed = speed;
        return pathToRide();
    }

    private boolean pathToRide() {
        if (isOnRide() || path(rideX + 0.5D, rideY + 1, rideZ + 0.5D, rideSpeed)) {
            return true;
        }
        stop();
        delay();
        return false;
    }

    private boolean isOnRide() {
        return MathHelper.floor_double(citizen.posX) == rideX && MathHelper.floor_double(citizen.posZ) == rideZ
            && MathHelper.floor_double(citizen.boundingBox.minY) - 1 == rideY;
    }

    private void delay() {
        nextRide = citizen.worldObj.getTotalWorldTime() + SEARCH_DELAY;
    }

    private boolean path(double x, double y, double z, double speed) {
        return citizen.getNavigator()
            .tryMoveToXYZ(x, y, z, speed);
    }
}
