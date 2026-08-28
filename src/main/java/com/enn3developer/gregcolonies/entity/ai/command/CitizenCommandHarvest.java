package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.BlockKey;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.ApproachTracker;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.TravelLeg;
import com.enn3developer.gregcolonies.entity.ai.WorkPhase;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;
import com.enn3developer.gregcolonies.entity.ai.work.WorldOps;

public abstract class CitizenCommandHarvest extends CitizenCommand {

    private static final double WORK_REACH_SQ = 25.0D;

    private static final double WALK_SPEED = 0.6D;

    private static final float LOOK_SPEED = 30.0F;

    private static final int APPROACH_TIMEOUT = 400;

    private static final int MAX_SKIPS = 24;

    private static final double HOME_REACH_SQ = 9.0D;

    private static final double DROP_OFF_REACH_SQ = 16.0D;

    private static final int HOME_TIMEOUT = 6000;

    private static final int LEG_RETRY = 200;

    private static final int PILLAR_INTERVAL = 10;

    private static final int PILLAR_CLIMB = 2;

    protected final WorkArea area = new WorkArea();

    private final BlockBreaker breaker = new BlockBreaker();

    private final Set<Long> skipped = new HashSet<>();

    private WorkPhase phase = WorkPhase.TRAVEL;

    private final TravelLeg leg = new TravelLeg();

    private String reason = "";

    private int harvested;

    private boolean hasTarget;

    private boolean hadTool;

    private boolean resumeAfterDropOff;

    private int targetX;

    private int targetY;

    private int targetZ;

    private int anchorX;

    private int anchorY;

    private int anchorZ;

    private final ApproachTracker approach = new ApproachTracker(APPROACH_TIMEOUT);

    private int skips;

    private int homeTicks;

    private int travelY;

    private int pillarCooldown;

    protected CitizenCommandHarvest() {}

    protected CitizenCommandHarvest(int x1, int y1, int z1, int x2, int y2, int z2) {
        area.set(x1, y1, z1, x2, y2, z2);
        area.capHeight(WorkArea.MAX_HEIGHT);
    }

    protected abstract Block referenceBlock();

    protected abstract String unitName();

    protected abstract boolean canTunnel();

    protected abstract void resetWork();

    protected abstract boolean acquireTarget(EntityCitizen citizen);

    protected abstract void onHarvested(EntityCitizen citizen, int x, int y, int z);

    protected void onAbandon(EntityCitizen citizen) {}

    protected void setTarget(int x, int y, int z, int ax, int ay, int az) {
        targetX = x;
        targetY = y;
        targetZ = z;
        anchorX = ax;
        anchorY = ay;
        anchorZ = az;
        hasTarget = true;
        approach.reset();
    }

    protected boolean isSkipped(int x, int y, int z) {
        return skipped.contains(BlockKey.pack(x, y, z));
    }

    protected int getHarvested() {
        return harvested;
    }

    @Override
    public void start(EntityCitizen citizen) {
        leg.clear(citizen);
        hasTarget = false;
        approach.reset();
        skips = 0;
        homeTicks = 0;
        pillarCooldown = 0;
        hadTool = false;
        resumeAfterDropOff = false;
        skipped.clear();
        breaker.clear(citizen);
        resetWork();
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        if (phase == WorkPhase.TRAVEL) {
            return travel(citizen);
        }
        if (phase == WorkPhase.WORK) {
            return work(citizen);
        }
        return goHome(citizen);
    }

    private CitizenCommandResult travel(EntityCitizen citizen) {
        int x = area.getCenterX();
        int z = area.getCenterZ();
        if (!leg.isActive()) {
            travelY = surfaceY(citizen.worldObj, x, z);
        }
        if (leg.walk(citizen, x, travelY, z) != TravelLeg.Step.RUNNING) {
            phase = WorkPhase.WORK;
        }
        return CitizenCommandResult.RUNNING;
    }

