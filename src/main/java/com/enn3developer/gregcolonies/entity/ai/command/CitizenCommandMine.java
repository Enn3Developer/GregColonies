package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenCommandMine extends CitizenCommandHarvest {

    public static final String ID = "mine";

    private static final int MIN_SCAN_Y = 1;

    private final List<int[]> ores = new ArrayList<>();

    private boolean scanned;

    public CitizenCommandMine() {}

    public CitizenCommandMine(int x, int z) {
        super((x >> 4) << 4, MIN_SCAN_Y, (z >> 4) << 4, (((x >> 4) << 4) + 15), 255, (((z >> 4) << 4) + 15));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Block referenceBlock() {
        return Blocks.stone;
    }

    @Override
    protected String unitName() {
        return "ores";
    }

    @Override
    protected boolean canTunnel() {
        return true;
    }

    @Override
    protected void resetWork() {
        ores.clear();
        scanned = false;
    }

    @Override
    protected void onHarvested(EntityCitizen citizen, int x, int y, int z) {}

    @Override
    protected boolean acquireTarget(EntityCitizen citizen) {
        World world = citizen.worldObj;
        if (!scanned) {
            if (!world.blockExists(area.getMinX(), 64, area.getMinZ())) {
                return false;
            }
            scanOres(world);
            scanned = true;
        }
        while (true) {
            int[] ore = WorkBlocks.takeNearest(citizen.posX, citizen.posY, citizen.posZ, ores);
            if (ore == null) {
                return false;
            }
            if (isSkipped(ore[0], ore[1], ore[2]) || !WorkBlocks.isBigOre(world, ore[0], ore[1], ore[2])) {
                continue;
            }
            setTarget(ore[0], ore[1], ore[2], ore[0], ore[1], ore[2]);
            return true;
        }
    }

    private void scanOres(World world) {
        ores.clear();
        int top = Math.min(
            area.getMaxY(),
            world.getChunkFromBlockCoords(area.getMinX(), area.getMinZ())
                .getTopFilledSegment() + 15);
        for (int y = top; y >= area.getMinY(); y--) {
            for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                    if (WorkBlocks.isBigOre(world, x, y, z)) {
                        ores.add(new int[] { x, y, z });
                    }
                }
            }
        }
    }
}
