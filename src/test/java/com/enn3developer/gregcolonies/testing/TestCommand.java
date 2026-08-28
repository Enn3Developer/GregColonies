package com.enn3developer.gregcolonies.testing;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandRegistry;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;

public class TestCommand extends CitizenCommand {

    public static final String ID = "gregcolonies:test";

    private static boolean registered;

    private int payload;

    public static synchronized void ensureRegistered() {
        if (registered) return;
        CitizenCommandRegistry.register(ID, TestCommand::new);
        registered = true;
    }

    public TestCommand() {
        this(0);
    }

    public TestCommand(int payload) {
        ensureRegistered();
        this.payload = payload;
    }

    public int getPayload() {
        return payload;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public CitizenCommandResult update(EntityCitizen citizen) {
        return CitizenCommandResult.DONE;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("payload", payload);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        payload = tag.getInteger("payload");
    }
}
