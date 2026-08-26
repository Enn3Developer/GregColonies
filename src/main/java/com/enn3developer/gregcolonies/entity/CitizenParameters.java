package com.enn3developer.gregcolonies.entity;

import net.minecraft.nbt.NBTTagCompound;

public class CitizenParameters {

    private static final String GENDER = "gender";

    private CitizenGender gender;

    public CitizenGender getGender() {
        return gender;
    }

    public void setGender(CitizenGender gender) {
        this.gender = gender;
    }

    public void readFromNBT(NBTTagCompound tag) {
        gender = CitizenGender.byId(tag.getByte(GENDER));
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setByte(GENDER, CitizenGender.idOf(gender));
    }
}
