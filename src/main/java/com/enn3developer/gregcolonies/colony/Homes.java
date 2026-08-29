package com.enn3developer.gregcolonies.colony;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.world.World;

public final class Homes {

    private Homes() {}

    public static boolean isBed(World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        Block block = world.getBlock(x, y, z);
        return block instanceof BlockBed && BlockBed.isBlockHeadOfBed(world.getBlockMetadata(x, y, z));
    }

    public static boolean isLoaded(World world, WorkArea area) {
        return world.checkChunksExist(
            area.getMinX(),
            area.getMinY(),
            area.getMinZ(),
            area.getMaxX(),
            area.getMaxY(),
            area.getMaxZ());
    }

    public static int countBeds(World world, WorkArea area) {
        int beds = 0;
        for (int y = area.getMinY(); y <= area.getMaxY(); y++) {
            for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                    if (isBed(world, x, y, z)) {
                        beds++;
                    }
                }
            }
        }
        return beds;
    }

    public static boolean rescan(World world, ColonyHome home) {
        long time = world.getTotalWorldTime();
        if (!home.needsScan(time) || !isLoaded(world, home.getArea())) {
            return false;
        }
        home.scanned(time);
        int beds = countBeds(world, home.getArea());
        if (beds == home.getBeds()) {
            return false;
        }
        home.setBeds(beds);
        return true;
    }

    public static void rescan(World world, ColonyRegistry registry, Colony colony) {
        boolean changed = false;
        for (ColonyHome home : colony.getHomes()) {
            changed |= rescan(world, home);
        }
        if (changed) {
            registry.markDirty();
        }
    }
}
