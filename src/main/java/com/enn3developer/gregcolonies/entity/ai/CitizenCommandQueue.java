package com.enn3developer.gregcolonies.entity.ai;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class CitizenCommandQueue {

    private final Deque<CitizenCommand> pending = new ArrayDeque<>();
    private CitizenCommand current;
    private boolean started;

    public void enqueue(CitizenCommand command) {
        pending.addLast(command);
    }

    public void enqueueNext(CitizenCommand command) {
        pending.addFirst(command);
    }

    public CitizenCommand getCurrent() {
        return current;
    }

    public int getPendingCount() {
        return pending.size();
    }

    public boolean hasWork() {
        return current != null || !pending.isEmpty();
    }

    public void clear(EntityCitizen citizen) {
        if (current != null && started) {
            current.finish(citizen);
        }
        current = null;
        started = false;
        pending.clear();
    }

    public void update(EntityCitizen citizen) {
        if (current == null) {
            current = pending.pollFirst();
            started = false;
        }
        if (current == null) {
            return;
        }
        if (!started) {
            current.start(citizen);
            started = true;
        }

        CitizenCommandResult result = current.update(citizen);
        if (result == CitizenCommandResult.RUNNING) {
            return;
        }

        current.finish(citizen);
        current = null;
        started = false;
    }

    public void readFromNBT(NBTTagCompound tag) {
        pending.clear();
        current = null;
        started = false;

        if (tag.hasKey("current", 10)) {
            current = CitizenCommandRegistry.read(tag.getCompoundTag("current"));
        }
        NBTTagList list = tag.getTagList("pending", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            CitizenCommand command = CitizenCommandRegistry.read(list.getCompoundTagAt(i));
            if (command != null) {
                pending.addLast(command);
            }
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        if (current != null) {
            tag.setTag("current", CitizenCommandRegistry.write(current));
        }
        NBTTagList list = new NBTTagList();
        for (CitizenCommand command : pending) {
            list.appendTag(CitizenCommandRegistry.write(command));
        }
        tag.setTag("pending", list);
    }
}
