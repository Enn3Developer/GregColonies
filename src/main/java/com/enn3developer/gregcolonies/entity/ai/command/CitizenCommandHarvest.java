package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;
import com.enn3developer.gregcolonies.entity.ai.work.DigResult;
import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public abstract class CitizenCommandHarvest extends CitizenCommand {

    protected static final int PHASE_TRAVEL = 0;

    protected static final int PHASE_WORK = 1;

    protected static final int PHASE_RETURN = 2;

    private static final double WORK_REACH_SQ = 25.0D;

    private static final double WALK_SPEED = 0.6D;

    private static final float LOOK_SPEED = 30.0F;

    private static final int APPROACH_TIMEOUT = 400;

    private static final double APPROACH_EPSILON = 0.5D;

    private static final int MAX_SKIPS = 24;

    private static final double HOME_REACH_SQ = 9.0D;

    private static final int HOME_TIMEOUT = 6000;

    private static final int LEG_RETRY = 200;

    private static final int PILLAR_INTERVAL = 10;

    private static final int PILLAR_CLIMB = 2;

    protected int minX;

    protected int minY;

    protected int minZ;

    protected int maxX;

    protected int maxY;

    protected int maxZ;

    private final BlockBreaker breaker = new BlockBreaker();

    private final Set<Long> skipped = new HashSet<>();

    private int phase = PHASE_TRAVEL;

    private CitizenCommandMoveTo leg;

    private String reason = "";

    private int harvested;

    private boolean hasTarget;

    private boolean hadTool;

    private int targetX;

    private int targetY;

    private int targetZ;

    private int anchorX;

    private int anchorY;

    private int anchorZ;

    private int approachTicks;

    private double bestApproachSq = Double.MAX_VALUE;

    private int skips;

    private int homeTicks;

    private int pillarCooldown;

    protected CitizenCommandHarvest() {}

    protected CitizenCommandHarvest(int x1, int y1, int z1, int x2, int y2, int z2) {
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);
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
        approachTicks = 0;
        bestApproachSq = Double.MAX_VALUE;
    }

    protected boolean isSkipped(int x, int y, int z) {
        return skipped.contains(WorkBlocks.pack(x, y, z));
    }

    protected int getHarvested() {
        return harvested;
    }

    @Override
    public void start(EntityCitizen citizen) {
        leg = null;
        hasTarget = false;
        approachTicks = 0;
        skips = 0;
        homeTicks = 0;
        pillarCooldown = 0;
        hadTool = false;
        skipped.clear();
        breaker.clear(citizen);
        resetWork();
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        if (phase == PHASE_TRAVEL) {
            return travel(citizen);
        }
        if (phase == PHASE_WORK) {
            return work(citizen);
        }
        return goHome(citizen);
    }

    private CitizenCommandResult travel(EntityCitizen citizen) {
        if (leg == null) {
            int x = (minX + maxX) / 2;
            int z = (minZ + maxZ) / 2;
            leg = new CitizenCommandMoveTo(x, surfaceY(citizen.worldObj, x, z), z);
            leg.start(citizen);
        }
        CitizenCommandResult result = leg.update(citizen);
        if (result == CitizenCommandResult.RUNNING) {
            return CitizenCommandResult.RUNNING;
        }
        leg.finish(citizen);
        leg = null;
        phase = PHASE_WORK;
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
        if (!citizen.worldObj.blockExists((minX + maxX) / 2, 64, (minZ + maxZ) / 2)) {
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
        approachTicks = 0;
        citizen.getNavigator()
            .clearPathEntity();
        return dig(citizen, targetX, targetY, targetZ, false);
    }

    private CitizenCommandResult approach(EntityCitizen citizen) {
        double distanceSq = citizen.getDistanceSq(anchorX + 0.5D, anchorY + 0.5D, anchorZ + 0.5D);
        if (distanceSq < bestApproachSq - APPROACH_EPSILON) {
            bestApproachSq = distanceSq;
            approachTicks = 0;
        } else if (++approachTicks > APPROACH_TIMEOUT) {
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
        Block block = Block.getBlockFromItem(scaffold.getItem());
        citizen.getNavigator()
            .clearPathEntity();
        world.setBlock(cx, cy, cz, block, scaffold.getItemDamage(), 3);
        world.playSoundEffect(
            cx + 0.5D,
            cy + 0.5D,
            cz + 0.5D,
            block.stepSound.func_150496_b(),
            (block.stepSound.getVolume() + 1.0F) / 2.0F,
            block.stepSound.getPitch() * 0.8F);
        citizen.setPosition(cx + 0.5D, cy + 1.0D, cz + 0.5D);
        citizen.motionY = 0.0D;
        citizen.fallDistance = 0.0F;
        citizen.swingItem();
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
            return phase == PHASE_RETURN ? CitizenCommandResult.RUNNING : abandonTarget(citizen);
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
                if (phase == PHASE_RETURN) {
                    return CitizenCommandResult.RUNNING;
                }
                reason = "inventory full";
                return startReturn(citizen);
            case TOOL_BROKEN:
                if (phase == PHASE_RETURN) {
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
                approachTicks = 0;
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
                if (phase == PHASE_RETURN) {
                    return CitizenCommandResult.RUNNING;
                }
                return abandonTarget(citizen);
        }
    }

    private CitizenCommandResult abandonTarget(EntityCitizen citizen) {
        breaker.clear(citizen);
        skipped.add(WorkBlocks.pack(anchorX, anchorY, anchorZ));
        onAbandon(citizen);
        hasTarget = false;
        approachTicks = 0;
        if (++skips > MAX_SKIPS) {
            reason = "unreachable";
            return startReturn(citizen);
        }
        return CitizenCommandResult.RUNNING;
    }

    private CitizenCommandResult startReturn(EntityCitizen citizen) {
        breaker.clear(citizen);
        hasTarget = false;
        phase = PHASE_RETURN;
        homeTicks = 0;
        if (leg != null) {
            leg.finish(citizen);
            leg = null;
        }
        return goHome(citizen);
    }

    private CitizenCommandResult goHome(EntityCitizen citizen) {
        Colony colony = citizen.getColony();
        if (colony == null) {
            return CitizenCommandResult.DONE;
        }
        anchorX = colony.getX();
        anchorY = colony.getY() + 1;
        anchorZ = colony.getZ();
        if (citizen.getDistanceSq(anchorX + 0.5D, anchorY, anchorZ + 0.5D) <= HOME_REACH_SQ) {
            return CitizenCommandResult.DONE;
        }
        if (++homeTicks > HOME_TIMEOUT) {
            return CitizenCommandResult.DONE;
        }

        if (leg == null && homeTicks % LEG_RETRY == 1) {
            leg = new CitizenCommandMoveTo(anchorX, anchorY, anchorZ);
            leg.start(citizen);
        }
        if (leg != null) {
            CitizenCommandResult result = leg.update(citizen);
            if (result == CitizenCommandResult.RUNNING) {
                return CitizenCommandResult.RUNNING;
            }
            leg.finish(citizen);
            leg = null;
            if (result == CitizenCommandResult.DONE) {
                return CitizenCommandResult.DONE;
            }
        }
        return move(citizen, true);
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
        reason = tag.getString("reason");
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
        tag.setString("reason", reason);
    }

    @Override
    public String describe() {
        String state = phase == PHASE_TRAVEL ? "walking there" : phase == PHASE_WORK ? "working" : "returning";
        String tail = reason.isEmpty() ? "" : " (" + reason + ")";
        return getId() + " " + state + " " + harvested + " " + unitName() + tail;
    }
}
