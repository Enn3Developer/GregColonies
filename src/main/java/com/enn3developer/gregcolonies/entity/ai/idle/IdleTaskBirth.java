package com.enn3developer.gregcolonies.entity.ai.idle;

import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.CitizenGender;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public class IdleTaskBirth extends AutoTask {

    public static final String ID = "family";

    private static final int SEARCH_RADIUS = 16;

    private static final int SEARCH_HEIGHT = 6;

    private static final int TRAVEL_TIMEOUT = 400;

    private static final int BIRTH_COOLDOWN = 24000;

    private static final int RETRY_COOLDOWN = 1200;

    private static final double SPEED = 0.6D;

    private static final double REACH_SQ = 4.0D;

    private static final double REACH_HEIGHT = 2.0D;

    private EntityCitizen partner;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!ready(world) || citizen.isChild() || citizen.getGender() != CitizenGender.FEMALE) {
            return false;
        }
        if (citizen.getRNG()
            .nextInt(Config.birthChance) != 0) {
            return false;
        }

        int dimension = world.provider.dimensionId;
        if (dimension != colony.getDimension() || !colony.isInside(dimension, citizen.posX, citizen.posZ)) {
            return false;
        }

        partner = findPartner(citizen, colony);
        if (partner == null) {
            delay(world, RETRY_COOLDOWN);
            return false;
        }
        return true;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        resetTravel();
        pathTowards(citizen, partner.posX, partner.posY, partner.posZ, SPEED);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (partner.worldObj != world || !isAvailable(partner, colony)) {
            return false;
        }

        if (isTogether(citizen, partner)) {
            birth(citizen, partner);
            delay(world, BIRTH_COOLDOWN);
            return false;
        }

        if (!chase(citizen, partner.posX, partner.posY, partner.posZ, SPEED, TRAVEL_TIMEOUT)) {
            delay(world, RETRY_COOLDOWN);
            return false;
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        partner = null;
        citizen.getNavigator()
            .clearPathEntity();
    }

    private static void birth(EntityCitizen mother, EntityCitizen father) {
        EntityCitizen child = mother.createChild(father);
        child.setLocationAndAngles(mother.posX, mother.posY, mother.posZ, mother.rotationYaw, 0.0F);
        child.ensureName();
        mother.worldObj.spawnEntityInWorld(child);
    }

    private static EntityCitizen findPartner(EntityCitizen citizen, Colony colony) {
        AxisAlignedBB box = citizen.boundingBox.expand(SEARCH_RADIUS, SEARCH_HEIGHT, SEARCH_RADIUS);
        List<?> found = citizen.worldObj.getEntitiesWithinAABB(EntityCitizen.class, box);
        EntityCitizen best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (Object entry : found) {
            EntityCitizen other = (EntityCitizen) entry;
            if (!isAvailable(other, colony)) {
                continue;
            }
            double distanceSq = citizen.getDistanceSqToEntity(other);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = other;
            }
        }
        return best;
    }

    private static boolean isAvailable(EntityCitizen partner, Colony colony) {
        return partner.isEntityAlive() && partner.getColonyId() == colony.getId()
            && partner.getGender() == CitizenGender.MALE
            && !partner.isChild()
            && !partner.isViewed()
            && partner.getLivingTask()
                .isEmpty()
            && !partner.getCommands()
                .hasWork();
    }

    private static boolean isTogether(EntityCitizen citizen, EntityCitizen partner) {
        double dx = citizen.posX - partner.posX;
        double dz = citizen.posZ - partner.posZ;
        return dx * dx + dz * dz <= REACH_SQ && Math.abs(citizen.posY - partner.posY) <= REACH_HEIGHT;
    }
}
