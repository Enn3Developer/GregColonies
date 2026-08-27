package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.AxisAlignedBB;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;

public class CitizenCommandGuard extends CitizenCommand {

    public static final String ID = "guard";

    private static final double PATROL_SPEED = 0.6D;

    private static final double ARRIVED_DISTANCE_SQ = 4.0D;

    private static final double SEARCH_RANGE = 24.0D;

    private static final double ATTACK_REACH_SQ = 4.0D;

    private static final double LEAP_MAX_DISTANCE_SQ = 36.0D;

    private static final double LEAP_MIN_DISTANCE_SQ = 4.0D;

    private static final int ATTACK_COOLDOWN = 20;

    private static final int LEAP_COOLDOWN = 30;

    private static final int REPATH_INTERVAL = 10;

    private static final int PATROL_PAUSE = 40;

    private static final int SURFACE_PROBE_Y = 64;

    private static final int PATROL_TIMEOUT = 600;

    private int patrolX;
    private int patrolY;
    private int patrolZ;
    private boolean hasPatrolTarget;
    private int patrolTicks;
    private int patrolPause;
    private int attackCooldown;
    private int leapCooldown;
    private int repathTicks;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean fearsEnemies() {
        return false;
    }

    @Override
    public boolean allowsSleep() {
        return false;
    }

    @Override
    public boolean canBeTakenBy(EntityCitizen citizen) {
        return citizen.getColony() != null;
    }

    @Override
    public void start(EntityCitizen citizen) {
        hasPatrolTarget = false;
        patrolPause = 0;
        attackCooldown = 0;
        leapCooldown = 0;
        repathTicks = 0;
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony == null) {
            return CitizenCommandResult.FAILED;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (leapCooldown > 0) {
            leapCooldown--;
        }

        EntityLivingBase target = findTarget(citizen, colony);
        if (target != null) {
            hasPatrolTarget = false;
            chase(citizen, target);
        } else {
            citizen.setSprinting(false);
            patrol(citizen, colony);
        }
        return CitizenCommandResult.RUNNING;
    }

    private EntityLivingBase findTarget(EntityCitizen citizen, Colony colony) {
        AxisAlignedBB box = citizen.boundingBox.expand(SEARCH_RANGE, SEARCH_RANGE * 0.5D, SEARCH_RANGE);
        List<?> candidates = citizen.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);

        EntityLivingBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object candidate : candidates) {
            if (!(candidate instanceof IMob)) {
                continue;
            }
            EntityLivingBase mob = (EntityLivingBase) candidate;
            if (!mob.isEntityAlive() || !colony.isInside(mob.dimension, mob.posX, mob.posZ)) {
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

    private void chase(EntityCitizen citizen, EntityLivingBase target) {
        citizen.setSprinting(true);
        citizen.getLookHelper()
            .setLookPositionWithEntity(target, 30.0F, 30.0F);

        double distanceSq = citizen.getDistanceSqToEntity(target);
        if (--repathTicks <= 0 || citizen.getNavigator()
            .noPath()) {
            repathTicks = REPATH_INTERVAL;
            citizen.getNavigator()
                .tryMoveToEntityLiving(target, citizen.panicSpeed());
        }

        if (leapCooldown <= 0 && citizen.onGround
            && distanceSq > LEAP_MIN_DISTANCE_SQ
            && distanceSq < LEAP_MAX_DISTANCE_SQ) {
            leapCooldown = LEAP_COOLDOWN;
            citizen.leapTowards(target);
        }

        if (attackCooldown <= 0 && distanceSq <= ATTACK_REACH_SQ && citizen.canEntityBeSeen(target)) {
            attackCooldown = ATTACK_COOLDOWN;
            citizen.attackTarget(target);
        }
    }

    private void patrol(EntityCitizen citizen, Colony colony) {
        if (patrolPause > 0) {
            patrolPause--;
            return;
        }

        if (!hasPatrolTarget) {
            pickPatrolTarget(citizen, colony);
            return;
        }

        double distanceSq = citizen.getDistanceSq(patrolX + 0.5D, patrolY, patrolZ + 0.5D);
        if (distanceSq < ARRIVED_DISTANCE_SQ || ++patrolTicks > PATROL_TIMEOUT) {
            hasPatrolTarget = false;
            patrolPause = PATROL_PAUSE;
            citizen.getNavigator()
                .clearPathEntity();
            return;
        }

        if (citizen.getNavigator()
            .noPath()) {
            pathTowards(citizen);
        }
    }

    private void pickPatrolTarget(EntityCitizen citizen, Colony colony) {
        double angle = citizen.getRNG()
            .nextDouble() * Math.PI
            * 2.0D;
        double radius = Config.colonyRadius * Math.sqrt(
            citizen.getRNG()
                .nextDouble());
        int x = (int) Math.round(colony.getX() + Math.cos(angle) * radius);
        int z = (int) Math.round(colony.getZ() + Math.sin(angle) * radius);
        if (!citizen.worldObj.blockExists(x, SURFACE_PROBE_Y, z)) {
            patrolPause = PATROL_PAUSE;
            return;
        }
        patrolX = x;
        patrolZ = z;
        patrolY = citizen.worldObj.getTopSolidOrLiquidBlock(patrolX, patrolZ);
        hasPatrolTarget = true;
        patrolTicks = 0;
        pathTowards(citizen);
    }

    private boolean pathTowards(EntityCitizen citizen) {
        return citizen.travelTo(patrolX + 0.5D, patrolY, patrolZ + 0.5D, PATROL_SPEED);
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.setSprinting(false);
        citizen.getNavigator()
            .clearPathEntity();
    }
}
