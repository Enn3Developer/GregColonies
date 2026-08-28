package com.enn3developer.gregcolonies.colony;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

public class ColonyManager extends WorldSavedData {

    public static final String DATA_NAME = "gregcolonies_colonies";

    private final ColonyRegistry registry = new ColonyRegistry(this::markDirty);

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

    public static ColonyRegistry registry(World world) {
        return get(world).getRegistry();
    }

    public ColonyRegistry getRegistry() {
        return registry;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        registry.clear();
        registry.setNextId(tag.getInteger("nextId"));
        NBTTagList list = tag.getTagList("colonies", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            registry.put(Colony.readFromNBT(list.getCompoundTagAt(i)));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("nextId", registry.getNextId());
        NBTTagList list = new NBTTagList();
        for (Colony colony : registry.getColonies()) {
            list.appendTag(colony.writeToNBT());
        }
        tag.setTag("colonies", list);
    }
}
