package com.enn3developer.gregcolonies.entity.ai.command;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;

public class CitizenCommandMoveTo extends CitizenCommand {

    public static final String ID = "move_to";

    private static final int DEFAULT_TIMEOUT = 600;

    private int x;
    private int y;
    private int z;
    private double speed = 0.6D;
    private int timeout = DEFAULT_TIMEOUT;
    private int ticks;

    public CitizenCommandMoveTo() {}

    public CitizenCommandMoveTo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void start(EntityCitizen citizen) {
        ticks = 0;
        citizen.getNavigator()
            .tryMoveToXYZ(x + 0.5D, y, z + 0.5D, speed);
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        ticks++;
        if (citizen.getDistanceSq(x + 0.5D, y, z + 0.5D) < 2.0D) {
            return CitizenCommandResult.DONE;
        }
        if (ticks > timeout) {
            return CitizenCommandResult.FAILED;
        }
        if (citizen.getNavigator()
            .noPath()
            && !citizen.getNavigator()
                .tryMoveToXYZ(x + 0.5D, y, z + 0.5D, speed)) {
            return CitizenCommandResult.FAILED;
        }
        return CitizenCommandResult.RUNNING;
    }

    @Override
    public void finish(EntityCitizen citizen) {
        citizen.getNavigator()
            .clearPathEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        x = tag.getInteger("x");
        y = tag.getInteger("y");
        z = tag.getInteger("z");
        speed = tag.hasKey("speed") ? tag.getDouble("speed") : speed;
        timeout = tag.hasKey("timeout") ? tag.getInteger("timeout") : DEFAULT_TIMEOUT;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setDouble("speed", speed);
        tag.setInteger("timeout", timeout);
    }

    @Override
    public String describe() {
        return ID + " " + x + "/" + y + "/" + z;
    }
}
