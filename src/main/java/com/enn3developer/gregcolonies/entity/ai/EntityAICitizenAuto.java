package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.auto.AutoTask;

public abstract class EntityAICitizenAuto extends EntityAIBase {

    private final EntityCitizen citizen;

    private final AutoTask[] tasks;

    private AutoTask active;

    private boolean finished;

    protected EntityAICitizenAuto(EntityCitizen citizen, AutoTask... tasks) {
        this.citizen = citizen;
        this.tasks = tasks;
        setMutexBits(1);
    }

    protected abstract boolean canRun();

    protected EntityCitizen getCitizen() {
        return citizen;
    }

    public String describeActive() {
        return active == null ? "" : active.describe();
    }

    @Override
    public boolean shouldExecute() {
        if (!canRun()) {
            return false;
        }
        Colony colony = citizen.getColony();
        if (colony == null) {
            return false;
        }
        for (AutoTask task : tasks) {
            if (task.shouldStart(citizen, colony)) {
                active = task;
                finished = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public void startExecuting() {
        Colony colony = citizen.getColony();
        if (active == null || colony == null) {
            finished = true;
            return;
        }
        active.start(citizen, colony);
    }

    @Override
    public boolean continueExecuting() {
        return !finished && active != null && canRun();
    }

    @Override
    public void updateTask() {
        if (finished || active == null) {
            return;
        }
        Colony colony = citizen.getColony();
        if (colony == null || !active.update(citizen, colony)) {
            finished = true;
        }
    }

    @Override
    public void resetTask() {
        if (active != null) {
            active.finish(citizen);
            active = null;
        }
        finished = false;
        citizen.getNavigator()
            .clearPathEntity();
    }
}
