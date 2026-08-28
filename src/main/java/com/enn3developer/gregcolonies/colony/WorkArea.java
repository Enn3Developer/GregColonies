package com.enn3developer.gregcolonies.colony;

import net.minecraft.nbt.NBTTagCompound;

public class WorkArea {

    public static final int MAX_SIDE = 32;

    public static final int MAX_HEIGHT = 32;

    private int minX;

    private int minY;

    private int minZ;

    private int maxX;

    private int maxY;

    private int maxZ;

    public WorkArea() {}

    public WorkArea(int x1, int y1, int z1, int x2, int y2, int z2) {
        set(x1, y1, z1, x2, y2, z2);
    }

    public void set(int x1, int y1, int z1, int x2, int y2, int z2) {
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);
    }

    public void capHeight(int height) {
        maxY = Math.min(maxY, minY + height - 1);
    }

    public void capSide(int side) {
        maxX = Math.min(maxX, minX + side - 1);
        maxZ = Math.min(maxZ, minZ + side - 1);
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getCenterX() {
        return (minX + maxX) / 2;
    }

    public int getCenterZ() {
        return (minZ + maxZ) / 2;
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("x1", minX);
        tag.setInteger("y1", minY);
        tag.setInteger("z1", minZ);
        tag.setInteger("x2", maxX);
        tag.setInteger("y2", maxY);
        tag.setInteger("z2", maxZ);
    }

    public void readFromNBT(NBTTagCompound tag) {
        minX = tag.getInteger("x1");
        minY = tag.getInteger("y1");
        minZ = tag.getInteger("z1");
        maxX = tag.getInteger("x2");
        maxY = tag.getInteger("y2");
        maxZ = tag.getInteger("z2");
    }
}
