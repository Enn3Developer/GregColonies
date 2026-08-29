package com.enn3developer.gregcolonies.colony;

import net.minecraft.nbt.NBTTagCompound;

public class ColonyHome {

    public static final int MAX_HEIGHT = 16;

    private static final long SCAN_INTERVAL = 200L;

    private int id;

    private final WorkArea area = new WorkArea();

    private int beds;

    private long scannedAt = Long.MIN_VALUE;

    private ColonyHome() {}

    public ColonyHome(int id, WorkArea area, int beds) {
        this.id = id;
        this.area.copyFrom(area);
        this.beds = beds;
    }

    public int getId() {
        return id;
    }

    public WorkArea getArea() {
        return area;
    }

    public int getBeds() {
        return beds;
    }

    public void setBeds(int beds) {
        this.beds = Math.max(beds, 0);
    }

    public boolean contains(int x, int y, int z) {
        return area.contains(x, y, z);
    }

    public boolean needsScan(long time) {
        return time - scannedAt >= SCAN_INTERVAL;
    }

    public void scanned(long time) {
        scannedAt = time;
    }

    public String describe() {
        return area.describe();
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("id", id);
        tag.setInteger("beds", beds);
        area.writeToNBT(tag);
        return tag;
    }

    public static ColonyHome readFromNBT(NBTTagCompound tag) {
        ColonyHome home = new ColonyHome();
        home.id = tag.getInteger("id");
        home.beds = tag.getInteger("beds");
        home.area.readFromNBT(tag);
        return home;
    }
}
