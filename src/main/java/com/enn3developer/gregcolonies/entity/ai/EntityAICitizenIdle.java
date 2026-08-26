package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTask;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskBath;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskReturnHome;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskWander;

public class EntityAICitizenIdle extends EntityAIBase {

    private final EntityCitizen citizen;

    private final IdleTask[] tasks;

    private IdleTask active;

    private boolean finished;

    public EntityAICitizenIdle(EntityCitizen citizen) {
        this.citizen = citizen;
        this.tasks = new IdleTask[] { new IdleTaskReturnHome(), new IdleTaskBath(), new IdleTaskWander() };
        setMutexBits(1);
    }

    public String describeActive() {
        return active == null ? "" : active.describe();
    }

    @Override
    public boolean shouldExecute() {
        if (!isIdle()) {
            return false;
        }
        Colony colony = citizen.getColony();
        if (colony == null) {
            return false;
        }
        for (IdleTask task : tasks) {
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
        return !finished && active != null && isIdle();
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

    private boolean isIdle() {
        return !citizen.isViewed() && !citizen.getCommands()
            .hasWork();
    }
}
