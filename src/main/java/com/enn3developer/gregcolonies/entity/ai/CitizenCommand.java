package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public abstract class CitizenCommand {

    private String targetGroup = "";

    public abstract String getId();

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup == null ? "" : targetGroup;
    }

    public final boolean canBeClaimedBy(EntityCitizen citizen) {
        return (targetGroup.isEmpty() || targetGroup.equals(citizen.getGroup())) && canBeTakenBy(citizen);
    }

    public boolean canBeTakenBy(EntityCitizen citizen) {
        return true;
    }

    public boolean fearsEnemies() {
        return true;
    }

    public void start(EntityCitizen citizen) {}

    public abstract CitizenCommandResult update(EntityCitizen citizen);

    public void finish(EntityCitizen citizen) {}

    public void readFromNBT(NBTTagCompound tag) {}

    public void writeToNBT(NBTTagCompound tag) {}

    public String describe() {
        return getId();
    }
}
