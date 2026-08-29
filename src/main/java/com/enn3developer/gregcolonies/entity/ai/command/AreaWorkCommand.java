package com.enn3developer.gregcolonies.entity.ai.command;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.colony.BlockKey;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.ApproachTracker;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.enn3developer.gregcolonies.entity.ai.TravelLeg;
import com.enn3developer.gregcolonies.entity.ai.WorkPhase;
import com.enn3developer.gregcolonies.entity.ai.work.BlockBreaker;

public abstract class AreaWorkCommand extends CitizenCommand {

    protected static final double WORK_REACH_SQ = 25.0D;

    protected static final double DROP_OFF_REACH_SQ = 16.0D;

    protected static final double WALK_SPEED = 0.6D;

    protected static final float LOOK_SPEED = 30.0F;

    protected static final int LEG_RETRY = 200;

    protected final WorkArea area = new WorkArea();

    protected final BlockBreaker breaker = new BlockBreaker();

    protected final TravelLeg leg = new TravelLeg();

    protected final ApproachTracker approach;

    private final Set<Long> skipped = new HashSet<>();

    private WorkPhase phase = WorkPhase.TRAVEL;

    private String reason = "";

    private int harvested;

    protected AreaWorkCommand(int approachTimeout) {
        approach = new ApproachTracker(approachTimeout);
    }

    protected abstract CitizenCommandResult travel(EntityCitizen citizen);

    protected abstract CitizenCommandResult work(EntityCitizen citizen);

    protected abstract CitizenCommandResult deliver(EntityCitizen citizen);

    protected abstract String describeWork();

    protected abstract String describeReturn();

    protected abstract String describeTally();

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        if (phase == WorkPhase.TRAVEL) {
            return travel(citizen);
        }
        if (phase == WorkPhase.WORK) {
            return work(citizen);
        }
        return deliver(citizen);
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
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        area.writeToNBT(tag);
        tag.setInteger("phase", phase.id());
        tag.setInteger("harvested", harvested);
        tag.setString("reason", reason);
    }

    @Override
    public String describe() {
        String state = phase == WorkPhase.TRAVEL ? "walking there"
            : phase == WorkPhase.WORK ? describeWork() : describeReturn();
        String tail = reason.isEmpty() ? "" : " (" + reason + ")";
        return getId() + " " + state + " " + describeTally() + tail;
    }

    protected WorkPhase getPhase() {
        return phase;
    }

    protected void setPhase(WorkPhase phase) {
        this.phase = phase;
    }

    protected boolean isReturning() {
        return phase == WorkPhase.FINISH;
    }

    protected String getReason() {
        return reason;
    }

    protected void setReason(String reason) {
        this.reason = reason;
    }

    protected int getHarvested() {
        return harvested;
    }

    protected void countHarvest() {
        harvested++;
    }

    protected boolean isSkipped(int x, int y, int z) {
        return skipped.contains(BlockKey.pack(x, y, z));
    }

    protected void skip(int x, int y, int z) {
        skipped.add(BlockKey.pack(x, y, z));
    }

    protected void clearSkipped() {
        skipped.clear();
    }

    protected void faceBlock(EntityCitizen citizen, int x, int y, int z) {
        citizen.getLookHelper()
            .setLookPosition(x + 0.5D, y + 0.5D, z + 0.5D, LOOK_SPEED, LOOK_SPEED);
    }
}
