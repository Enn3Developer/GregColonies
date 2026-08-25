package com.enn3developer.gregcolonies.entity;

import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.ai.CitizenCommandQueue;
import com.enn3developer.gregcolonies.entity.ai.EntityAICitizenCommands;

public class EntityCitizen extends EntityVillager {

    public static final String NAME = "citizen";

    private final CitizenCommandQueue commands = new CitizenCommandQueue();
    private final CitizenParameters parameters = new CitizenParameters();
    private int colonyId;

    public EntityCitizen(World world) {
        super(world);
        tasks.taskEntries.clear();
        targetTasks.taskEntries.clear();
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAICitizenCommands(this));
        tasks.addTask(2, new EntityAIOpenDoor(this, true));
        tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(4, new EntityAIWander(this, 0.4D));
    }

    public CitizenCommandQueue getCommands() {
        return commands;
    }

    public CitizenParameters getParameters() {
        return parameters;
    }

    public int getColonyId() {
        return colonyId;
    }

    public void setColonyId(int colonyId) {
        this.colonyId = colonyId;
    }

    @Override
    protected void updateAITick() {}

    @Override
    public boolean interact(EntityPlayer player) {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("colonyId", colonyId);

        NBTTagCompound commandsTag = new NBTTagCompound();
        commands.writeToNBT(commandsTag);
        tag.setTag("commands", commandsTag);

        NBTTagCompound parametersTag = new NBTTagCompound();
        parameters.writeToNBT(parametersTag);
        tag.setTag("parameters", parametersTag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        colonyId = tag.getInteger("colonyId");
        commands.readFromNBT(tag.getCompoundTag("commands"));
        parameters.readFromNBT(tag.getCompoundTag("parameters"));
    }
}