    private static int surfaceY(World world, int x, int z) {
        int y = Math.max(1, world.getTopSolidOrLiquidBlock(x, z));
        while (y > 1 && (WorkBlocks.isLog(world, x, y - 1, z) || WorkBlocks.isLeaves(world, x, y - 1, z))) {
            y--;
        }
        return y;
    }

    private CitizenCommandResult work(EntityCitizen citizen) {
        ItemStack tool = citizen.getInventory()
            .getHeldTool();
        if (!WorkBlocks.isEffectiveOn(tool, referenceBlock())) {
            reason = hadTool ? "tool broken" : "no tool";
            return startReturn(citizen);
        }
        hadTool = true;
        if (!citizen.worldObj.blockExists(area.getCenterX(), 64, area.getCenterZ())) {
            reason = "out of range";
            return startReturn(citizen);
        }
        if (!hasTarget && !acquireTarget(citizen)) {
            reason = "cleared";
            return startReturn(citizen);
        }

        if (citizen.getDistanceSq(anchorX + 0.5D, anchorY + 0.5D, anchorZ + 0.5D) > WORK_REACH_SQ) {
            return approach(citizen);
        }
        approach.restart();
        citizen.getNavigator()
            .clearPathEntity();
        return dig(citizen, targetX, targetY, targetZ, false);
    }

    private CitizenCommandResult approach(EntityCitizen citizen) {
        if (approach.stalled(citizen.getDistanceSq(anchorX + 0.5D, anchorY + 0.5D, anchorZ + 0.5D))) {
            return abandonTarget(citizen);
        }
        return move(citizen, canTunnel());
    }

