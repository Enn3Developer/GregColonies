package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class EntityAICitizenSwim extends EntityAIBase {

    private static final float FLOAT_CHANCE = 0.8F;

    private static final int LOW_AIR = 150;

    private static final int SHORE_INTERVAL = 20;

    private static final int SHORE_RADIUS = 6;

    private static final int SHORE_HEIGHT = 4;

    private static final double SHORE_SPEED = 1.0D;

    private final EntityCitizen citizen;

    private int shoreTicks;

    public EntityAICitizenSwim(EntityCitizen citizen) {
        this.citizen = citizen;
        setMutexBits(4);
        citizen.getNavigator()
            .setCanSwim(true);
    }

    @Override
    public boolean shouldExecute() {
        return citizen.isInWater() || citizen.handleLavaMovement();
    }

    @Override
    public void startExecuting() {
        shoreTicks = 0;
    }

    @Override
    public void updateTask() {
        boolean drowning = citizen.getAir() < LOW_AIR;
        if (drowning || citizen.handleLavaMovement()
            || citizen.getRNG()
                .nextFloat() < FLOAT_CHANCE) {
            citizen.getJumpHelper()
                .setJumping();
        }

        if (!citizen.isInWater() || citizen.wantsWater() || --shoreTicks > 0) {
            return;
        }
        shoreTicks = SHORE_INTERVAL;
        if (!citizen.getNavigator()
            .noPath()) {
            return;
        }
        int[] shore = Hazards.findSafeSpot(citizen, SHORE_RADIUS, SHORE_HEIGHT);
        if (shore != null) {
            citizen.getNavigator()
                .tryMoveToXYZ(shore[0] + 0.5D, shore[1], shore[2] + 0.5D, SHORE_SPEED);
        }
    }
}
