package com.enn3developer.gregcolonies.entity.ai;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public abstract class CitizenCommand {

    public abstract String getId();

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
