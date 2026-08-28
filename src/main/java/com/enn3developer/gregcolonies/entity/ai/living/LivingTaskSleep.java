package com.enn3developer.gregcolonies.entity.ai.living;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.Hazards;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public class LivingTaskSleep extends AutoTask {

    public static final String ID = "sleep";

    private static final int NIGHT_START = 13000;

    private static final int NIGHT_END = 23000;

    private static final int DAY_LENGTH = 24000;

    private static final int SEARCH_RADIUS = 12;

    private static final int SEARCH_HEIGHT = 4;

    private static final double SPEED = 0.7D;

    private static final double REACH_SQ = 4.0D;

    private static final int TRAVEL_TIMEOUT = 1200;

    private static final int REPATH_INTERVAL = 10;

    private static final int RETRY_DELAY = 600;

    private static final double BED_HEIGHT = 0.5625D;

    private static final double BODY_INSET = 0.4D;

    private long nextAttempt;

    private int travelTicks;

    private int bedX;

    private int bedY;

    private int bedZ;

    private double sleepX;

    private double sleepY;

    private double sleepZ;

    private float sleepYaw;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!isNight(world) || world.getTotalWorldTime() < nextAttempt || !citizen.allowsSleep()) {
            return false;
        }
        if (isThreatened(citizen)) {
            return false;
        }
        int dimension = world.provider.dimensionId;
        if (dimension != colony.getDimension()) {
            return false;
        }

        ColonyCitizen entry = colony.getCitizen(citizen.getUniqueID());
        if (entry == null) {
            return false;
        }
        if (entry.hasBed()) {
            if (isBed(world, entry.getBedX(), entry.getBedY(), entry.getBedZ())) {
                bedX = entry.getBedX();
                bedY = entry.getBedY();
                bedZ = entry.getBedZ();
                return true;
            }
            ColonyManager.registry(world)
                .releaseBed(colony.getId(), citizen.getUniqueID());
        }
        if (claimBed(citizen, colony, dimension)) {
            return true;
        }
        delay(citizen);
        return false;
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        travelTicks = 0;
        pathToBed(citizen);
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!isNight(world) || isThreatened(citizen)) {
            return false;
        }
        if (!isBed(world, bedX, bedY, bedZ)) {
            ColonyManager.registry(world)
                .releaseBed(colony.getId(), citizen.getUniqueID());
            return false;
        }

        if (citizen.isAsleep()) {
            keepInBed(citizen);
            return true;
        }
        if (citizen.getDistanceSq(bedX + 0.5D, bedY, bedZ + 0.5D) <= REACH_SQ) {
            lieDown(citizen, world);
            return true;
        }

        if (++travelTicks > TRAVEL_TIMEOUT) {
            delay(citizen);
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 0 && citizen.getNavigator()
            .noPath()) {
            pathToBed(citizen);
        }
        return true;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.setAsleep(false);
        citizen.getNavigator()
            .clearPathEntity();
    }

    private void lieDown(EntityCitizen citizen, World world) {
        int[] offset = BlockBed.field_149981_a[world.getBlockMetadata(bedX, bedY, bedZ) & 3];
        sleepX = bedX - offset[0] + 0.5D - offset[0] * BODY_INSET;
        sleepY = bedY + BED_HEIGHT;
        sleepZ = bedZ - offset[1] + 0.5D - offset[1] * BODY_INSET;
        sleepYaw = (float) (Math.atan2(-offset[0], offset[1]) * 180.0D / Math.PI);

        citizen.getNavigator()
            .clearPathEntity();
        citizen.setPositionAndRotation(sleepX, sleepY, sleepZ, sleepYaw, 0.0F);
        citizen.setAsleep(true);
        keepInBed(citizen);
    }

    private void keepInBed(EntityCitizen citizen) {
        citizen.setPosition(sleepX, sleepY, sleepZ);
        citizen.motionX = 0.0D;
        citizen.motionY = 0.0D;
        citizen.motionZ = 0.0D;
        citizen.rotationYaw = sleepYaw;
        citizen.renderYawOffset = sleepYaw;
        citizen.rotationYawHead = sleepYaw;
        citizen.prevRenderYawOffset = sleepYaw;
        citizen.prevRotationYawHead = sleepYaw;
        citizen.rotationPitch = 0.0F;
    }

    private boolean claimBed(EntityCitizen citizen, Colony colony, int dimension) {
        World world = citizen.worldObj;
        int cx = MathHelper.floor_double(citizen.posX);
        int cy = MathHelper.floor_double(citizen.posY);
        int cz = MathHelper.floor_double(citizen.posZ);
        int minY = Math.max(cy - SEARCH_HEIGHT, 1);
        int maxY = Math.min(cy + SEARCH_HEIGHT, 254);
        if (!world.checkChunksExist(
            cx - SEARCH_RADIUS,
            minY,
            cz - SEARCH_RADIUS,
            cx + SEARCH_RADIUS,
            maxY,
            cz + SEARCH_RADIUS)) {
            return false;
        }

        double bestDistanceSq = Double.MAX_VALUE;
        boolean found = false;
        for (int y = minY; y <= maxY; y++) {
            for (int x = cx - SEARCH_RADIUS; x <= cx + SEARCH_RADIUS; x++) {
                for (int z = cz - SEARCH_RADIUS; z <= cz + SEARCH_RADIUS; z++) {
                    if (!isBed(world, x, y, z) || !colony.isInside(dimension, x + 0.5D, z + 0.5D)) {
                        continue;
                    }
                    double distanceSq = citizen.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq >= bestDistanceSq || !colony.isBedFree(citizen.getUniqueID(), x, y, z)) {
                        continue;
                    }
                    bestDistanceSq = distanceSq;
                    bedX = x;
                    bedY = y;
                    bedZ = z;
                    found = true;
                }
            }
        }
        return found && ColonyManager.registry(world)
            .claimBed(colony.getId(), citizen.getUniqueID(), bedX, bedY, bedZ);
    }

    private boolean pathToBed(EntityCitizen citizen) {
        return pathTowards(citizen, bedX + 0.5D, bedY, bedZ + 0.5D, SPEED);
    }

    private void delay(EntityCitizen citizen) {
        nextAttempt = citizen.worldObj.getTotalWorldTime() + RETRY_DELAY;
    }

    private static boolean isThreatened(EntityCitizen citizen) {
        return citizen.isAfraid() && Hazards.findThreat(citizen, Hazards.FEAR_RANGE) != null;
    }

    private static boolean isNight(World world) {
        long time = world.getWorldTime() % DAY_LENGTH;
        return time >= NIGHT_START && time < NIGHT_END;
    }

    private static boolean isBed(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        Block block = world.getBlock(x, y, z);
        return block instanceof BlockBed && BlockBed.isBlockHeadOfBed(world.getBlockMetadata(x, y, z));
    }
}
