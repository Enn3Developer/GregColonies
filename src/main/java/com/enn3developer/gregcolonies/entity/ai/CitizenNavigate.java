package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenNavigate extends PathNavigate {

    private static final int FAILURE_COOLDOWN = 40;

    private final EntityLiving entity;

    private boolean canSwim;

    private boolean enterDoors = true;

    private long failedFrom;

    private long failedTo;

    private long failedAt = Long.MIN_VALUE;

    public CitizenNavigate(EntityLiving entity, World world) {
        super(entity, world);
        this.entity = entity;
    }

    @Override
    public void setCanSwim(boolean canSwim) {
        super.setCanSwim(canSwim);
        this.canSwim = canSwim;
    }

    @Override
    public void setEnterDoors(boolean enterDoors) {
        super.setEnterDoors(enterDoors);
        this.enterDoors = enterDoors;
    }

    @Override
    public PathEntity getPathToXYZ(double x, double y, double z) {
        return findPath(MathHelper.floor_double(x) + 0.5D, (int) y + 0.5D, MathHelper.floor_double(z) + 0.5D);
    }

    @Override
    public PathEntity getPathToEntityLiving(Entity target) {
        return findPath(target.posX, target.boundingBox.minY, target.posZ);
    }

    @Override
    public boolean setPath(PathEntity path, double speed) {
        return super.setPath(trim(path), speed);
    }

    private PathEntity findPath(double x, double y, double z) {
        if (!canNavigate()) {
            return null;
        }

        long from = packEntity();
        long to = WorkBlocks.pack(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
        long now = entity.worldObj.getTotalWorldTime();
        if (from == failedFrom && to == failedTo && now - failedAt < FAILURE_COOLDOWN) {
            return null;
        }

        PathEntity path = finder().createPath(x, y, z, getPathSearchRange());
        if (path == null) {
            failedFrom = from;
            failedTo = to;
            failedAt = now;
        }
        return path;
    }

    private long packEntity() {
        return WorkBlocks.pack(
            MathHelper.floor_double(entity.posX),
            MathHelper.floor_double(entity.boundingBox.minY),
            MathHelper.floor_double(entity.posZ));
    }

    private CitizenPathFinder finder() {
        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.posY);
        int z = MathHelper.floor_double(entity.posZ);
        boolean keepAway = !Hazards.isDeadlyStep(world, x, y, z) && !Hazards.isBesideDeadly(world, x, y, z);
        return new CitizenPathFinder(entity, enterDoors, getCanBreakDoors(), getAvoidsWater(), canSwim, keepAway);
    }

    private boolean canNavigate() {
        return entity.onGround || canSwim && (entity.isInWater() || entity.handleLavaMovement());
    }

    private PathEntity trim(PathEntity path) {
        World world = entity.worldObj;
        if (path == null || world == null) {
            return path;
        }

        int length = path.getCurrentPathLength();
        int end = 0;
        if (Hazards.isInDanger(entity)) {
            while (end < length && isDeadly(world, path.getPathPointFromIndex(end))) {
                end++;
            }
        }
        while (end < length && !isDeadly(world, path.getPathPointFromIndex(end))) {
            end++;
        }

        if (end >= length) {
            return path;
        }
        if (end == 0) {
            return null;
        }

        PathPoint[] points = new PathPoint[end];
        for (int i = 0; i < end; i++) {
            points[i] = path.getPathPointFromIndex(i);
        }
        return new PathEntity(points);
    }

    private static boolean isDeadly(World world, PathPoint point) {
        return Hazards.isDeadlyStep(world, point.xCoord, point.yCoord, point.zCoord);
    }
}
