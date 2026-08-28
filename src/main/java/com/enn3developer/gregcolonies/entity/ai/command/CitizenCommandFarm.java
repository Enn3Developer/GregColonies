package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;
import com.enn3developer.gregcolonies.entity.ai.work.Crops;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.SeedCrafting;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenCommandFarm extends CitizenCommand {

    public static final String ID = "farm";

    public static final int MAX_SIDE = 32;

    public static final int SEED_RESERVE = 32;

    private static final int PHASE_TRAVEL = 0;

    private static final int PHASE_WORK = 1;

    private static final int PHASE_DELIVER = 2;

    private static final int WALK_RUNNING = 0;

    private static final int WALK_ARRIVED = 1;

    private static final int WALK_FAILED = 2;

    private static final int JOB_HARVEST = 0;

    private static final int JOB_PLANT = 1;

    private static final int JOB_FERTILIZE = 2;

    private static final int JOB_TILL = 3;

    private static final int SCAN_MARGIN = 2;

    private static final int SCAN_INTERVAL = 100;

    private static final int CRAFT_INTERVAL = 40;

    private static final int SEED_TARGET = 16;

    private static final float TILL_EXHAUSTION = 0.025F;

    private static final int MAX_JOBS = 1024;

    private static final double WORK_REACH_SQ = 25.0D;

    private static final double DROP_OFF_REACH_SQ = 16.0D;

    private static final double WALK_SPEED = 0.6D;

    private static final float LOOK_SPEED = 30.0F;

    private static final double APPROACH_EPSILON = 0.5D;

    private static final int APPROACH_TIMEOUT = 200;

    private static final int ERRAND_TIMEOUT = 6000;

    private static final int ERRAND_RETRY = 600;

    private static final int LEG_RETRY = 200;

    private int minX;

    private int minY;

    private int minZ;

    private int maxX;

    private int maxY;

    private int maxZ;

    private final BlockBreaker breaker = new BlockBreaker();

    private final List<int[]> harvests = new ArrayList<>();

    private final List<int[]> chores = new ArrayList<>();

    private final Set<Long> skipped = new HashSet<>();

    private final Map<Long, Block> crops = new HashMap<>();

    private int phase = PHASE_TRAVEL;

    private CitizenCommandMoveTo leg;

    private String reason = "";

    private int harvested;

    private int planted;

    private boolean hasJob;

    private int jobX;

    private int jobY;

    private int jobZ;

    private int jobType;

    private int scanTicks;

    private int craftTicks;

    private int approachTicks;

    private double bestApproachSq = Double.MAX_VALUE;

    private boolean needsSeed;

    private Block missingSeed;

    private int errandTicks;

    private long nextDeliver;

    public CitizenCommandFarm() {}

    public CitizenCommandFarm(int x1, int y1, int z1, int x2, int y2, int z2) {
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);
        maxX = Math.min(maxX, minX + MAX_SIDE - 1);
        maxZ = Math.min(maxZ, minZ + MAX_SIDE - 1);
        maxY = Math.min(maxY, minY + CitizenCommandHarvest.MAX_HEIGHT - 1);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean canBeTakenBy(EntityCitizen citizen) {
        return citizen.getColony() != null;
    }

    @Override
    public void start(EntityCitizen citizen) {
        leg = null;
        hasJob = false;
        scanTicks = 0;
        craftTicks = 0;
        approachTicks = 0;
        errandTicks = 0;
        needsSeed = false;
        missingSeed = null;
        harvests.clear();
        chores.clear();
        skipped.clear();
        breaker.clear(citizen);
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony == null) {
            reason = "no colony";
            return CitizenCommandResult.FAILED;
        }
        if (scanTicks > 0) {
            scanTicks--;
        }
        if (craftTicks > 0) {
            craftTicks--;
        }
        if (phase == PHASE_TRAVEL) {
            return travel(citizen);
        }
        if (phase == PHASE_WORK) {
            return work(citizen, colony);
        }
        return deliver(citizen, colony);
    }

    private CitizenCommandResult travel(EntityCitizen citizen) {
        if (leg == null) {
            leg = new CitizenCommandMoveTo(centerX(), minY + 1, centerZ());
            leg.start(citizen);
        }
        CitizenCommandResult result = leg.update(citizen);
        if (result == CitizenCommandResult.RUNNING) {
            return CitizenCommandResult.RUNNING;
        }
        leg.finish(citizen);
        leg = null;
        reason = result == CitizenCommandResult.FAILED ? "no path to the field" : "";
        phase = PHASE_WORK;
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult work(EntityCitizen citizen, Colony colony) {
        if (!citizen.worldObj.blockExists(centerX(), minY, centerZ())) {
            reason = "out of range";
            phase = PHASE_TRAVEL;
            return CitizenCommandResult.RUNNING;
        }
        makeSeeds(citizen);
        if (!hasJob && !acquire(citizen)) {
            return rest(citizen, colony);
        }

        double distanceSq = citizen.getDistanceSq(jobX + 0.5D, jobY, jobZ + 0.5D);
        if (distanceSq > WORK_REACH_SQ) {
            return approach(citizen, distanceSq);
        }
        approachTicks = 0;
        citizen.getNavigator()
            .clearPathEntity();
        citizen.getLookHelper()
            .setLookPosition(jobX + 0.5D, jobY + 0.5D, jobZ + 0.5D, LOOK_SPEED, LOOK_SPEED);
        if (jobType == JOB_PLANT) {
            return sow(citizen);
        }
        if (jobType == JOB_FERTILIZE) {
            return feed(citizen);
        }
        if (jobType == JOB_TILL) {
            return till(citizen);
        }
        return reap(citizen, colony);
    }

    private CitizenCommandResult rest(EntityCitizen citizen, Colony colony) {
        breaker.clear(citizen);
        citizen.getNavigator()
            .clearPathEntity();
        if (canDeliver(citizen, colony) && citizen.getInventory()
            .hasExcess(CitizenCommandFarm::keep, SEED_RESERVE)) {
            return startDeliver(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private boolean acquire(EntityCitizen citizen) {
        World world = citizen.worldObj;
        while (true) {
            if (harvests.isEmpty() && chores.isEmpty()) {
                if (scanTicks > 0) {
                    return false;
                }
                scan(citizen);
                scanTicks = SCAN_INTERVAL;
                if (harvests.isEmpty() && chores.isEmpty()) {
                    reason = "nothing to tend";
                    return false;
                }
            }
            List<int[]> source = harvests.isEmpty() ? chores : harvests;
            int[] job = WorkBlocks.takeNearest(citizen.posX, citizen.posY, citizen.posZ, source);
            if (job == null) {
                return false;
            }
            if (skipped.contains(WorkBlocks.pack(job[0], job[1], job[2])) || !isValid(citizen, world, job)) {
                continue;
            }
            jobX = job[0];
            jobY = job[1];
            jobZ = job[2];
            jobType = job[3];
            hasJob = true;
            approachTicks = 0;
            bestApproachSq = Double.MAX_VALUE;
            reason = "";
            return true;
        }
    }

    private boolean isValid(EntityCitizen citizen, World world, int[] job) {
        int x = job[0];
        int y = job[1];
        int z = job[2];
        if (job[3] == JOB_PLANT) {
            return world.isAirBlock(x, y, z) && Crops.isSoil(world, x, y - 1, z)
                && hasSeedFor(citizen, x, y, z, crops.get(WorkBlocks.pack(x, y, z)));
        }
        if (job[3] == JOB_TILL) {
            return Crops.isTillable(world, x, y, z) && Crops.isHoe(
                citizen.getInventory()
                    .getHeldTool());
        }
        if (job[3] == JOB_FERTILIZE) {
            return Crops.canGrow(world, x, y, z) && citizen.getInventory()
                .hasMain(Crops::isBonemeal);
        }
        return Crops.isMature(world, x, y, z) || Crops.isProduce(world, x, y, z);
    }

    private void scan(EntityCitizen citizen) {
        World world = citizen.worldObj;
        harvests.clear();
        chores.clear();
        skipped.clear();
        needsSeed = false;
        boolean bonemeal = citizen.getInventory()
            .hasMain(Crops::isBonemeal);
        boolean hoe = Crops.isHoe(
            citizen.getInventory()
                .getHeldTool());
        int low = Math.max(1, minY - SCAN_MARGIN);
        int high = Math.min(world.getHeight() - 2, maxY + SCAN_MARGIN);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!world.blockExists(x, low, z)) {
                    continue;
                }
                for (int y = low; y <= high; y++) {
                    if (harvests.size() + chores.size() >= MAX_JOBS) {
                        return;
                    }
                    if (Crops.isMature(world, x, y, z) || Crops.isProduce(world, x, y, z)) {
                        harvests.add(new int[] { x, y, z, JOB_HARVEST });
                    } else if (world.isAirBlock(x, y, z) && Crops.isSoil(world, x, y - 1, z)) {
                        if (hasSeedFor(citizen, x, y, z, crops.get(WorkBlocks.pack(x, y, z)))) {
                            chores.add(new int[] { x, y, z, JOB_PLANT });
                        } else {
                            needsSeed = true;
                            missingSeed = crops.get(WorkBlocks.pack(x, y, z));
                        }
                    } else if (bonemeal && Crops.canGrow(world, x, y, z)) {
                        chores.add(new int[] { x, y, z, JOB_FERTILIZE });
                    } else if (hoe && Crops.isTillable(world, x, y, z)) {
                        chores.add(new int[] { x, y, z, JOB_TILL });
                    }
                }
            }
        }
    }

    private CitizenCommandResult approach(EntityCitizen citizen, double distanceSq) {
        breaker.clear(citizen);
        if (distanceSq < bestApproachSq - APPROACH_EPSILON) {
            bestApproachSq = distanceSq;
            approachTicks = 0;
        } else if (++approachTicks > APPROACH_TIMEOUT) {
            return skip(citizen);
        }
        if (citizen.getNavigator()
            .noPath()) {
            citizen.travelTo(jobX + 0.5D, jobY, jobZ + 0.5D, WALK_SPEED);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult reap(EntityCitizen citizen, Colony colony) {
        if (!breaker.isAt(jobX, jobY, jobZ)) {
            breaker.setTarget(citizen, jobX, jobY, jobZ);
        }
        Block crop = Crops.isCrop(citizen.worldObj, jobX, jobY, jobZ) ? citizen.worldObj.getBlock(jobX, jobY, jobZ)
            : null;
        DigResult result = breaker.tick(citizen, true);
        switch (result) {
            case PROGRESS:
                return CitizenCommandResult.RUNNING;
            case BROKEN:
            case TOOL_BROKEN:
                harvested++;
                remember(citizen, crop);
                hasJob = false;
                return CitizenCommandResult.RUNNING;
            case GONE:
                hasJob = false;
                return CitizenCommandResult.RUNNING;
            case INVENTORY_FULL:
                reason = "inventory full";
                if (canDeliver(citizen, colony)) {
                    return startDeliver(citizen);
                }
                return skip(citizen);
            default:
                return skip(citizen);
        }
    }

    private CitizenCommandResult sow(EntityCitizen citizen) {
        Block wanted = crops.get(WorkBlocks.pack(jobX, jobY, jobZ));
        World world = citizen.worldObj;
        ItemStack seed = citizen.getInventory()
            .takeMain(stack -> Crops.isSeedFor(stack, world, jobX, jobY, jobZ, wanted));
        if (seed == null) {
            needsSeed = true;
            missingSeed = wanted;
            reason = "no seeds";
            hasJob = false;
            return CitizenCommandResult.RUNNING;
        }
        if (Crops.plant(citizen.worldObj, jobX, jobY, jobZ, seed)) {
            planted++;
            citizen.swingItem();
        }
        if (seed.stackSize > 0) {
            store(citizen, seed);
        }
        hasJob = false;
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult till(EntityCitizen citizen) {
        ItemStack tool = citizen.getInventory()
            .getHeldTool();
        if (!Crops.isHoe(tool)) {
            reason = "no hoe";
            hasJob = false;
            return CitizenCommandResult.RUNNING;
        }
        if (Crops.till(citizen.worldObj, jobX, jobY, jobZ)) {
            citizen.swingItem();
            citizen.getDiet()
                .addExhaustion(TILL_EXHAUSTION);
            tool.damageItem(1, citizen);
            if (tool.stackSize <= 0) {
                citizen.getInventory()
                    .getTool()
                    .setStackInSlot(0, null);
            }
            Block wanted = crops.get(WorkBlocks.pack(jobX, jobY + 1, jobZ));
            if (hasSeedFor(citizen, jobX, jobY + 1, jobZ, wanted)) {
                chores.add(new int[] { jobX, jobY + 1, jobZ, JOB_PLANT });
            } else {
                needsSeed = true;
                missingSeed = wanted;
            }
        }
        hasJob = false;
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult feed(EntityCitizen citizen) {
        ItemStack bonemeal = citizen.getInventory()
            .takeMain(Crops::isBonemeal);
        if (bonemeal == null) {
            hasJob = false;
            return CitizenCommandResult.RUNNING;
        }
        if (Crops.fertilize(citizen.worldObj, citizen.getRNG(), jobX, jobY, jobZ)) {
            citizen.swingItem();
            bonemeal.stackSize--;
        }
        if (bonemeal.stackSize > 0) {
            store(citizen, bonemeal);
        }
        hasJob = false;
        return CitizenCommandResult.RUNNING;
    }

    private static boolean keep(ItemStack stack) {
        return Crops.isSeed(stack) || SeedCrafting.isSeedSource(stack);
    }

    private static void store(EntityCitizen citizen, ItemStack stack) {
        ItemStack rest = citizen.getInventory()
            .store(stack);
        if (rest != null) {
            citizen.entityDropItem(rest, 0.0F);
        }
    }

    private CitizenCommandResult skip(EntityCitizen citizen) {
        breaker.clear(citizen);
        skipped.add(WorkBlocks.pack(jobX, jobY, jobZ));
        hasJob = false;
        approachTicks = 0;
        return CitizenCommandResult.RUNNING;
    }

    private boolean canDeliver(EntityCitizen citizen, Colony colony) {
        return colony.site(ColonySiteKind.DROP_OFF)
            .isPresent() && colony.getDimension() == citizen.worldObj.provider.dimensionId
            && citizen.worldObj.getTotalWorldTime() >= nextDeliver;
    }

    private CitizenCommandResult startDeliver(EntityCitizen citizen) {
        breaker.clear(citizen);
        hasJob = false;
        errandTicks = 0;
        phase = PHASE_DELIVER;
        if (leg != null) {
            leg.finish(citizen);
            leg = null;
        }
        return CitizenCommandResult.RUNNING;
    }

    private int walkTo(EntityCitizen citizen, int x, int y, int z, double reachSq) {
        if (citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) <= reachSq) {
            return WALK_ARRIVED;
        }
        if (++errandTicks > ERRAND_TIMEOUT) {
            return WALK_FAILED;
        }
        if (leg == null && errandTicks % LEG_RETRY == 1) {
            leg = new CitizenCommandMoveTo(x, y, z);
            leg.start(citizen);
        }
        if (leg != null) {
            CitizenCommandResult result = leg.update(citizen);
            if (result != CitizenCommandResult.RUNNING) {
                leg.finish(citizen);
                leg = null;
                if (result == CitizenCommandResult.DONE) {
                    return WALK_ARRIVED;
                }
            }
        }
        return WALK_RUNNING;
    }

    private CitizenCommandResult deliver(EntityCitizen citizen, Colony colony) {
        ColonySite site = colony.site(ColonySiteKind.DROP_OFF);
        if (!site.isPresent() || colony.getDimension() != citizen.worldObj.provider.dimensionId) {
            reason = "no drop-off";
            return backToWork(citizen);
        }
        int x = site.getX();
        int y = site.getY();
        int z = site.getZ();
        int walk = walkTo(citizen, x, y, z, DROP_OFF_REACH_SQ);
        if (walk == WALK_ARRIVED) {
            return unload(citizen, x, y, z);
        }
        if (walk == WALK_FAILED) {
            reason = "drop-off unreachable";
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
            return backToWork(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult unload(EntityCitizen citizen, int x, int y, int z) {
        faceChest(citizen, x, y, z);
        IInventory target = Inventories.at(citizen.worldObj, x, y, z);
        if (target == null) {
            reason = "drop-off gone";
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
            return backToWork(citizen);
        }

        int moved = citizen.getInventory()
            .deposit(target, CitizenCommandFarm::keep, SEED_RESERVE);
        if (moved > 0) {
            citizen.swingItem();
            reason = "";
        } else {
            reason = "drop-off full";
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
        }
        return backToWork(citizen);
    }

    private void remember(EntityCitizen citizen, Block crop) {
        if (crop == null) {
            return;
        }
        crops.put(WorkBlocks.pack(jobX, jobY, jobZ), crop);
        if (!hasSeedFor(citizen, jobX, jobY, jobZ, crop)) {
            needsSeed = true;
            missingSeed = crop;
        }
    }

    private static boolean hasSeedFor(EntityCitizen citizen, int x, int y, int z, Block wanted) {
        World world = citizen.worldObj;
        return citizen.getInventory()
            .hasMain(stack -> Crops.isSeedFor(stack, world, x, y, z, wanted));
    }

    private void makeSeeds(EntityCitizen citizen) {
        if (!needsSeed || craftTicks > 0) {
            return;
        }
        craftTicks = CRAFT_INTERVAL;
        if (SeedCrafting.craft(citizen, missingSeed, centerX(), minY + 1, centerZ(), SEED_TARGET) > 0) {
            citizen.swingItem();
            needsSeed = false;
            scanTicks = 0;
            reason = "";
        } else {
            reason = "no seeds";
        }
    }

    private static void faceChest(EntityCitizen citizen, int x, int y, int z) {
        citizen.getLookHelper()
            .setLookPosition(x + 0.5D, y + 0.5D, z + 0.5D, LOOK_SPEED, LOOK_SPEED);
        citizen.getNavigator()
            .clearPathEntity();
    }

    private CitizenCommandResult backToWork(EntityCitizen citizen) {
        if (leg != null) {
            leg.finish(citizen);
            leg = null;
        }
        errandTicks = 0;
        scanTicks = 0;
        phase = PHASE_TRAVEL;
        return CitizenCommandResult.RUNNING;
    }

    private int centerX() {
        return (minX + maxX) / 2;
    }

    private int centerZ() {
        return (minZ + maxZ) / 2;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        breaker.clear(citizen);
        if (leg != null) {
            leg.finish(citizen);
            leg = null;
        }
        citizen.getNavigator()
            .clearPathEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        minX = tag.getInteger("x1");
        minY = tag.getInteger("y1");
        minZ = tag.getInteger("z1");
        maxX = tag.getInteger("x2");
        maxY = tag.getInteger("y2");
        maxZ = tag.getInteger("z2");
        phase = tag.getInteger("phase");
        harvested = tag.getInteger("harvested");
        planted = tag.getInteger("planted");
        reason = tag.getString("reason");
        crops.clear();
        NBTTagList list = tag.getTagList("crops", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            Block crop = Block.getBlockFromName(entry.getString("block"));
            if (crop != null) {
                crops.put(entry.getLong("pos"), crop);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("x1", minX);
        tag.setInteger("y1", minY);
        tag.setInteger("z1", minZ);
        tag.setInteger("x2", maxX);
        tag.setInteger("y2", maxY);
        tag.setInteger("z2", maxZ);
        tag.setInteger("phase", phase);
        tag.setInteger("harvested", harvested);
        tag.setInteger("planted", planted);
        tag.setString("reason", reason);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Long, Block> entry : crops.entrySet()) {
            NBTTagCompound crop = new NBTTagCompound();
            crop.setLong("pos", entry.getKey());
            crop.setString("block", Block.blockRegistry.getNameForObject(entry.getValue()));
            list.appendTag(crop);
        }
        tag.setTag("crops", list);
    }

    @Override
    public String describe() {
        String state = phase == PHASE_TRAVEL ? "walking there" : phase == PHASE_WORK ? "tending" : "delivering";
        String tail = reason.isEmpty() ? "" : " (" + reason + ")";
        return ID + " " + state + " " + harvested + " crops " + planted + " seeded" + tail;
    }
}
