package com.enn3developer.gregcolonies.entity.ai;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskBath;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskElevator;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskReturnHome;
import com.enn3developer.gregcolonies.entity.ai.idle.IdleTaskWander;

public class EntityAICitizenIdle extends EntityAICitizenAuto {

    public EntityAICitizenIdle(EntityCitizen citizen) {
        super(citizen, new IdleTaskReturnHome(), new IdleTaskBath(), new IdleTaskElevator(), new IdleTaskWander());
    }

    @Override
    protected boolean canRun() {
        return !getCitizen().isViewed() && !getCitizen().getCommands()
            .hasWork();
    }
}
