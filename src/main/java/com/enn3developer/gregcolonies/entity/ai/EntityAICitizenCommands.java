package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class EntityAICitizenCommands extends EntityAIBase {

    private final EntityCitizen citizen;

    public EntityAICitizenCommands(EntityCitizen citizen) {
        this.citizen = citizen;
        setMutexBits(7);
    }

    @Override
    public boolean shouldExecute() {
        return citizen.getCommands()
            .hasWork();
    }

    @Override
    public boolean continueExecuting() {
        return citizen.getCommands()
            .hasWork();
    }

    @Override
    public void updateTask() {
        citizen.getCommands()
            .update(citizen);
    }

    @Override
    public void resetTask() {
        citizen.getNavigator()
            .clearPathEntity();
    }
}
