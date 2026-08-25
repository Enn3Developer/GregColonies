package com.enn3developer.gregcolonies.entity.ai;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class EntityAICitizenFlee extends EntityAIBase {

    private static final double FEAR_RANGE = 12.0D;

    private static final double SAFE_DISTANCE_SQ = 18.0D * 18.0D;

    private static final double FLEE_SPEED = 1.3D;

    private static final int ESCAPE_RANGE = 16;

    private static final int ESCAPE_HEIGHT = 7;

    private final EntityCitizen citizen;
    private EntityLivingBase threat;

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
            && citizen.getDistanceSqToEntity(threat) < SAFE_DISTANCE_SQ
            && !citizen.getNavigator()
                .noPath();
    }

    @Override
    public void startExecuting() {
        citizen.setSprinting(true);
    }

    @Override
    public void updateTask() {
        citizen.getLookHelper()
            .setLookPositionWithEntity(threat, 30.0F, 30.0F);
        if (citizen.getNavigator()
            .noPath()) {
            flee();
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
            .tryMoveToXYZ(escape.xCoord, escape.yCoord, escape.zCoord, FLEE_SPEED);
    }

    private EntityLivingBase findThreat() {
        AxisAlignedBB box = citizen.boundingBox.expand(FEAR_RANGE, FEAR_RANGE * 0.5D, FEAR_RANGE);
        List<?> candidates = citizen.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);

        EntityLivingBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object candidate : candidates) {
            if (!(candidate instanceof IMob)) {
                continue;
            }
            EntityLivingBase mob = (EntityLivingBase) candidate;
            if (!mob.isEntityAlive()) {
                continue;
            }
            double distance = citizen.getDistanceSqToEntity(mob);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = mob;
            }
        }
        return best;
    }
}
