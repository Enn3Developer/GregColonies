package com.enn3developer.gregcolonies.entity.ai.auto;

import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public abstract class AutoTask {

    protected static final int REPATH_INTERVAL = 10;

    private long nextAttempt;

    private int travelTicks;

    public abstract String getId();

    public abstract boolean shouldStart(EntityCitizen citizen, Colony colony);

    public void start(EntityCitizen citizen, Colony colony) {}

    public abstract boolean update(EntityCitizen citizen, Colony colony);

    public void finish(EntityCitizen citizen) {}

    public String describe() {
        return getId();
    }

    protected final boolean ready(World world) {
        return world.getTotalWorldTime() >= nextAttempt;
    }

    protected final void delay(World world, int ticks) {
        nextAttempt = world.getTotalWorldTime() + ticks;
    }

    protected final void resetTravel() {
        travelTicks = 0;
    }

    protected final boolean travel(EntityCitizen citizen, double x, double y, double z, double speed, int timeout) {
        return step(citizen, x, y, z, speed, timeout, true);
    }

    protected final boolean chase(EntityCitizen citizen, double x, double y, double z, double speed, int timeout) {
        return step(citizen, x, y, z, speed, timeout, false);
    }

    private boolean step(EntityCitizen citizen, double x, double y, double z, double speed, int timeout,
        boolean onlyWhenIdle) {
        if (++travelTicks > timeout) {
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 1 && (!onlyWhenIdle || citizen.getNavigator()
            .noPath())) {
            pathTowards(citizen, x, y, z, speed);
        }
        return true;
    }

    protected static boolean pathTowards(EntityCitizen citizen, double x, double y, double z, double speed) {
        return citizen.travelTo(x, y, z, speed);
    }
}
