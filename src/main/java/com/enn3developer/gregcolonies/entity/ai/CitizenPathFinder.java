package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class CitizenPathFinder extends PathFinder {

    private static final int LAVA = -2;

    private final boolean keepAway;

    public CitizenPathFinder(IBlockAccess access, boolean enterDoors, boolean breakDoors, boolean avoidsWater,
        boolean canSwim, boolean keepAway) {
        super(access, enterDoors, breakDoors, avoidsWater, canSwim);
        this.keepAway = keepAway;
    }

    @Override
    public int getVerticalOffset(Entity entity, int x, int y, int z, PathPoint size) {
        World world = entity.worldObj;
        for (int bx = x; bx < x + size.xCoord; bx++) {
            for (int by = y; by < y + size.yCoord; by++) {
                for (int bz = z; bz < z + size.zCoord; bz++) {
                    if (Hazards.isDeadly(world, bx, by, bz)) {
                        return LAVA;
                    }
                }
            }
        }
        if (keepAway && Hazards.isBesideDeadly(world, x, y, z)) {
            return LAVA;
        }
        return super.getVerticalOffset(entity, x, y, z, size);
    }
}