    private CitizenCommandResult move(EntityCitizen citizen, boolean tunnelling) {
        if (tunnelling) {
            CitizenCommandResult climb = pillar(citizen);
            if (climb != null) {
                return climb;
            }
            int[] step = tunnelStep(citizen);
            if (step != null) {
                int[] blocked = firstBlocked(citizen, step);
                if (blocked != null) {
                    return dig(citizen, blocked[0], blocked[1], blocked[2], true);
                }
                breaker.clear(citizen);
                if (citizen.getNavigator()
                    .noPath()) {
                    citizen.getNavigator()
                        .tryMoveToXYZ(step[0] + 0.5D, step[1], step[2] + 0.5D, WALK_SPEED);
                }
                return CitizenCommandResult.RUNNING;
            }
        }
        breaker.clear(citizen);
        if (citizen.getNavigator()
            .noPath()) {
            citizen.getNavigator()
                .tryMoveToXYZ(anchorX + 0.5D, anchorY, anchorZ + 0.5D, WALK_SPEED);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult pillar(EntityCitizen citizen) {
        World world = citizen.worldObj;
        int cx = MathHelper.floor_double(citizen.posX);
        int cy = MathHelper.floor_double(citizen.boundingBox.minY + 0.1D);
        int cz = MathHelper.floor_double(citizen.posZ);
        if (anchorY - cy < PILLAR_CLIMB || !WorkBlocks.blocksMovement(world, cx, cy - 1, cz)) {
            return null;
        }
        if (WorkBlocks.blocksMovement(world, cx, cy + 1, cz)) {
            return dig(citizen, cx, cy + 1, cz, true);
        }
        if (WorkBlocks.blocksMovement(world, cx, cy + 2, cz)) {
            return dig(citizen, cx, cy + 2, cz, true);
        }
        breaker.clear(citizen);
        if (pillarCooldown > 0) {
            pillarCooldown--;
            return CitizenCommandResult.RUNNING;
        }

        ItemStack scaffold = citizen.getInventory()
            .takeScaffold();
        if (scaffold == null) {
            return null;
        }
        WorldOps.stepUp(citizen, cx, cy, cz, scaffold);
        pillarCooldown = PILLAR_INTERVAL;
        return CitizenCommandResult.RUNNING;
    }

    private int[] tunnelStep(EntityCitizen citizen) {
        int cx = MathHelper.floor_double(citizen.posX);
        int cy = MathHelper.floor_double(citizen.boundingBox.minY + 0.1D);
        int cz = MathHelper.floor_double(citizen.posZ);
        int dx = anchorX - cx;
        int dy = anchorY - cy;
        int dz = anchorZ - cz;
        if (dx == 0 && dy == 0 && dz == 0) {
            return null;
        }

        int stepX = 0;
        int stepZ = 0;
        if (Math.abs(dx) >= Math.abs(dz)) {
            stepX = Integer.signum(dx);
        } else {
            stepZ = Integer.signum(dz);
        }
        if (stepX == 0 && stepZ == 0) {
            if (dz != 0) {
                stepZ = Integer.signum(dz);
            } else {
                stepX = 1;
            }
        }
        return new int[] { cx + stepX, cy + Integer.signum(dy), cz + stepZ, cx, cy, cz };
    }

    private int[] firstBlocked(EntityCitizen citizen, int[] step) {
        World world = citizen.worldObj;
        int nx = step[0];
        int ny = step[1];
        int nz = step[2];
        int cy = step[4];
        if (ny > cy && WorkBlocks.blocksMovement(world, step[3], cy + 2, step[5])) {
            return new int[] { step[3], cy + 2, step[5] };
        }
        if (WorkBlocks.blocksMovement(world, nx, ny, nz)) {
            return new int[] { nx, ny, nz };
        }
        if (WorkBlocks.blocksMovement(world, nx, ny + 1, nz)) {
            return new int[] { nx, ny + 1, nz };
        }
        return null;
    }

    private CitizenCommandResult dig(EntityCitizen citizen, int x, int y, int z, boolean tunnelling) {
        if (WorkBlocks.isLiquid(citizen.worldObj, x, y, z)) {
            return phase == WorkPhase.FINISH ? CitizenCommandResult.RUNNING : abandonTarget(citizen);
        }
        if (!breaker.isAt(x, y, z)) {
            breaker.setTarget(citizen, x, y, z);
        }
        citizen.getLookHelper()
            .setLookPosition(x + 0.5D, y + 0.5D, z + 0.5D, LOOK_SPEED, LOOK_SPEED);

        DigResult result = breaker.tick(citizen, !tunnelling);
        switch (result) {
            case PROGRESS:
                return CitizenCommandResult.RUNNING;
            case INVENTORY_FULL:
                if (phase == WorkPhase.FINISH) {
                    return CitizenCommandResult.RUNNING;
                }
                reason = "inventory full";
                resumeAfterDropOff = true;
                return startReturn(citizen);
            case TOOL_BROKEN:
                if (phase == WorkPhase.FINISH) {
                    return CitizenCommandResult.RUNNING;
                }
                if (!tunnelling) {
                    harvested++;
                    onHarvested(citizen, x, y, z);
                    hasTarget = false;
                }
                reason = "tool broken";
                return startReturn(citizen);
            case BROKEN:
                approach.restart();
                if (!tunnelling) {
                    harvested++;
                    onHarvested(citizen, x, y, z);
                    hasTarget = false;
                }
                return CitizenCommandResult.RUNNING;
            case GONE:
                if (!tunnelling) {
                    hasTarget = false;
                }
                return CitizenCommandResult.RUNNING;
            default:
                if (phase == WorkPhase.FINISH) {
                    return CitizenCommandResult.RUNNING;
                }
                return abandonTarget(citizen);
        }
    }

    private CitizenCommandResult abandonTarget(EntityCitizen citizen) {
        breaker.clear(citizen);
        skipped.add(BlockKey.pack(anchorX, anchorY, anchorZ));
        onAbandon(citizen);
        hasTarget = false;
        approach.restart();
        if (++skips > MAX_SKIPS) {
            reason = "unreachable";
            return startReturn(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult startReturn(EntityCitizen citizen) {
        breaker.clear(citizen);
        hasTarget = false;
        phase = WorkPhase.FINISH;
        homeTicks = 0;
        leg.clear(citizen);
        return goHome(citizen);
    }

    private CitizenCommandResult goHome(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony == null) {
            return CitizenCommandResult.DONE;
        }
        ColonySite site = colony.site(ColonySiteKind.DROP_OFF);
        boolean dropOff = site.isPresent() && colony.getDimension() == citizen.worldObj.provider.dimensionId;
        if (dropOff) {
            anchorX = site.getX();
            anchorY = site.getY();
            anchorZ = site.getZ();
        } else {
            anchorX = colony.getX();
            anchorY = colony.getY() + 1;
            anchorZ = colony.getZ();
        }
        if (citizen.getDistanceSq(anchorX + 0.5D, anchorY, anchorZ + 0.5D)
            <= (dropOff ? DROP_OFF_REACH_SQ : HOME_REACH_SQ)) {
            return arrive(citizen, dropOff);
        }
        if (++homeTicks > HOME_TIMEOUT) {
            return CitizenCommandResult.DONE;
        }

        TravelLeg.Step step = leg.walk(citizen, anchorX, anchorY, anchorZ, homeTicks % LEG_RETRY == 1);
        if (step == TravelLeg.Step.RUNNING && leg.isActive()) {
            return CitizenCommandResult.RUNNING;
        }
        if (step == TravelLeg.Step.ARRIVED) {
            return arrive(citizen, dropOff);
        }
        return move(citizen, true);
    }

    private CitizenCommandResult arrive(EntityCitizen citizen, boolean dropOff) {
        leg.clear(citizen);
        citizen.getNavigator()
            .clearPathEntity();
        return dropOff ? deposit(citizen) : CitizenCommandResult.DONE;
    }

    private CitizenCommandResult deposit(EntityCitizen citizen) {
        citizen.getLookHelper()
            .setLookPosition(anchorX + 0.5D, anchorY + 0.5D, anchorZ + 0.5D, LOOK_SPEED, LOOK_SPEED);
        IInventory target = Inventories.at(citizen.worldObj, anchorX, anchorY, anchorZ);
        if (target == null) {
            reason = "drop-off gone";
            return CitizenCommandResult.DONE;
        }
        int moved = citizen.getInventory()
            .deposit(target);
        if (moved > 0) {
            citizen.swingItem();
        }
        if (!resumeAfterDropOff) {
            return CitizenCommandResult.DONE;
        }
        resumeAfterDropOff = false;
        if (moved <= 0 || !citizen.getInventory()
            .hasFreeMainSlot()) {
            reason = "drop-off full";
            return CitizenCommandResult.DONE;
        }
        reason = "";
        homeTicks = 0;
        phase = WorkPhase.TRAVEL;
        return CitizenCommandResult.RUNNING;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        breaker.clear(citizen);
        leg.clear(citizen);
        citizen.getNavigator()
            .clearPathEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        area.readFromNBT(tag);
        phase = WorkPhase.byId(tag.getInteger("phase"));
        harvested = tag.getInteger("harvested");
        reason = tag.getString("reason");
        resumeAfterDropOff = tag.getBoolean("resume");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        area.writeToNBT(tag);
        tag.setInteger("phase", phase.id());
        tag.setInteger("harvested", harvested);
        tag.setString("reason", reason);
        tag.setBoolean("resume", resumeAfterDropOff);
    }

    @Override
    public String describe() {
        String state = phase == WorkPhase.TRAVEL ? "walking there" : phase == WorkPhase.WORK ? "working" : "returning";
        String tail = reason.isEmpty() ? "" : " (" + reason + ")";
        return getId() + " " + state + " " + harvested + " " + unitName() + tail;
    }
}
