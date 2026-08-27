package com.enn3developer.gregcolonies.entity.ai.idle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class IdleTaskBuild extends AutoTask {

    public static final String ID = "build";

    private static final double SPEED = 0.6D;

    private static final int RETRY_INTERVAL = 200;

    private static final int TRAVEL_TIMEOUT = 400;

    private static final int STAND_TIMEOUT = 200;

    private static final int REPATH_INTERVAL = 10;

    private static final double REACH = 4.0D;

    private static final int PLACE_INTERVAL = 8;

    private static final int STAND_RANGE = 3;

    private static final int STAND_LEVELS = 2;

    private static final int COLUMN_RANGE = 2;

    private static final int SCAFFOLD_FETCH = 16;

    private static final int PHASE_CLEAR = 0;

    private static final int PHASE_BUILD = 1;

    private static final int PHASE_STRIP = 2;

    private final BlockBreaker breaker = new BlockBreaker();

    private final Set<Long> blocked = new HashSet<>();

    private final Set<Integer> missing = new HashSet<>();

    private String activity = "building";

    private int phase;

    private int cursor;

    private boolean progressed;

    private boolean storing;

    private boolean hasTarget;

    private int targetX;

    private int targetY;

    private int targetZ;

    private boolean hasStand;

    private boolean standDenied;

    private int standX;

    private int standY;

    private int standZ;

    private boolean hasColumn;

    private int columnX;

    private int columnZ;

    private int columnTop;

    private int travelTicks;

    private int placeCooldown;

    private long nextAttempt;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String describe() {
        return storing ? "storing materials" : activity;
    }

    @Override
    public boolean shouldStart(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        if (!citizen.canWork() || world.getTotalWorldTime() < nextAttempt) {
            return false;
        }
        if (world.provider.dimensionId != colony.getDimension() || citizen.getJob() != CitizenJob.BUILDER) {
            return false;
        }
        return colony.getBuildSite() != null && colony.hasMaterials();
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        blocked.clear();
        storing = false;
        travelTicks = 0;
        placeCooldown = 0;
        breaker.clear(citizen);
        startPhase(PHASE_CLEAR, colony.getBuildSite());
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        BuildSite site = colony.getBuildSite();
        if (site == null) {
            return false;
        }
        if (!ColonyManager.get(world)
            .claimBuildSite(colony.getId(), citizen.getUniqueID(), world.getTotalWorldTime())) {
            return false;
        }
        if (storing) {
            return store(citizen, colony);
        }
        if (phase == PHASE_CLEAR) {
            return clear(citizen, colony, site);
        }
        if (phase == PHASE_BUILD) {
            return build(citizen, colony, site);
        }
        return strip(citizen, colony, site);
    }

    @Override
    public void finish(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony != null) {
            ColonyManager.get(citizen.worldObj)
                .releaseBuildSite(colony.getId(), citizen.getUniqueID());
        }
        breaker.clear(citizen);
        nextAttempt = citizen.worldObj.getTotalWorldTime() + RETRY_INTERVAL;
        citizen.getNavigator()
            .clearPathEntity();
    }

    private void startPhase(int next, BuildSite site) {
        phase = next;
        cursor = next == PHASE_CLEAR && site != null ? site.getBlueprint()
            .volume() - 1 : 0;
        progressed = false;
        hasTarget = false;
        hasStand = false;
        standDenied = false;
        hasColumn = false;
        travelTicks = 0;
        missing.clear();
    }

    private boolean clear(EntityCitizen citizen, Colony colony, BuildSite site) {
        World world = citizen.worldObj;
        activity = "clearing the site";
        if (!hasTarget && !nextClearTarget(world, colony, site)) {
            startPhase(PHASE_BUILD, site);
            return true;
        }
        if (!inReach(citizen, targetX, targetY, targetZ)) {
            breaker.clear(citizen);
            if (!travel(citizen, targetX, targetY + 1, targetZ)) {
                skipTarget();
            }
            return true;
        }

        citizen.getNavigator()
            .clearPathEntity();
        if (!breaker.isAt(targetX, targetY, targetZ)) {
            breaker.setTarget(citizen, targetX, targetY, targetZ);
        }
        DigResult result = breaker.tick(citizen, true);
        if (result == DigResult.PROGRESS) {
            return true;
        }
        if (result == DigResult.INVENTORY_FULL) {
            storing = true;
            return true;
        }
        if (result == DigResult.BROKEN || result == DigResult.GONE) {
            progressed = true;
            hasTarget = false;
            travelTicks = 0;
            return true;
        }
        skipTarget();
        return true;
    }

    private boolean nextClearTarget(World world, Colony colony, BuildSite site) {
        Blueprint blueprint = site.getBlueprint();
        int sizeX = blueprint.getSizeX();
        int sizeZ = blueprint.getSizeZ();
        while (cursor >= 0) {
            int x = site.getX() + cursor % sizeX;
            int y = site.getY() + cursor / (sizeX * sizeZ);
            int z = site.getZ() + cursor / sizeX % sizeZ;
            if (!blocked.contains(WorkBlocks.pack(x, y, z)) && !isProtected(colony, x, y, z)
                && needsClearing(world, site, x, y, z)) {
                targetX = x;
                targetY = y;
                targetZ = z;
                hasTarget = true;
                travelTicks = 0;
                return true;
            }
            cursor--;
        }
        return false;
    }

    private static boolean needsClearing(World world, BuildSite site, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)
            || block.getMaterial()
                .isLiquid()) {
            return false;
        }
        if (site.isScaffoldAt(x, y, z)) {
            return false;
        }
        return site.cellFor(x, y, z) == Blueprint.AIR || !site.isPlaced(world, x, y, z);
    }

    private static boolean isProtected(Colony colony, int x, int y, int z) {
        if (colony.getX() == x && colony.getY() == y && colony.getZ() == z) {
            return true;
        }
        if (colony.hasMaterials() && colony.getMaterialsX() == x
            && colony.getMaterialsY() == y
            && colony.getMaterialsZ() == z) {
            return true;
        }
        if (colony.hasDropOff() && colony.getDropOffX() == x
            && colony.getDropOffY() == y
            && colony.getDropOffZ() == z) {
            return true;
        }
        return colony.hasPickUp() && colony.getPickUpX() == x && colony.getPickUpY() == y && colony.getPickUpZ() == z;
    }

    private boolean build(EntityCitizen citizen, Colony colony, BuildSite site) {
        World world = citizen.worldObj;
        Blueprint blueprint = site.getBlueprint();
        if (!hasTarget && !nextBuildTarget(world, site)) {
            if (progressed) {
                progressed = false;
                cursor = 0;
                missing.clear();
                blocked.clear();
                return true;
            }
            startPhase(PHASE_STRIP, site);
            return true;
        }

        int cell = site.cellFor(targetX, targetY, targetZ);
        if (!citizen.getInventory()
            .hasMain(stack -> blueprint.matches(cell, stack))) {
            return fetch(citizen, colony, blueprint, cell);
        }
        if (!inReach(citizen, targetX, targetY, targetZ) || occupies(citizen, targetX, targetY, targetZ)) {
            return approach(citizen, colony, site);
        }
        return placeBlock(citizen, site, cell);
    }

    private boolean nextBuildTarget(World world, BuildSite site) {
        Blueprint blueprint = site.getBlueprint();
        int sizeX = blueprint.getSizeX();
        int sizeZ = blueprint.getSizeZ();
        int volume = blueprint.volume();
        while (cursor < volume) {
            int x = site.getX() + cursor % sizeX;
            int y = site.getY() + cursor / (sizeX * sizeZ);
            int z = site.getZ() + cursor / sizeX % sizeZ;
            int cell = site.cellFor(x, y, z);
            if (cell != Blueprint.AIR && !missing.contains(cell)
                && !blocked.contains(WorkBlocks.pack(x, y, z))
                && !site.isPlaced(world, x, y, z)
                && site.isFree(world, x, y, z)) {
                targetX = x;
                targetY = y;
                targetZ = z;
                hasTarget = true;
                hasStand = false;
                standDenied = false;
                hasColumn = false;
                travelTicks = 0;
                return true;
            }
            cursor++;
        }
        return false;
    }

    private boolean fetch(EntityCitizen citizen, Colony colony, Blueprint blueprint, int cell) {
        activity = "fetching materials";
        if (!colony.hasMaterials()) {
            missing.add(cell);
            skipCell();
            return true;
        }
        if (!citizen.getInventory()
            .hasFreeMainSlot()) {
            storing = true;
            return true;
        }
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
        ItemStack taken = Inventories.extract(inventory, stack -> blueprint.matches(cell, stack), 64);
        travelTicks = 0;
        if (taken == null) {
            missing.add(cell);
            skipCell();
            return true;
        }
        ItemStack rest = citizen.getInventory()
            .store(taken);
        if (rest != null) {
            Inventories.insert(inventory, rest);
        }
        citizen.swingItem();
        return true;
    }

    private boolean approach(EntityCitizen citizen, Colony colony, BuildSite site) {
        World world = citizen.worldObj;
        if (!standDenied && !hasStand && !findStand(world, citizen, site)) {
            standDenied = true;
        }
        if (standDenied) {
            return scaffold(citizen, colony, site);
        }
        activity = "building";
        breaker.clear(citizen);
        if (!travel(citizen, standX, standY, standZ, STAND_TIMEOUT)) {
            hasStand = false;
            standDenied = true;
            travelTicks = 0;
        }
        return true;
    }

    private boolean findStand(World world, EntityCitizen citizen, BuildSite site) {
        double best = Double.MAX_VALUE;
        for (int dy = -STAND_LEVELS; dy <= STAND_LEVELS; dy++) {
            for (int dz = -STAND_RANGE; dz <= STAND_RANGE; dz++) {
                for (int dx = -STAND_RANGE; dx <= STAND_RANGE; dx++) {
                    int x = targetX + dx;
                    int y = targetY + dy;
                    int z = targetZ + dz;
                    if (!canStand(world, site, x, y, z) || !reaches(x, y, z, targetX, targetY, targetZ)) {
                        continue;
                    }
                    double distance = citizen.getDistanceSq(x + 0.5D, y, z + 0.5D);
                    if (distance < best) {
                        best = distance;
                        standX = x;
                        standY = y;
                        standZ = z;
                        hasStand = true;
                    }
                }
            }
        }
        return hasStand;
    }

    private boolean canStand(World world, BuildSite site, int x, int y, int z) {
        if (x == targetX && z == targetZ && (y == targetY || y + 1 == targetY)) {
            return false;
        }
        if (site.cellFor(x, y, z) != Blueprint.AIR || site.cellFor(x, y + 1, z) != Blueprint.AIR) {
            return false;
        }
        return !WorkBlocks.blocksMovement(world, x, y, z) && !WorkBlocks.blocksMovement(world, x, y + 1, z)
            && WorkBlocks.blocksMovement(world, x, y - 1, z);
    }

    private static boolean reaches(int x, int y, int z, int targetX, int targetY, int targetZ) {
        double dx = x - targetX;
        double dy = y - targetY;
        double dz = z - targetZ;
        return dx * dx + dy * dy + dz * dz <= REACH * REACH;
    }

    private boolean scaffold(EntityCitizen citizen, Colony colony, BuildSite site) {
        World world = citizen.worldObj;
        activity = "raising scaffolding";
        if (!hasColumn && !findColumn(world, citizen, site)) {
            skipTarget();
            return true;
        }
        int x = MathHelper.floor_double(citizen.posX);
        int z = MathHelper.floor_double(citizen.posZ);
        int feet = MathHelper.floor_double(citizen.boundingBox.minY + 0.1D);
        if (feet >= columnTop) {
            hasColumn = false;
            hasStand = false;
            return true;
        }
        if (x != columnX || z != columnZ) {
            breaker.clear(citizen);
            if (!travel(citizen, columnX, feet, columnZ)) {
                skipTarget();
            }
            return true;
        }
        if (!WorkBlocks.blocksMovement(world, x, feet - 1, z) || WorkBlocks.blocksMovement(world, x, feet + 1, z)
            || WorkBlocks.blocksMovement(world, x, feet + 2, z)) {
            skipTarget();
            return true;
        }
        if (placeCooldown > 0) {
            placeCooldown--;
            return true;
        }

        ItemStack stack = citizen.getInventory()
            .takeScaffold();
        if (stack == null) {
            return fetchScaffold(citizen, colony, site);
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null) {
            skipTarget();
            return true;
        }
        citizen.getNavigator()
            .clearPathEntity();
        world.setBlock(x, feet, z, block, stack.getItemDamage(), 3);
        site.addScaffold(x, feet, z);
        ColonyManager.get(world)
            .markDirty();
        playPlace(world, block, x, feet, z);
        citizen.setPosition(x + 0.5D, feet + 1.0D, z + 0.5D);
        citizen.motionY = 0.0D;
        citizen.fallDistance = 0.0F;
        citizen.swingItem();
        placeCooldown = PLACE_INTERVAL;
        return true;
    }

    private boolean findColumn(World world, EntityCitizen citizen, BuildSite site) {
        double best = Double.MAX_VALUE;
        for (int dz = -COLUMN_RANGE; dz <= COLUMN_RANGE; dz++) {
            for (int dx = -COLUMN_RANGE; dx <= COLUMN_RANGE; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int x = targetX + dx;
                int z = targetZ + dz;
                if (!reaches(x, targetY, z, targetX, targetY, targetZ) || !isFreeColumn(world, site, x, z)) {
                    continue;
                }
                double distance = citizen.getDistanceSq(x + 0.5D, targetY, z + 0.5D);
                if (distance < best) {
                    best = distance;
                    columnX = x;
                    columnZ = z;
                    columnTop = targetY;
                    hasColumn = true;
                }
            }
        }
        return hasColumn;
    }

    private boolean isFreeColumn(World world, BuildSite site, int x, int z) {
        for (int y = site.getY(); y <= targetY + 1; y++) {
            if (site.cellFor(x, y, z) != Blueprint.AIR) {
                return false;
            }
            if (y >= targetY && WorkBlocks.blocksMovement(world, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    private boolean fetchScaffold(EntityCitizen citizen, Colony colony, BuildSite site) {
        activity = "fetching scaffolding";
        if (!colony.hasMaterials()) {
            skipTarget();
            return true;
        }
        if (!citizen.getInventory()
            .hasFreeMainSlot()) {
            storing = true;
            return true;
        }
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
        Blueprint blueprint = site.getBlueprint();
        ItemStack taken = Inventories
            .extract(inventory, stack -> WorkBlocks.isScaffold(stack) && !isMaterial(blueprint, stack), SCAFFOLD_FETCH);
        travelTicks = 0;
        if (taken == null) {
            skipTarget();
            return true;
        }
        ItemStack rest = citizen.getInventory()
            .store(taken);
        if (rest != null) {
            Inventories.insert(inventory, rest);
        }
        citizen.swingItem();
        return true;
    }

    private static boolean isMaterial(Blueprint blueprint, ItemStack stack) {
        for (int cell : blueprint.materials()
            .keySet()) {
            if (blueprint.matches(cell, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean placeBlock(EntityCitizen citizen, BuildSite site, int cell) {
        activity = "building";
        if (placeCooldown > 0) {
            placeCooldown--;
            return true;
        }
        World world = citizen.worldObj;
        Blueprint blueprint = site.getBlueprint();
        Block block = blueprint.blockOf(cell);
        ItemStack used = citizen.getInventory()
            .takeMain(stack -> blueprint.matches(cell, stack));
        if (block == null || used == null) {
            skipTarget();
            return true;
        }
        citizen.getNavigator()
            .clearPathEntity();
        world.setBlock(targetX, targetY, targetZ, block, Blueprint.metaOf(cell), 3);
        playPlace(world, block, targetX, targetY, targetZ);
        citizen.swingItem();
        progressed = true;
        placeCooldown = PLACE_INTERVAL;
        skipCell();
        return true;
    }

    private boolean strip(EntityCitizen citizen, Colony colony, BuildSite site) {
        World world = citizen.worldObj;
        if (!site.hasScaffolds()) {
            return citizen.getInventory()
                .countMain(stack -> true) > 0 && store(citizen, colony);
        }
        activity = "removing scaffolding";
        int x = MathHelper.floor_double(citizen.posX);
        int z = MathHelper.floor_double(citizen.posZ);
        int feet = MathHelper.floor_double(citizen.boundingBox.minY + 0.1D);
        int[] scaffold = site.isScaffoldAt(x, feet - 1, z) ? new int[] { x, feet - 1, z } : site.topScaffold();
        if (scaffold == null) {
            return true;
        }
        if (!inReach(citizen, scaffold[0], scaffold[1], scaffold[2])) {
            if (!travel(citizen, scaffold[0], scaffold[1] + 1, scaffold[2])) {
                site.removeScaffold(scaffold[0], scaffold[1], scaffold[2]);
                travelTicks = 0;
            }
            return true;
        }
        if (!citizen.getInventory()
            .hasFreeMainSlot()) {
            storing = true;
            return true;
        }
        if (placeCooldown > 0) {
            placeCooldown--;
            return true;
        }

        Block block = world.getBlock(scaffold[0], scaffold[1], scaffold[2]);
        if (block != null && !block.isAir(world, scaffold[0], scaffold[1], scaffold[2])) {
            int meta = world.getBlockMetadata(scaffold[0], scaffold[1], scaffold[2]);
            for (ItemStack drop : new ArrayList<ItemStack>(
                block.getDrops(world, scaffold[0], scaffold[1], scaffold[2], meta, 0))) {
                ItemStack rest = citizen.getInventory()
                    .store(drop);
                if (rest != null) {
                    dropAt(world, scaffold[0], scaffold[1], scaffold[2], rest);
                }
            }
            world.setBlockToAir(scaffold[0], scaffold[1], scaffold[2]);
            playPlace(world, block, scaffold[0], scaffold[1], scaffold[2]);
            citizen.swingItem();
        }
        site.removeScaffold(scaffold[0], scaffold[1], scaffold[2]);
        ColonyManager.get(world)
            .markDirty();
        placeCooldown = PLACE_INTERVAL;
        travelTicks = 0;
        return true;
    }

    private boolean store(EntityCitizen citizen, Colony colony) {
        activity = "storing materials";
        if (!colony.hasMaterials()) {
            storing = false;
            return false;
        }
        int x = colony.getMaterialsX();
        int y = colony.getMaterialsY();
        int z = colony.getMaterialsZ();
        if (!inReach(citizen, x, y, z)) {
            return travel(citizen, x, y + 1, z);
        }
        IInventory inventory = Inventories.at(citizen.worldObj, x, y, z);
        if (inventory == null) {
            storing = false;
            return false;
        }
        int moved = citizen.getInventory()
            .deposit(inventory);
        citizen.swingItem();
        storing = false;
        travelTicks = 0;
        return moved > 0;
    }

    private void skipCell() {
        hasTarget = false;
        hasStand = false;
        standDenied = false;
        hasColumn = false;
        travelTicks = 0;
        cursor++;
    }

    private void skipTarget() {
        if (hasTarget) {
            blocked.add(WorkBlocks.pack(targetX, targetY, targetZ));
        }
        hasTarget = false;
        hasStand = false;
        standDenied = false;
        hasColumn = false;
        travelTicks = 0;
        if (phase == PHASE_BUILD) {
            cursor++;
        }
    }

    private boolean travel(EntityCitizen citizen, int x, int y, int z) {
        return travel(citizen, x, y, z, TRAVEL_TIMEOUT);
    }

    private boolean travel(EntityCitizen citizen, int x, int y, int z, int limit) {
        if (++travelTicks > limit) {
            return false;
        }
        if (travelTicks % REPATH_INTERVAL == 1 && citizen.getNavigator()
            .noPath()) {
            pathTowards(citizen, x + 0.5D, y, z + 0.5D, SPEED);
        }
        return true;
    }

    private static void playPlace(World world, Block block, int x, int y, int z) {
        world.playSoundEffect(
            x + 0.5D,
            y + 0.5D,
            z + 0.5D,
            block.stepSound.func_150496_b(),
            (block.stepSound.getVolume() + 1.0F) / 2.0F,
            block.stepSound.getPitch() * 0.8F);
    }

    private static void dropAt(World world, int x, int y, int z, ItemStack stack) {
        EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack);
        item.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(item);
    }

    private static boolean inReach(EntityCitizen citizen, int x, int y, int z) {
        return citizen.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= REACH * REACH;
    }

    private static boolean occupies(EntityCitizen citizen, int x, int y, int z) {
        if (MathHelper.floor_double(citizen.posX) != x || MathHelper.floor_double(citizen.posZ) != z) {
            return false;
        }
        int feet = MathHelper.floor_double(citizen.boundingBox.minY + 0.1D);
        return feet == y || feet + 1 == y;
    }
}
