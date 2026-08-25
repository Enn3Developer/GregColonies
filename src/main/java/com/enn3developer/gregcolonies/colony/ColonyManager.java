package com.enn3developer.gregcolonies.colony;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;

public class ColonyManager extends WorldSavedData {

    public static final String DATA_NAME = "gregcolonies_colonies";

    private final Map<Integer, Colony> colonies = new LinkedHashMap<>();
    private int nextId = 1;

    public ColonyManager() {
        super(DATA_NAME);
    }

    public ColonyManager(String name) {
        super(name);
    }

    public static ColonyManager get(World world) {
        MapStorage storage = world.mapStorage;
        ColonyManager manager = (ColonyManager) storage.loadData(ColonyManager.class, DATA_NAME);
        if (manager == null) {
            manager = new ColonyManager();
            storage.setData(DATA_NAME, manager);
        }
        return manager;
    }

    public Collection<Colony> getColonies() {
        return Collections.unmodifiableCollection(colonies.values());
    }

    public int getColonyCount() {
        return colonies.size();
    }

    public Colony getColony(int id) {
        return colonies.get(id);
    }

    public Colony getColonyAt(int dimension, int x, int y, int z) {
        for (Colony colony : colonies.values()) {
            if (colony.isCenteredAt(dimension, x, y, z)) {
                return colony;
            }
        }
        return null;
    }

    public Colony getNearestColony(int dimension, int x, int z) {
        Colony nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Colony colony : colonies.values()) {
            double distance = colony.distanceSqTo(dimension, x, z);
            if (distance < nearestDistance) {
                nearest = colony;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public Colony createColony(String name, UUID owner, String ownerName, int dimension, int x, int y, int z) {
        Colony colony = new Colony(nextId++, name, owner, ownerName, dimension, x, y, z);
        colonies.put(colony.getId(), colony);
        markDirty();
        return colony;
    }

    public boolean enqueueOrder(int colonyId, CitizenCommand command) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return false;
        }
        colony.enqueueOrder(command);
        markDirty();
        return true;
    }

    public CitizenCommand pollOrder(int colonyId) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return null;
        }
        CitizenCommand order = colony.pollOrder();
        if (order != null) {
            markDirty();
        }
        return order;
    }

    public int clearOrders(int colonyId) {
        Colony colony = colonies.get(colonyId);
        if (colony == null) {
            return 0;
        }
        int cleared = colony.getOrderCount();
        colony.clearOrders();
        if (cleared > 0) {
            markDirty();
        }
        return cleared;
    }

    public boolean removeColony(int id) {
        if (colonies.remove(id) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        colonies.clear();
        nextId = tag.getInteger("nextId");
        if (nextId < 1) {
            nextId = 1;
        }
        NBTTagList list = tag.getTagList("colonies", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Colony colony = Colony.readFromNBT(list.getCompoundTagAt(i));
            colonies.put(colony.getId(), colony);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("nextId", nextId);
        NBTTagList list = new NBTTagList();
        for (Colony colony : colonies.values()) {
            list.appendTag(colony.writeToNBT());
        }
        tag.setTag("colonies", list);
    }
}
