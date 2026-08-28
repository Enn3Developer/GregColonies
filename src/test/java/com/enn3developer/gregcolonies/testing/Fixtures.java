package com.enn3developer.gregcolonies.testing;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.entity.CitizenGender;
import com.enn3developer.gregcolonies.entity.CitizenJob;

public final class Fixtures {

    private Fixtures() {}

    public static ColonyCitizen citizen(UUID id, String name, String group, int dimension, int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("idMost", id.getMostSignificantBits());
        tag.setLong("idLeast", id.getLeastSignificantBits());
        tag.setString("name", name);
        tag.setString("group", group);
        tag.setByte("job", CitizenJob.idOf(CitizenJob.NONE));
        tag.setByte("gender", CitizenGender.idOf(CitizenGender.FEMALE));
        tag.setBoolean("child", false);
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setBoolean("hasBed", false);
        return ColonyCitizen.readFromNBT(tag);
    }

    public static Colony colonyWith(Colony base, ColonyCitizen... citizens) {
        NBTTagCompound tag = base.writeToNBT();
        NBTTagList list = new NBTTagList();
        for (ColonyCitizen citizen : citizens) {
            list.appendTag(citizen.writeToNBT());
        }
        tag.setTag("citizens", list);
        return Colony.readFromNBT(tag);
    }

    public static Blueprint cube(String name, int side) {
        Blueprint blueprint = Blueprint.empty(name, side, side, side);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        for (int y = 0; y < side; y++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    blueprint.setCell(x, y, z, cell);
                }
            }
        }
        return blueprint;
    }

    public static Blueprint single(String name) {
        Blueprint blueprint = Blueprint.empty(name, 1, 1, 1);
        blueprint.setCell(
            0,
            0,
            0,
            blueprint.getPalette()
                .cellFor(Blocks.stone, 0));
        return blueprint;
    }
}
