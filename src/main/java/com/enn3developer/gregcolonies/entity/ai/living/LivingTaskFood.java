package com.enn3developer.gregcolonies.entity.ai.living;

import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;

public class LivingTaskFood extends AutoTask {

    public static final String ID = "get food";

    private static final int LOW_FOOD = 4;

    private static final int TARGET_FOOD = 16;

    private static final double SPEED = 0.7D;

    private static final double REACH_SQ = 16.0D;

    private static final float LOOK_SPEED = 30.0F;

    private static final int TRAVEL_TIMEOUT = 1200;

    private static final int REPATH_INTERVAL = 10;

    private static final int RETRY_DELAY = 1200;

    private long nextAttempt;

    private int travelTicks;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (world.getTotalWorldTime() < nextAttempt || !colony.hasPickUp()) {
            return false;
        }
        if (world.provider.dimensionId != colony.getDimension()) {
            return false;
        }
        return citizen.getInventory()
            .countFood() < LOW_FOOD;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        travelTicks = 0;
        pathToPickUp(citizen, colony);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        if (!colony.hasPickUp()) {
            return false;
        }

        int x = colony.getPickUpX();
        int y = colony.getPickUpY();
        int z = colony.getPickUpZ();
        if (citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) <= REACH_SQ) {
            return take(citizen, x, y, z);
        }

        if (++travelTicks > TRAVEL_TIMEOUT) {
            delay(citizen);
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 0 && citizen.getNavigator()
            .noPath()) {
            pathToPickUp(citizen, colony);
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
    }

    private boolean take(EntityCitizen citizen, int x, int y, int z) {
        citizen.getLookHelper()
            .setLookPosition(x + 0.5D, y + 0.5D, z + 0.5D, LOOK_SPEED, LOOK_SPEED);
        citizen.getNavigator()
            .clearPathEntity();
        delay(citizen);

        IInventory source = Inventories.at(citizen.worldObj, x, y, z);
        if (source == null) {
            return false;
        }
        if (citizen.getInventory()
            .stockFood(source, TARGET_FOOD) > 0) {
            citizen.swingItem();
        }
        return false;
    }

    private static boolean pathToPickUp(EntityCitizen citizen, Colony colony) {
        return pathTowards(citizen, colony.getPickUpX() + 0.5D, colony.getPickUpY(), colony.getPickUpZ() + 0.5D, SPEED);
    }

    private void delay(EntityCitizen citizen) {
        nextAttempt = citizen.worldObj.getTotalWorldTime() + RETRY_DELAY;
    }
}
