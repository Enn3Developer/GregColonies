package com.enn3developer.gregcolonies.entity.ai.idle;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildPlan;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;
import com.enn3developer.gregcolonies.entity.ai.work.WorldOps;

public class IdleTaskBuild extends AutoTask {

    public static final String ID = "build";

    private static final double SPEED = 0.6D;

    private static final int RETRY_INTERVAL = 200;

    private static final int TRAVEL_TIMEOUT = 400;

    private static final int STAND_TIMEOUT = 200;

    private static final double REACH = 4.0D;

    private static final int PLACE_INTERVAL = 8;

    private static final int STAND_RANGE = 3;

    private static final int STAND_LEVELS = 2;

    private static final int COLUMN_RANGE = 2;

    private static final int SCAFFOLD_FETCH = 16;

    private final BlockBreaker breaker = new BlockBreaker();

    private final BuildPlan plan = new BuildPlan();

    private final SiteProbe probe = new SiteProbe();

    private String activity = "building";

    private boolean storing;

    private boolean hasStand;

    private boolean standDenied;

    private int standX;

    private int standY;

    private int standZ;

    private boolean hasColumn;

    private int columnX;

    private int columnZ;

    private int columnTop;

    private int placeCooldown;

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
        if (!citizen.canWork() || !ready(world)) {
            return false;
        }
        if (world.provider.dimensionId != colony.getDimension() || citizen.getJob() != CitizenJob.BUILDER) {
            return false;
        }
        return colony.getBuildSite() != null && colony.site(ColonySiteKind.MATERIALS)
            .isPresent();
    }

    @Override
    public void start(EntityCitizen citizen, Colony colony) {
        storing = false;
        resetTravel();
        placeCooldown = 0;
        breaker.clear(citizen);
        resetApproach();
        plan.start(colony.getBuildSite());
    }

    @Override
    public boolean update(EntityCitizen citizen, Colony colony) {
        World world = citizen.worldObj;
        BuildSite site = colony.getBuildSite();
        if (site == null) {
            return false;
        }
        if (!ColonyManager.registry(world)
            .claimBuildSite(colony.getId(), citizen.getUniqueID(), world.getTotalWorldTime())) {
            return false;
        }
        probe.bind(world, site);
        if (storing) {
            return store(citizen, colony);
        }
        if (plan.getPhase() == BuildPlan.Phase.CLEAR) {
            return clear(citizen, colony, site);
        }
        if (plan.getPhase() == BuildPlan.Phase.BUILD) {
            return build(citizen, colony, site);
        }
        return strip(citizen, colony, site);
    }

    @Override
    public void finish(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony != null) {
            ColonyManager.registry(citizen.worldObj)
                .releaseBuildSite(colony.getId(), citizen.getUniqueID());
        }
        breaker.clear(citizen);
        delay(citizen.worldObj, RETRY_INTERVAL);
        citizen.getNavigator()
            .clearPathEntity();
    }

    private void resetApproach() {
        hasStand = false;
        standDenied = false;
        hasColumn = false;
        resetTravel();
    }

    private boolean acquire(Colony colony, BuildSite site) {
        if (plan.hasTarget()) {
            return true;
        }
        if (!plan.nextTarget(probe, colony, site)) {
            return false;
        }
        resetApproach();
        return true;
    }

    private boolean clear(EntityCitizen citizen, Colony colony, BuildSite site) {
        activity = "clearing the site";
        if (!acquire(colony, site)) {
            return true;
        }
        int targetX = plan.getTargetX();
        int targetY = plan.getTargetY();
        int targetZ = plan.getTargetZ();
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
            plan.markProgress();
            plan.clearTarget();
            resetTravel();
            return true;
        }
        skipTarget();
        return true;
    }

    private boolean build(EntityCitizen citizen, Colony colony, BuildSite site) {
        Blueprint blueprint = site.getBlueprint();
        if (!acquire(colony, site)) {
            return true;
        }

        int cell = site.cellFor(plan.getTargetX(), plan.getTargetY(), plan.getTargetZ());
        if (!citizen.getInventory()
            .hasMain(
                stack -> blueprint.getPalette()
                    .matches(cell, stack))) {
            return fetch(citizen, colony, blueprint, cell);
        }
        if (!inReach(citizen, plan.getTargetX(), plan.getTargetY(), plan.getTargetZ())
            || occupies(citizen, plan.getTargetX(), plan.getTargetY(), plan.getTargetZ())) {
            return approach(citizen, colony, site);
        }
        return placeBlock(citizen, site, cell);
    }

    private boolean fetch(EntityCitizen citizen, Colony colony, Blueprint blueprint, int cell) {
        activity = "fetching materials";
        ColonySite chest = colony.site(ColonySiteKind.MATERIALS);
        if (!chest.isPresent()) {
            plan.materialMissing(cell);
            skipCell();
            return true;
        }
        if (!citizen.getInventory()
            .hasFreeMainSlot()) {
            storing = true;
            return true;
        }
        int x = chest.getX();
        int y = chest.getY();
        int z = chest.getZ();
        if (!inReach(citizen, x, y, z)) {
            return travel(citizen, x, y + 1, z);
        }

        IInventory inventory = Inventories.at(citizen.worldObj, x, y, z);
        if (inventory == null) {
            return false;
        }
        ItemStack taken = Inventories.extract(
            inventory,
            stack -> blueprint.getPalette()
                .matches(cell, stack),
            64);
        resetTravel();
        if (taken == null) {
            plan.materialMissing(cell);
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
            resetTravel();
        }
        return true;
    }

    private boolean findStand(World world, EntityCitizen citizen, BuildSite site) {
        int targetX = plan.getTargetX();
        int targetY = plan.getTargetY();
        int targetZ = plan.getTargetZ();
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
        if (x == plan.getTargetX() && z == plan.getTargetZ()
            && (y == plan.getTargetY() || y + 1 == plan.getTargetY())) {
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
        if (!WorldOps.stepUp(citizen, x, feet, z, stack)) {
            skipTarget();
            return true;
        }
        site.addScaffold(x, feet, z);
        ColonyManager.registry(world)
            .markDirty();
        placeCooldown = PLACE_INTERVAL;
        return true;
    }

    private boolean findColumn(World world, EntityCitizen citizen, BuildSite site) {
        int targetX = plan.getTargetX();
        int targetY = plan.getTargetY();
        int targetZ = plan.getTargetZ();
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
        int targetY = plan.getTargetY();
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
        ColonySite chest = colony.site(ColonySiteKind.MATERIALS);
        if (!chest.isPresent()) {
            skipTarget();
            return true;
        }
        if (!citizen.getInventory()
            .hasFreeMainSlot()) {
            storing = true;
            return true;
        }
        int x = chest.getX();
        int y = chest.getY();
        int z = chest.getZ();
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
        resetTravel();
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
            if (blueprint.getPalette()
                .matches(cell, stack)) {
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
        Block block = blueprint.getPalette()
            .blockOf(cell);
        ItemStack used = citizen.getInventory()
            .takeMain(
                stack -> blueprint.getPalette()
                    .matches(cell, stack));
        if (block == null || used == null) {
            skipTarget();
            return true;
        }
        int targetX = plan.getTargetX();
        int targetY = plan.getTargetY();
        int targetZ = plan.getTargetZ();
        citizen.getNavigator()
            .clearPathEntity();
        WorldOps.place(world, targetX, targetY, targetZ, block, Blueprint.metaOf(cell));
        citizen.swingItem();
        plan.markProgress();
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
                resetTravel();
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
                    WorldOps.dropAt(world, scaffold[0], scaffold[1], scaffold[2], rest);
                }
            }
            world.setBlockToAir(scaffold[0], scaffold[1], scaffold[2]);
            WorldOps.playPlace(world, block, scaffold[0], scaffold[1], scaffold[2]);
            citizen.swingItem();
        }
        site.removeScaffold(scaffold[0], scaffold[1], scaffold[2]);
        ColonyManager.registry(world)
            .markDirty();
        placeCooldown = PLACE_INTERVAL;
        resetTravel();
        return true;
    }

    private boolean store(EntityCitizen citizen, Colony colony) {
        activity = "storing materials";
        ColonySite chest = colony.site(ColonySiteKind.MATERIALS);
        if (!chest.isPresent()) {
            storing = false;
            return false;
        }
        int x = chest.getX();
        int y = chest.getY();
        int z = chest.getZ();
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
        resetTravel();
        return moved > 0;
    }

    private void skipCell() {
        plan.skipCell();
        resetApproach();
    }

    private void skipTarget() {
        plan.blockTarget();
        resetApproach();
    }

    private boolean travel(EntityCitizen citizen, int x, int y, int z) {
        return travel(citizen, x, y, z, TRAVEL_TIMEOUT);
    }

    private boolean travel(EntityCitizen citizen, int x, int y, int z, int limit) {
        return travel(citizen, x + 0.5D, y, z + 0.5D, SPEED, limit);
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

    private static final class SiteProbe implements BuildPlan.Probe {

        private World world;

        private BuildSite site;

        private void bind(World world, BuildSite site) {
            this.world = world;
            this.site = site;
        }

        @Override
        public boolean needsClearing(int x, int y, int z) {
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

        @Override
        public boolean canPlace(int x, int y, int z) {
            return !site.isPlaced(world, x, y, z) && site.isFree(world, x, y, z);
        }

        @Override
        public boolean isFullCube(int cell) {
            Block block = site.getBlueprint()
                .getPalette()
                .blockOf(cell);
            return block != null && block.renderAsNormalBlock();
        }
    }
}
