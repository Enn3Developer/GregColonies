package com.enn3developer.gregcolonies.entity.ai.idle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class IdleTaskBuild extends AutoTask {

    public static final String ID = "build";

    private static final double SPEED = 0.6D;

    private static final int RETRY_INTERVAL = 200;

    private static final int TRAVEL_TIMEOUT = 600;

    private static final int REPATH_INTERVAL = 10;

    private static final double REACH = 4.5D;

    private static final int PLACE_INTERVAL = 8;

    private static final int MAX_CANDIDATES = 128;

    private static final int NEEDED_REFRESH = 40;

    private static final int[][] NEIGHBOURS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private static final int PHASE_FETCH = 0;

    private static final int PHASE_PLACE = 1;

    private static final int PHASE_RETURN = 2;

    private final List<int[]> candidates = new ArrayList<>();

    private Set<Integer> needed = Collections.emptySet();

    private long neededAt;

    private boolean neededValid;

    private ItemStack fetched;

    private long nextAttempt;

    private int phase;

    private boolean hasSpot;

    private int spotX;

    private int spotY;

    private int spotZ;

    private int travelTicks;

    private int placeCooldown;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String describe() {
        if (phase == PHASE_RETURN) {
            return "returning materials";
        }
        return phase == PHASE_FETCH ? "fetching materials" : "building";
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!citizen.canWork() || world.getTotalWorldTime() < nextAttempt) {
            return false;
        }
        if (world.provider.dimensionId != colony.getDimension()) {
            return false;
        }
        if (colony.getBuildSite() == null) {
            return fetched != null && colony.hasMaterials();
        }
        return colony.hasMaterials();
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        hasSpot = false;
        ColonyManager.get(citizen.worldObj)
            .releaseBuildSpot(colony.getId(), citizen.getUniqueID());
        travelTicks = 0;
        placeCooldown = 0;
        candidates.clear();
        neededValid = false;
        phase = PHASE_FETCH;
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        BuildSite site = colony.getBuildSite();
        if (site == null) {
            return fetched != null && returnMaterial(citizen, colony);
        }
        if (phase == PHASE_RETURN) {
            return returnMaterial(citizen, colony);
        }

        World world = citizen.worldObj;
        Set<Integer> wanted = needed(world, site);
        ItemStack carrying = carried(citizen, site.getBlueprint(), wanted);
        if (carrying != null) {
            if (hasSpot && !site.needsStack(world, carrying, spotX, spotY, spotZ)) {
                release(citizen, colony);
            }
            if (hasSpot || claim(citizen, colony, site, carrying)) {
                phase = PHASE_PLACE;
                return place(citizen, colony, site, carrying);
            }
        }
        release(citizen, colony);
        if (fetched != null && carrying == null) {
            return returnMaterial(citizen, colony);
        }
        phase = PHASE_FETCH;
        return fetch(citizen, colony, site, wanted);
    }

    @Override
    public void finish(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony != null) {
            release(citizen, colony);
        }
        candidates.clear();
        neededValid = false;
        nextAttempt = citizen.worldObj.getTotalWorldTime() + RETRY_INTERVAL;
        citizen.getNavigator()
            .clearPathEntity();
    }

    private static ItemStack carried(EntityCitizen citizen, Blueprint blueprint, Set<Integer> wanted) {
        return citizen.getInventory()
            .peekMain(stack -> matchesAny(blueprint, wanted, stack));
    }

    private Set<Integer> needed(World world, BuildSite site) {
        long time = world.getTotalWorldTime();
        if (!neededValid || time - neededAt >= NEEDED_REFRESH) {
            needed = neededCells(world, site);
            neededAt = time;
            neededValid = true;
        }
        return needed;
    }

    private boolean fetch(EntityCitizen citizen, Colony colony, BuildSite site, Set<Integer> wanted) {
        if (!colony.hasMaterials() || wanted.isEmpty()) {
            return false;
        }
        int x = colony.getMaterialsX();
        int y = colony.getMaterialsY();
        int z = colony.getMaterialsZ();
        if (!inReach(citizen, x, y, z)) {
            return travel(citizen, x, y + 1, z);
        }

        World world = citizen.worldObj;
        IInventory inventory = Inventories.at(world, x, y, z);
        if (inventory == null) {
            return false;
        }
        Blueprint blueprint = site.getBlueprint();
        ItemStack taken = Inventories
            .extract(inventory, stack -> matchesAny(blueprint, wanted, stack), stackLimit(citizen));
        if (taken == null) {
            return false;
        }
        fetched = taken.copy();
        fetched.stackSize = 1;
        ItemStack rest = citizen.getInventory()
            .store(taken);
        if (rest != null) {
            Inventories.insert(inventory, rest);
        }
        citizen.swingItem();
        travelTicks = 0;
        candidates.clear();
        phase = PHASE_PLACE;
        return true;
    }

    private boolean returnMaterial(EntityCitizen citizen, Colony colony) {
        if (!colony.hasMaterials() || fetched == null) {
            return false;
        }
        phase = PHASE_RETURN;
        int x = colony.getMaterialsX();
        int y = colony.getMaterialsY();
        int z = colony.getMaterialsZ();
        if (!inReach(citizen, x, y, z)) {
            return travel(citizen, x, y + 1, z);
        }
        IInventory inventory = Inventories.at(citizen.worldObj, x, y, z);
        if (inventory == null) {
            return false;
        }
        ItemStack kind = fetched;
        ItemStack held = citizen.getInventory()
            .takeMain(stack -> sameKind(kind, stack));
        while (held != null) {
            ItemStack rest = Inventories.insert(inventory, held);
            if (rest != null) {
                citizen.getInventory()
                    .store(rest);
                break;
            }
            held = citizen.getInventory()
                .takeMain(stack -> sameKind(kind, stack));
        }
        fetched = null;
        citizen.swingItem();
        return false;
    }

    private static boolean sameKind(ItemStack kind, ItemStack stack) {
        return stack != null && kind != null
            && stack.getItem() == kind.getItem()
            && stack.getItemDamage() == kind.getItemDamage();
    }

    private boolean place(EntityCitizen citizen, Colony colony, BuildSite site, ItemStack carrying) {
        World world = citizen.worldObj;
        if (!hasSpot) {
            return true;
        }
        if (standsOn(citizen, spotX, spotY, spotZ) || !inReach(citizen, spotX, spotY, spotZ)) {
            return travelNear(citizen, site);
        }
        if (placeCooldown > 0) {
            placeCooldown--;
            return true;
        }

        int cell = site.cellFor(spotX, spotY, spotZ);
        Blueprint blueprint = site.getBlueprint();
        Block block = blueprint.blockOf(cell);
        ItemStack used = citizen.getInventory()
            .takeMain(stack -> blueprint.matches(cell, stack));
        if (block == null || used == null) {
            release(citizen, colony);
            return true;
        }
        citizen.getNavigator()
            .clearPathEntity();
        world.setBlock(spotX, spotY, spotZ, block, Blueprint.metaOf(cell), 3);
        world.playSoundEffect(
            spotX + 0.5D,
            spotY + 0.5D,
            spotZ + 0.5D,
            block.stepSound.func_150496_b(),
            (block.stepSound.getVolume() + 1.0F) / 2.0F,
            block.stepSound.getPitch() * 0.8F);
        citizen.swingItem();
        release(citizen, colony);
        if (!citizen.getInventory()
            .hasMain(stack -> sameKind(fetched, stack))) {
            fetched = null;
        }
        placeCooldown = PLACE_INTERVAL;
        travelTicks = 0;
        return true;
    }

    private boolean travelNear(EntityCitizen citizen, BuildSite site) {
        World world = citizen.worldObj;
        int bestX = spotX + 1;
        int bestY = spotY;
        int bestZ = spotZ;
        double best = Double.MAX_VALUE;
        for (int level = 0; level <= 1; level++) {
            int y = spotY - level;
            for (int[] step : NEIGHBOURS) {
                int x = spotX + step[0];
                int z = spotZ + step[1];
                if (site.cellFor(x, y, z) != Blueprint.AIR || !WorkBlocks.blocksMovement(world, x, y - 1, z)) {
                    continue;
                }
                double distance = citizen.getDistanceSq(x + 0.5D, y, z + 0.5D);
                if (distance < best) {
                    best = distance;
                    bestX = x;
                    bestY = y;
                    bestZ = z;
                }
            }
        }
        return travel(citizen, bestX, bestY, bestZ);
    }

    private boolean claim(EntityCitizen citizen, Colony colony, BuildSite site, ItemStack carrying) {
        World world = citizen.worldObj;
        if (candidates.isEmpty()) {
            scan(world, colony, citizen, site, carrying);
        }
        while (!candidates.isEmpty()) {
            int[] spot = WorkBlocks.takeNearest(citizen.posX, citizen.posY, citizen.posZ, candidates);
            if (spot == null) {
                return false;
            }
            if (!site.needsStack(world, carrying, spot[0], spot[1], spot[2])) {
                continue;
            }
            if (!ColonyManager.get(world)
                .claimBuildSpot(colony.getId(), citizen.getUniqueID(), spot[0], spot[1], spot[2])) {
                continue;
            }
            spotX = spot[0];
            spotY = spot[1];
            spotZ = spot[2];
            hasSpot = true;
            travelTicks = 0;
            return true;
        }
        return false;
    }

    private void scan(World world, Colony colony, EntityCitizen citizen, BuildSite site, ItemStack carrying) {
        Blueprint blueprint = site.getBlueprint();
        for (int dy = 0; dy < blueprint.getSizeY() && candidates.isEmpty(); dy++) {
            for (int dz = 0; dz < blueprint.getSizeZ(); dz++) {
                for (int dx = 0; dx < blueprint.getSizeX(); dx++) {
                    int x = site.getX() + dx;
                    int y = site.getY() + dy;
                    int z = site.getZ() + dz;
                    if (!site.needsStack(world, carrying, x, y, z)) {
                        continue;
                    }
                    if (!colony.isBuildSpotFree(citizen.getUniqueID(), x, y, z)) {
                        continue;
                    }
                    candidates.add(new int[] { x, y, z });
                    if (candidates.size() >= MAX_CANDIDATES) {
                        return;
                    }
                }
            }
        }
    }

    private static Set<Integer> neededCells(World world, BuildSite site) {
        Blueprint blueprint = site.getBlueprint();
        Set<Integer> needed = new HashSet<>();
        for (int dy = 0; dy < blueprint.getSizeY(); dy++) {
            for (int dz = 0; dz < blueprint.getSizeZ(); dz++) {
                for (int dx = 0; dx < blueprint.getSizeX(); dx++) {
                    int cell = blueprint.cellAt(dx, dy, dz);
                    if (cell == Blueprint.AIR || needed.contains(cell)) {
                        continue;
                    }
                    int x = site.getX() + dx;
                    int y = site.getY() + dy;
                    int z = site.getZ() + dz;
                    if (!site.isPlaced(world, x, y, z) && site.isFree(world, x, y, z)) {
                        needed.add(cell);
                    }
                }
            }
        }
        return needed;
    }

    private static boolean matchesAny(Blueprint blueprint, Set<Integer> needed, ItemStack stack) {
        if (!WorkBlocks.isScaffold(stack)) {
            return false;
        }
        for (int cell : needed) {
            if (blueprint.matches(cell, stack)) {
                return true;
            }
        }
        return false;
    }

    private static int stackLimit(EntityCitizen citizen) {
        return citizen.getInventory()
            .hasFreeMainSlot() ? 64 : 0;
    }

    private void release(EntityCitizen citizen, Colony colony) {
        if (hasSpot) {
            ColonyManager.get(citizen.worldObj)
                .releaseBuildSpot(colony.getId(), citizen.getUniqueID());
            hasSpot = false;
        }
    }

    private boolean travel(EntityCitizen citizen, int x, int y, int z) {
        if (++travelTicks > TRAVEL_TIMEOUT) {
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 1 && citizen.getNavigator()
            .noPath()) {
            pathTowards(citizen, x + 0.5D, y, z + 0.5D, SPEED);
        }
        return true;
    }

    private static boolean inReach(EntityCitizen citizen, int x, int y, int z) {
        return citizen.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= REACH * REACH;
    }

    private static boolean standsOn(EntityCitizen citizen, int x, int y, int z) {
        if (MathHelper.floor_double(citizen.posX) != x || MathHelper.floor_double(citizen.posZ) != z) {
            return false;
        }
        int feet = MathHelper.floor_double(citizen.boundingBox.minY);
        return y == feet || y == feet + 1;
    }
}
