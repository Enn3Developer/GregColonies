package com.enn3developer.gregcolonies.entity.ai;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.living.LivingTaskFood;
import com.enn3developer.gregcolonies.entity.ai.living.LivingTaskSleep;

public class EntityAICitizenLiving extends EntityAICitizenAuto {

    public EntityAICitizenLiving(EntityCitizen citizen) {
        super(citizen, new LivingTaskFood(), new LivingTaskSleep());
    }

    @Override
    protected boolean canRun() {
        return !getCitizen().isViewed();
    }
}
