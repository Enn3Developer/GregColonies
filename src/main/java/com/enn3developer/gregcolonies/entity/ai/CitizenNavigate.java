package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.BlockKey;

public class CitizenNavigate extends PathNavigate {

    private static final int FAILURE_COOLDOWN = 40;

    private static final int CLIMB_UP = 1;

    private static final int CLIMB_DOWN = -1;

    private static final int CLIMB_NONE = 0;

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
    public void onUpdateNavigation() {
        boolean ground = entity.onGround;
        entity.onGround = ground || onClimbable(0);
        super.onUpdateNavigation();
        entity.onGround = ground;
    }

    public int getClimbDirection() {
        if (noPath()) {
            return CLIMB_NONE;
        }
        PathEntity path = getPath();
        int index = path.getCurrentPathIndex();
        if (index >= path.getCurrentPathLength()) {
            return CLIMB_NONE;
        }

        int y = MathHelper.floor_double(entity.boundingBox.minY);
        int wanted = path.getPathPointFromIndex(index).yCoord;
        if (wanted > y) {
            return onClimbable(0) ? CLIMB_UP : CLIMB_NONE;
        }
        if (wanted < y) {
            return onClimbable(0) || onClimbable(-1) ? CLIMB_DOWN : CLIMB_NONE;
        }
        return CLIMB_NONE;
    }

    private boolean onClimbable(int offset) {
        return CitizenPathFinder.isClimbable(
            entity.worldObj,
            MathHelper.floor_double(entity.posX),
            MathHelper.floor_double(entity.boundingBox.minY) + offset,
            MathHelper.floor_double(entity.posZ),
            entity);
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
        long to = BlockKey.pack(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
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
        return BlockKey.pack(
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
        return entity.onGround || onClimbable(0) || canSwim && (entity.isInWater() || entity.handleLavaMovement());
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
