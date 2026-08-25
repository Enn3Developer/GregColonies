package com.enn3developer.gregcolonies.colony;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class ColonyCitizen {

    private UUID id;
    private String group = "";
    private int dimension;
    private int x;
    private int y;
    private int z;

    private ColonyCitizen() {}

    public ColonyCitizen(EntityCitizen citizen) {
        this.id = citizen.getUniqueID();
        this.group = citizen.getGroup();
        updatePosition(citizen);
    }

    public UUID getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group == null ? "" : group;
    }

    public int getDimension() {
        return dimension;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void updatePosition(EntityCitizen citizen) {
        this.dimension = citizen.worldObj.provider.dimensionId;
        this.x = (int) Math.floor(citizen.posX);
        this.y = (int) Math.floor(citizen.posY);
        this.z = (int) Math.floor(citizen.posZ);
    }

    public double distanceSqTo(int dimension, double x, double z) {
        if (this.dimension != dimension) {
            return Double.MAX_VALUE;
        }
        double dx = this.x + 0.5D - x;
        double dz = this.z + 0.5D - z;
        return dx * dx + dz * dz;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setString("group", group);
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        return tag;
    }

    public static ColonyCitizen readFromNBT(NBTTagCompound tag) {
        ColonyCitizen citizen = new ColonyCitizen();
        citizen.id = new UUID(tag.getLong("idMost"), tag.getLong("idLeast"));
        citizen.group = tag.getString("group");
        citizen.dimension = tag.getInteger("dim");
        citizen.x = tag.getInteger("x");
        citizen.y = tag.getInteger("y");
        citizen.z = tag.getInteger("z");
        return citizen;
    }
}
