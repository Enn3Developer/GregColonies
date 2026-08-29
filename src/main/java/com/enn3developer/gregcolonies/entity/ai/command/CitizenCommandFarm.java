package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.BlockKey;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.TravelLeg;
import com.enn3developer.gregcolonies.entity.ai.WorkPhase;
import com.enn3developer.gregcolonies.entity.ai.work.Crops;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.SeedCrafting;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenCommandFarm extends AreaWorkCommand {

    public static final String ID = "farm";

    public static final int SEED_RESERVE = 32;

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

    private static final int APPROACH_TIMEOUT = 200;

    private static final int ERRAND_TIMEOUT = 6000;

    private static final int ERRAND_RETRY = 600;

    private final List<int[]> harvests = new ArrayList<>();

    private final List<int[]> chores = new ArrayList<>();

    private final Map<Long, Block> crops = new HashMap<>();

    private int planted;

    private boolean hasJob;

    private int jobX;

    private int jobY;

    private int jobZ;

    private int jobType;

    private int scanTicks;

    private int craftTicks;

    private boolean needsSeed;

    private Block missingSeed;

    private int errandTicks;

    private long nextDeliver;

    public CitizenCommandFarm() {
        super(APPROACH_TIMEOUT);
    }

    public CitizenCommandFarm(int x1, int y1, int z1, int x2, int y2, int z2) {
        super(APPROACH_TIMEOUT);
        area.set(x1, y1, z1, x2, y2, z2);
        area.capSide(WorkArea.MAX_SIDE);
        area.capHeight(WorkArea.MAX_HEIGHT);
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
        leg.clear(citizen);
        hasJob = false;
        scanTicks = 0;
        craftTicks = 0;
        approach.reset();
        errandTicks = 0;
        needsSeed = false;
        missingSeed = null;
        harvests.clear();
        chores.clear();
        clearSkipped();
        breaker.clear(citizen);
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        if (citizen.getColony() == null) {
            setReason("no colony");
            return CitizenCommandResult.FAILED;
        }
        if (scanTicks > 0) {
            scanTicks--;
        }
        if (craftTicks > 0) {
            craftTicks--;
        }
        return super.update(citizen);
    }

    @Override
    protected CitizenCommandResult travel(EntityCitizen citizen) {
        TravelLeg.Step step = leg.walk(citizen, centerX(), area.getMinY() + 1, centerZ());
        if (step == TravelLeg.Step.RUNNING) {
            return CitizenCommandResult.RUNNING;
        }
        setReason(step == TravelLeg.Step.FAILED ? "no path to the field" : "");
        setPhase(WorkPhase.WORK);
        return CitizenCommandResult.RUNNING;
    }

    @Override
    protected CitizenCommandResult work(EntityCitizen citizen) {
        return work(citizen, citizen.getColony());
    }

    @Override
    protected CitizenCommandResult deliver(EntityCitizen citizen) {
        return deliver(citizen, citizen.getColony());
    }

    private CitizenCommandResult work(EntityCitizen citizen, Colony colony) {
        if (!citizen.worldObj.blockExists(centerX(), area.getMinY(), centerZ())) {
            setReason("out of range");
            setPhase(WorkPhase.TRAVEL);
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
        approach.restart();
        citizen.getNavigator()
            .clearPathEntity();
        faceBlock(citizen, jobX, jobY, jobZ);
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
                    setReason("nothing to tend");
                    return false;
                }
            }
            List<int[]> source = harvests.isEmpty() ? chores : harvests;
            int[] job = WorkBlocks.takeNearest(citizen.posX, citizen.posY, citizen.posZ, source);
            if (job == null) {
                return false;
            }
            if (isSkipped(job[0], job[1], job[2]) || !isValid(citizen, world, job)) {
                continue;
            }
            jobX = job[0];
            jobY = job[1];
            jobZ = job[2];
            jobType = job[3];
            hasJob = true;
            approach.reset();
            setReason("");
            return true;
        }
    }

    private boolean isValid(EntityCitizen citizen, World world, int[] job) {
        int x = job[0];
        int y = job[1];
        int z = job[2];
        if (job[3] == JOB_PLANT) {
            return world.isAirBlock(x, y, z) && Crops.isSoil(world, x, y - 1, z)
                && hasSeedFor(citizen, x, y, z, crops.get(BlockKey.pack(x, y, z)));
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
        clearSkipped();
        needsSeed = false;
        boolean bonemeal = citizen.getInventory()
            .hasMain(Crops::isBonemeal);
        boolean hoe = Crops.isHoe(
            citizen.getInventory()
                .getHeldTool());
        int low = Math.max(1, area.getMinY() - SCAN_MARGIN);
        int high = Math.min(world.getHeight() - 2, area.getMaxY() + SCAN_MARGIN);

        for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
            for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
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
                        if (hasSeedFor(citizen, x, y, z, crops.get(BlockKey.pack(x, y, z)))) {
                            chores.add(new int[] { x, y, z, JOB_PLANT });
                        } else {
                            needsSeed = true;
                            missingSeed = crops.get(BlockKey.pack(x, y, z));
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
        if (approach.stalled(distanceSq)) {
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
                countHarvest();
                remember(citizen, crop);
                hasJob = false;
                return CitizenCommandResult.RUNNING;
            case GONE:
                hasJob = false;
                return CitizenCommandResult.RUNNING;
            case INVENTORY_FULL:
                setReason("inventory full");
                if (canDeliver(citizen, colony)) {
                    return startDeliver(citizen);
                }
                return skip(citizen);
            default:
                return skip(citizen);
        }
    }

    private CitizenCommandResult sow(EntityCitizen citizen) {
        Block wanted = crops.get(BlockKey.pack(jobX, jobY, jobZ));
        World world = citizen.worldObj;
        ItemStack seed = citizen.getInventory()
            .takeMain(stack -> Crops.isSeedFor(stack, world, jobX, jobY, jobZ, wanted));
        if (seed == null) {
            needsSeed = true;
            missingSeed = wanted;
            setReason("no seeds");
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
            setReason("no hoe");
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
            Block wanted = crops.get(BlockKey.pack(jobX, jobY + 1, jobZ));
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
        skip(jobX, jobY, jobZ);
        hasJob = false;
        approach.restart();
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
        setPhase(WorkPhase.FINISH);
        leg.clear(citizen);
        return CitizenCommandResult.RUNNING;
    }

    private TravelLeg.Step walkTo(EntityCitizen citizen, int x, int y, int z, double reachSq) {
        if (citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) <= reachSq) {
            return TravelLeg.Step.ARRIVED;
        }
        if (++errandTicks > ERRAND_TIMEOUT) {
            return TravelLeg.Step.FAILED;
        }
        TravelLeg.Step step = leg.walk(citizen, x, y, z, errandTicks % LEG_RETRY == 1);
        return step == TravelLeg.Step.ARRIVED ? TravelLeg.Step.ARRIVED : TravelLeg.Step.RUNNING;
    }

    private CitizenCommandResult deliver(EntityCitizen citizen, Colony colony) {
        ColonySite site = colony.site(ColonySiteKind.DROP_OFF);
        if (!site.isPresent() || colony.getDimension() != citizen.worldObj.provider.dimensionId) {
            setReason("no drop-off");
            return backToWork(citizen);
        }
        int x = site.getX();
        int y = site.getY();
        int z = site.getZ();
        TravelLeg.Step walk = walkTo(citizen, x, y, z, DROP_OFF_REACH_SQ);
        if (walk == TravelLeg.Step.ARRIVED) {
            return unload(citizen, x, y, z);
        }
        if (walk == TravelLeg.Step.FAILED) {
            setReason("drop-off unreachable");
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
            return backToWork(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult unload(EntityCitizen citizen, int x, int y, int z) {
        faceChest(citizen, x, y, z);
        IInventory target = Inventories.at(citizen.worldObj, x, y, z);
        if (target == null) {
            setReason("drop-off gone");
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
            return backToWork(citizen);
        }

        int moved = citizen.getInventory()
            .deposit(target, CitizenCommandFarm::keep, SEED_RESERVE);
        if (moved > 0) {
            citizen.swingItem();
            setReason("");
        } else {
            setReason("drop-off full");
            nextDeliver = citizen.worldObj.getTotalWorldTime() + ERRAND_RETRY;
        }
        return backToWork(citizen);
    }

    private void remember(EntityCitizen citizen, Block crop) {
        if (crop == null) {
            return;
        }
        crops.put(BlockKey.pack(jobX, jobY, jobZ), crop);
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
        if (SeedCrafting.craft(citizen, missingSeed, centerX(), area.getMinY() + 1, centerZ(), SEED_TARGET) > 0) {
            citizen.swingItem();
            needsSeed = false;
            scanTicks = 0;
            setReason("");
        } else {
            setReason("no seeds");
        }
    }

    private void faceChest(EntityCitizen citizen, int x, int y, int z) {
        faceBlock(citizen, x, y, z);
        citizen.getNavigator()
            .clearPathEntity();
    }

    private CitizenCommandResult backToWork(EntityCitizen citizen) {
        leg.clear(citizen);
        errandTicks = 0;
        scanTicks = 0;
        setPhase(WorkPhase.TRAVEL);
        return CitizenCommandResult.RUNNING;
    }

    private int centerX() {
        return area.getCenterX();
    }

    private int centerZ() {
        return area.getCenterZ();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        planted = tag.getInteger("planted");
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
        super.writeToNBT(tag);
        tag.setInteger("planted", planted);
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
    protected String describeWork() {
        return "tending";
    }

    @Override
    protected String describeReturn() {
        return "delivering";
    }

    @Override
    protected String describeTally() {
        return getHarvested() + " crops " + planted + " seeded";
    }
}
