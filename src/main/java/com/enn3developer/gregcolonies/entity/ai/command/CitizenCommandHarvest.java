package com.enn3developer.gregcolonies.entity.ai.command;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.TravelLeg;
import com.enn3developer.gregcolonies.entity.ai.WorkPhase;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;
import com.enn3developer.gregcolonies.entity.ai.work.WorldOps;

public abstract class CitizenCommandHarvest extends AreaWorkCommand {

    private static final int APPROACH_TIMEOUT = 400;

    private static final int MAX_SKIPS = 24;

    private static final double HOME_REACH_SQ = 9.0D;

    private static final int HOME_TIMEOUT = 6000;

    private static final int PILLAR_INTERVAL = 10;

    private static final int PILLAR_CLIMB = 2;

    private boolean hasTarget;

    private boolean hadTool;

    private boolean resumeAfterDropOff;

    private int targetX;

    private int targetY;

    private int targetZ;

    private int anchorX;

    private int anchorY;

    private int anchorZ;

    private int skips;

    private int homeTicks;

    private int travelY;

    private int pillarCooldown;

    protected CitizenCommandHarvest() {
        super(APPROACH_TIMEOUT);
    }

    protected CitizenCommandHarvest(int x1, int y1, int z1, int x2, int y2, int z2) {
        super(APPROACH_TIMEOUT);
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
        clearSkipped();
        breaker.clear(citizen);
        resetWork();
    }

    @Override
    protected CitizenCommandResult deliver(EntityCitizen citizen) {
        return goHome(citizen);
    }

    @Override
    protected CitizenCommandResult travel(EntityCitizen citizen) {
        int x = area.getCenterX();
        int z = area.getCenterZ();
        if (!leg.isActive()) {
            travelY = surfaceY(citizen.worldObj, x, z);
        }
        if (leg.walk(citizen, x, travelY, z) != TravelLeg.Step.RUNNING) {
            setPhase(WorkPhase.WORK);
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

    @Override
    protected CitizenCommandResult work(EntityCitizen citizen) {
        ItemStack tool = citizen.getInventory()
            .getHeldTool();
        if (!WorkBlocks.isEffectiveOn(tool, referenceBlock())) {
            setReason(hadTool ? "tool broken" : "no tool");
            return startReturn(citizen);
        }
        hadTool = true;
        if (!citizen.worldObj.blockExists(area.getCenterX(), 64, area.getCenterZ())) {
            setReason("out of range");
            return startReturn(citizen);
        }
        if (!hasTarget && !acquireTarget(citizen)) {
            setReason("cleared");
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
            return isReturning() ? CitizenCommandResult.RUNNING : abandonTarget(citizen);
        }
        if (!breaker.isAt(x, y, z)) {
            breaker.setTarget(citizen, x, y, z);
        }
        faceBlock(citizen, x, y, z);

        DigResult result = breaker.tick(citizen, !tunnelling);
        switch (result) {
            case PROGRESS:
                return CitizenCommandResult.RUNNING;
            case INVENTORY_FULL:
                if (isReturning()) {
                    return CitizenCommandResult.RUNNING;
                }
                setReason("inventory full");
                resumeAfterDropOff = true;
                return startReturn(citizen);
            case TOOL_BROKEN:
                if (isReturning()) {
                    return CitizenCommandResult.RUNNING;
                }
                if (!tunnelling) {
                    countHarvest();
                    onHarvested(citizen, x, y, z);
                    hasTarget = false;
                }
                setReason("tool broken");
                return startReturn(citizen);
            case BROKEN:
                approach.restart();
                if (!tunnelling) {
                    countHarvest();
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
                if (isReturning()) {
                    return CitizenCommandResult.RUNNING;
                }
                return abandonTarget(citizen);
        }
    }

    private CitizenCommandResult abandonTarget(EntityCitizen citizen) {
        breaker.clear(citizen);
        skip(anchorX, anchorY, anchorZ);
        onAbandon(citizen);
        hasTarget = false;
        approach.restart();
        if (++skips > MAX_SKIPS) {
            setReason("unreachable");
            return startReturn(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult startReturn(EntityCitizen citizen) {
        breaker.clear(citizen);
        hasTarget = false;
        setPhase(WorkPhase.FINISH);
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
        faceBlock(citizen, anchorX, anchorY, anchorZ);
        IInventory target = Inventories.at(citizen.worldObj, anchorX, anchorY, anchorZ);
        if (target == null) {
            setReason("drop-off gone");
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
            setReason("drop-off full");
            return CitizenCommandResult.DONE;
        }
        setReason("");
        homeTicks = 0;
        setPhase(WorkPhase.TRAVEL);
        return CitizenCommandResult.RUNNING;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        resumeAfterDropOff = tag.getBoolean("resume");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("resume", resumeAfterDropOff);
    }

    @Override
    protected String describeWork() {
        return "working";
    }

    @Override
    protected String describeReturn() {
        return "returning";
    }

    @Override
    protected String describeTally() {
        return getHarvested() + " " + unitName();
    }
}
