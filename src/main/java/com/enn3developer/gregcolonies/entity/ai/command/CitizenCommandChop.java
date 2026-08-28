package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenCommandChop extends CitizenCommandHarvest {

    public static final String ID = "chop";

    public static final int MAX_SIDE = 32;

    private static final int SCAN_DEPTH = 48;

    private static final int MAX_TREE_LOGS = 400;

    private static final int MAX_TREE_HEIGHT = 40;

    private static final int MAX_TREE_SPREAD = 8;

    private final List<int[]> bases = new ArrayList<>();

    private final List<int[]> logs = new ArrayList<>();

    private boolean scanned;

    private int baseX;

    private int baseY;

    private int baseZ;

    public CitizenCommandChop() {}

    public CitizenCommandChop(int x1, int y1, int z1, int x2, int y2, int z2) {
        super(x1, y1, z1, x2, y2, z2);
        area.capSide(MAX_SIDE);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected Block referenceBlock() {
        return Blocks.log;
    }

    @Override
    protected String unitName() {
        return "logs";
    }

    @Override
    protected boolean canTunnel() {
        return false;
    }

    @Override
    protected void resetWork() {
        bases.clear();
        logs.clear();
        scanned = false;
    }

    @Override
    protected void onHarvested(EntityCitizen citizen, int x, int y, int z) {}

    @Override
    protected void onAbandon(EntityCitizen citizen) {
        logs.clear();
    }

    @Override
    protected boolean acquireTarget(EntityCitizen citizen) {
        World world = citizen.worldObj;
        while (true) {
            while (!logs.isEmpty()) {
                int[] log = logs.remove(0);
                if (WorkBlocks.isLog(world, log[0], log[1], log[2])) {
                    setTarget(log[0], log[1], log[2], baseX, baseY, baseZ);
                    return true;
                }
            }
            if (!scanned) {
                scanBases(citizen);
                scanned = true;
            }
            int[] base = WorkBlocks.takeNearest(citizen.posX, citizen.posY, citizen.posZ, bases);
            if (base == null) {
                return false;
            }
            if (isSkipped(base[0], base[1], base[2]) || !WorkBlocks.isLog(world, base[0], base[1], base[2])) {
                continue;
            }
            List<int[]> tree = collectTree(world, base[0], base[1], base[2]);
            if (tree == null) {
                continue;
            }
            logs.addAll(tree);
            baseX = base[0];
            baseY = base[1];
            baseZ = base[2];
        }
    }

    private void scanBases(EntityCitizen citizen) {
        World world = citizen.worldObj;
        bases.clear();
        for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
            for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                if (!world.blockExists(x, 64, z)) {
                    continue;
                }
                int top = Math.min(world.getHeightValue(x, z), world.getHeight() - 2);
                int bottom = Math.max(1, top - SCAN_DEPTH);
                for (int y = top; y >= bottom; y--) {
                    if (!WorkBlocks.isLog(world, x, y, z)) {
                        continue;
                    }
                    if (WorkBlocks.isLog(world, x, y - 1, z)) {
                        continue;
                    }
                    if (WorkBlocks.isTreeSoil(world, x, y - 1, z) && !isSkipped(x, y, z)) {
                        bases.add(new int[] { x, y, z });
                    }
                    break;
                }
            }
        }
    }

    private List<int[]> collectTree(World world, int rootX, int rootY, int rootZ) {
        List<int[]> tree = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { rootX, rootY, rootZ });
        seen.add(WorkBlocks.pack(rootX, rootY, rootZ));
        boolean leaves = false;

        while (!queue.isEmpty() && tree.size() < MAX_TREE_LOGS) {
            int[] position = queue.poll();
            tree.add(position);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        int x = position[0] + dx;
                        int y = position[1] + dy;
                        int z = position[2] + dz;
                        if (y > rootY + MAX_TREE_HEIGHT || Math.abs(x - rootX) > MAX_TREE_SPREAD
                            || Math.abs(z - rootZ) > MAX_TREE_SPREAD) {
                            continue;
                        }
                        if (!seen.add(WorkBlocks.pack(x, y, z))) {
                            continue;
                        }
                        if (WorkBlocks.isLog(world, x, y, z)) {
                            queue.add(new int[] { x, y, z });
                        } else if (WorkBlocks.isLeaves(world, x, y, z)) {
                            leaves = true;
                        }
                    }
                }
            }
        }

        if (!leaves) {
            return null;
        }
        tree.sort(Comparator.<int[]>comparingInt(position -> position[1]));
        return tree;
    }
}
