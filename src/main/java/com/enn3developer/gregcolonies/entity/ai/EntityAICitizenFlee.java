package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.Vec3;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class EntityAICitizenFlee extends EntityAIBase {

    private static final double SAFE_DISTANCE_SQ = 18.0D * 18.0D;

    private static final int ESCAPE_RANGE = 16;

    private static final int ESCAPE_HEIGHT = 7;

    private static final int MAX_STUCK = 5;

    private final EntityCitizen citizen;
    private EntityLivingBase threat;

    private int stuck;

    public EntityAICitizenFlee(EntityCitizen citizen) {
        this.citizen = citizen;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!citizen.isAfraid()) {
            return false;
        }
        threat = findThreat();
        return threat != null && flee();
    }

    @Override
    public boolean continueExecuting() {
        return threat != null && threat.isEntityAlive()
            && citizen.isAfraid()
            && stuck < MAX_STUCK
            && citizen.getDistanceSqToEntity(threat) < SAFE_DISTANCE_SQ;
    }

    @Override
    public void startExecuting() {
        stuck = 0;
        citizen.setSprinting(true);
        citizen.getNavigator()
            .setSpeed(citizen.panicSpeed());
    }

    @Override
    public void updateTask() {
        citizen.getLookHelper()
            .setLookPositionWithEntity(threat, 30.0F, 30.0F);
        if (citizen.getNavigator()
            .noPath()) {
            stuck = flee() ? 0 : stuck + 1;
        }
    }

    @Override
    public void resetTask() {
        citizen.setSprinting(false);
        citizen.getNavigator()
            .clearPathEntity();
        threat = null;
    }

    private boolean flee() {
        Vec3 escape = RandomPositionGenerator.findRandomTargetBlockAwayFrom(
            citizen,
            ESCAPE_RANGE,
            ESCAPE_HEIGHT,
            Vec3.createVectorHelper(threat.posX, threat.posY, threat.posZ));
        if (escape == null) {
            return false;
        }
        return citizen.getNavigator()
            .tryMoveToXYZ(escape.xCoord, escape.yCoord, escape.zCoord, citizen.panicSpeed());
    }

    private EntityLivingBase findThreat() {
        return Hazards.findThreat(citizen, Hazards.FEAR_RANGE);
    }
}
