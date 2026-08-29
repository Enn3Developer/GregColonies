package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

public final class NearestSpot {

    public interface Test {

        boolean matches(World world, int x, int y, int z);
    }

    private NearestSpot() {}

    public static int[] in(EntityLivingBase entity, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        Test test) {
        World world = entity.worldObj;
        int[] best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double distanceSq = entity.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distanceSq >= bestDistanceSq || !test.matches(world, x, y, z)) {
                        continue;
                    }
                    bestDistanceSq = distanceSq;
                    best = new int[] { x, y, z };
                }
            }
        }
        return best;
    }
}
