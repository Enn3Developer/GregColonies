package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

public class CitizenNavigate extends PathNavigate {

    private static final int XYZ_MARGIN = 8;

    private static final int ENTITY_MARGIN = 16;

    private final EntityLiving entity;

    private boolean canSwim;

    private boolean enterDoors = true;

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
        if (!canNavigate()) {
            return null;
        }

        float range = getPathSearchRange();
        int margin = (int) (range + XYZ_MARGIN);
        return finder(margin)
            .createEntityPathTo(entity, MathHelper.floor_double(x), (int) y, MathHelper.floor_double(z), range);
    }

    @Override
    public PathEntity getPathToEntityLiving(Entity target) {
        if (!canNavigate()) {
            return null;
        }

        float range = getPathSearchRange();
        return finder((int) (range + ENTITY_MARGIN)).createEntityPathTo(entity, target, range);
    }

    @Override
    public boolean setPath(PathEntity path, double speed) {
        return super.setPath(trim(path), speed);
    }

    private PathFinder finder(int margin) {
        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.posY);
        int z = MathHelper.floor_double(entity.posZ);
        ChunkCache cache = new ChunkCache(
            world,
            x - margin,
            y - margin,
            z - margin,
            x + margin,
            y + margin,
            z + margin,
            0);
        boolean keepAway = !Hazards.isDeadlyStep(world, x, y, z) && !Hazards.isBesideDeadly(world, x, y, z);
        return new CitizenPathFinder(cache, enterDoors, getCanBreakDoors(), getAvoidsWater(), canSwim, keepAway);
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
