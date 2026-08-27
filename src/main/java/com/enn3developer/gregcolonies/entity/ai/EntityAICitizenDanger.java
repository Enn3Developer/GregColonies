package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class EntityAICitizenDanger extends EntityAIBase {

    private static final double ESCAPE_SPEED = 1.3D;

    private static final int ESCAPE_RADIUS = 12;

    private static final int SEARCH_HEIGHT = 4;

    private static final int WATER_RADIUS = 10;

    private static final int LOW_AIR = 200;

    private static final int TIMEOUT = 200;

    private static final int REPATH_INTERVAL = 10;

    private static final int SEARCH_INTERVAL = 10;

    private static final double REACHED_SQ = 2.25D;

    private final EntityCitizen citizen;

    private int x;

    private int y;

    private int z;

    private int ticks;

    private int searchCooldown;

    public EntityAICitizenDanger(EntityCitizen citizen) {
        this.citizen = citizen;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        boolean danger = Hazards.isInDanger(citizen) || isDrowning();
        if (!danger && !citizen.isBurning()) {
            searchCooldown = 0;
            return false;
        }
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }
        int[] spot = danger ? Hazards.findEscapeSpot(citizen, ESCAPE_RADIUS, SEARCH_HEIGHT)
            : Hazards.findWater(citizen, WATER_RADIUS, SEARCH_HEIGHT);
        if (spot == null) {
            searchCooldown = SEARCH_INTERVAL;
            return false;
        }
        x = spot[0];
        y = spot[1];
        z = spot[2];
        return true;
    }

    @Override
    public void startExecuting() {
        ticks = 0;
        escape();
    }

    @Override
    public boolean continueExecuting() {
        return ticks <= TIMEOUT && (Hazards.isInDanger(citizen) || citizen.isBurning() || isDrowning())
            && citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) > REACHED_SQ;
    }

    private boolean isDrowning() {
        return citizen.isInWater() && citizen.getAir() < LOW_AIR;
    }

    @Override
    public void updateTask() {
        ticks++;
        if (citizen.handleLavaMovement() || citizen.isInWater()) {
            citizen.getJumpHelper()
                .setJumping();
        }
        if (ticks % REPATH_INTERVAL == 0 && citizen.getNavigator()
            .noPath()) {
            escape();
        }
    }

    @Override
    public void resetTask() {
        citizen.getNavigator()
            .clearPathEntity();
    }

    private void escape() {
        if (citizen.getNavigator()
            .tryMoveToXYZ(x + 0.5D, y, z + 0.5D, ESCAPE_SPEED)) {
            return;
        }
        citizen.getMoveHelper()
            .setMoveTo(x + 0.5D, y, z + 0.5D, ESCAPE_SPEED);
    }
}
