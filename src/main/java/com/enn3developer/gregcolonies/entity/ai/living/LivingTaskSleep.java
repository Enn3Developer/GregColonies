package com.enn3developer.gregcolonies.entity.ai.living;

import net.minecraft.block.BlockBed;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyHome;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.colony.Homes;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.Hazards;
import com.enn3developer.gregcolonies.entity.ai.NearestSpot;
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

    private static final int RETRY_DELAY = 600;

    private static final double BED_HEIGHT = 0.5625D;

    private static final double BODY_INSET = 0.4D;

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
        if (!isNight(world) || !ready(world) || !citizen.allowsSleep()) {
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

        ColonyHome home = home(citizen, colony, entry);
        if (entry.hasBed()) {
            if (keepsBed(world, colony, entry, home)) {
                bedX = entry.getBedX();
                bedY = entry.getBedY();
                bedZ = entry.getBedZ();
                return true;
            }
            ColonyManager.registry(world)
                .releaseBed(colony.getId(), citizen.getUniqueID());
        }
        if (home == null ? claimBed(citizen, colony, dimension) : claimBedAtHome(citizen, colony, home)) {
            return true;
        }
        delay(citizen);
        return false;
    }

    private static boolean keepsBed(World world, Colony colony, ColonyCitizen entry, ColonyHome home) {
        if (!isBed(world, entry.getBedX(), entry.getBedY(), entry.getBedZ())) {
            return false;
        }
        ColonyHome standing = colony.getHomeAt(entry.getBedX(), entry.getBedY(), entry.getBedZ());
        return standing == home;
    }

    private static ColonyHome home(EntityCitizen citizen, Colony colony, ColonyCitizen entry) {
        World world = citizen.worldObj;
        ColonyRegistry registry = ColonyManager.registry(world);
        ColonyHome current = colony.getHome(entry.getHomeId());
        if (current != null) {
            return current;
        }
        if (entry.hasHome()) {
            registry.releaseHome(colony.getId(), entry.getId());
        }

        ColonyHome best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (ColonyHome candidate : colony.getHomes()) {
            if (Homes.rescan(world, candidate)) {
                registry.markDirty();
            }
            if (colony.homeOccupants(candidate.getId()) >= candidate.getBeds()) {
                continue;
            }
            WorkArea area = candidate.getArea();
            double distanceSq = citizen
                .getDistanceSq(area.getCenterX() + 0.5D, area.getCenterY(), area.getCenterZ() + 0.5D);
            if (distanceSq >= bestDistanceSq) {
                continue;
            }
            best = candidate;
            bestDistanceSq = distanceSq;
        }
        if (best == null) {
            return null;
        }
        return registry.claimHome(colony.getId(), entry.getId(), best.getId()) ? best : null;
    }

    private boolean claimBedAtHome(EntityCitizen citizen, Colony colony, ColonyHome home) {
        World world = citizen.worldObj;
        WorkArea area = home.getArea();
        if (!Homes.isLoaded(world, area)) {
            return false;
        }

        int[] bed = NearestSpot.in(
            citizen,
            area.getMinX(),
            area.getMinY(),
            area.getMinZ(),
            area.getMaxX(),
            area.getMaxY(),
            area.getMaxZ(),
            (w, x, y, z) -> isBed(w, x, y, z) && colony.isBedFree(citizen.getUniqueID(), x, y, z));
        return take(citizen, colony, bed);
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        resetTravel();
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
        ColonyCitizen entry = colony.getCitizen(citizen.getUniqueID());
        if (entry == null || !entry.isBedAt(bedX, bedY, bedZ)) {
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

        if (!travel(citizen, bedX + 0.5D, bedY, bedZ + 0.5D, SPEED, TRAVEL_TIMEOUT)) {
            delay(citizen);
            return false;
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

        int[] bed = NearestSpot.in(
            citizen,
            cx - SEARCH_RADIUS,
            minY,
            cz - SEARCH_RADIUS,
            cx + SEARCH_RADIUS,
            maxY,
            cz + SEARCH_RADIUS,
            (w, x, y, z) -> isBed(w, x, y, z) && colony.isInside(dimension, x + 0.5D, z + 0.5D)
                && colony.getHomeAt(x, y, z) == null
                && colony.isBedFree(citizen.getUniqueID(), x, y, z));
        return take(citizen, colony, bed);
    }

    private boolean take(EntityCitizen citizen, Colony colony, int[] bed) {
        if (bed == null) {
            return false;
        }
        bedX = bed[0];
        bedY = bed[1];
        bedZ = bed[2];
        return ColonyManager.registry(citizen.worldObj)
            .claimBed(colony.getId(), citizen.getUniqueID(), bedX, bedY, bedZ);
    }

    private boolean pathToBed(EntityCitizen citizen) {
        return pathTowards(citizen, bedX + 0.5D, bedY, bedZ + 0.5D, SPEED);
    }

    private void delay(EntityCitizen citizen) {
        delay(citizen.worldObj, RETRY_DELAY);
    }

    private static boolean isThreatened(EntityCitizen citizen) {
        return citizen.isAfraid() && Hazards.findThreat(citizen, Hazards.FEAR_RANGE) != null;
    }

    private static boolean isNight(World world) {
        long time = world.getWorldTime() % DAY_LENGTH;
        return time >= NIGHT_START && time < NIGHT_END;
    }

    private static boolean isBed(World world, int x, int y, int z) {
        return Homes.isBed(world, x, y, z);
    }
}
