package com.enn3developer.gregcolonies.colony;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class BuildSite {

    private int x;
    private int y;
    private int z;
    private int rotation;
    private boolean mirror;
    private Blueprint blueprint;
    private final List<int[]> scaffolds = new ArrayList<>();

    private BuildSite() {}

    public BuildSite(int x, int y, int z, Blueprint blueprint, int rotation, boolean mirror) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = ((rotation % Blueprint.ROTATIONS) + Blueprint.ROTATIONS) % Blueprint.ROTATIONS;
        this.mirror = mirror;
        this.blueprint = blueprint.transformed(this.rotation, mirror);
    }

    public int getRotation() {
        return rotation;
    }

    public boolean isMirrored() {
        return mirror;
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

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public boolean contains(int worldX, int worldY, int worldZ) {
        return blueprint.contains(worldX - x, worldY - y, worldZ - z);
    }

    public boolean hasScaffolds() {
        return !scaffolds.isEmpty();
    }

    public boolean isScaffoldAt(int worldX, int worldY, int worldZ) {
        return indexOfScaffold(worldX, worldY, worldZ) >= 0;
    }

    public void addScaffold(int worldX, int worldY, int worldZ) {
        if (!isScaffoldAt(worldX, worldY, worldZ)) {
            scaffolds.add(new int[] { worldX, worldY, worldZ });
        }
    }

    public void removeScaffold(int worldX, int worldY, int worldZ) {
        int index = indexOfScaffold(worldX, worldY, worldZ);
        if (index >= 0) {
            scaffolds.remove(index);
        }
    }

    public int[] topScaffold() {
        int[] top = null;
        for (int[] scaffold : scaffolds) {
            if (top == null || scaffold[1] > top[1]) {
                top = scaffold;
            }
        }
        return top;
    }

    private int indexOfScaffold(int worldX, int worldY, int worldZ) {
        for (int i = 0; i < scaffolds.size(); i++) {
            int[] scaffold = scaffolds.get(i);
            if (scaffold[0] == worldX && scaffold[1] == worldY && scaffold[2] == worldZ) {
                return i;
            }
        }
        return -1;
    }

    public boolean isAt(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public int cellFor(int worldX, int worldY, int worldZ) {
        return blueprint.cellAt(worldX - x, worldY - y, worldZ - z);
    }

    public boolean isPlaced(World world, int worldX, int worldY, int worldZ) {
        int cell = cellFor(worldX, worldY, worldZ);
        if (cell == Blueprint.AIR) {
            return true;
        }
        return world.getBlock(worldX, worldY, worldZ) == blueprint.blockOf(cell)
            && world.getBlockMetadata(worldX, worldY, worldZ) == Blueprint.metaOf(cell);
    }

    public boolean isFree(World world, int worldX, int worldY, int worldZ) {
        Block block = world.getBlock(worldX, worldY, worldZ);
        return block == null || block.isAir(world, worldX, worldY, worldZ)
            || block.getMaterial()
                .isReplaceable();
    }

    public boolean needsStack(World world, ItemStack stack, int worldX, int worldY, int worldZ) {
        int cell = cellFor(worldX, worldY, worldZ);
        return cell != Blueprint.AIR && blueprint.matches(cell, stack)
            && !isPlaced(world, worldX, worldY, worldZ)
            && isFree(world, worldX, worldY, worldZ);
    }

    public int remaining(World world) {
        int count = 0;
        for (int dy = 0; dy < blueprint.getSizeY(); dy++) {
            for (int dz = 0; dz < blueprint.getSizeZ(); dz++) {
                for (int dx = 0; dx < blueprint.getSizeX(); dx++) {
                    if (blueprint.cellAt(dx, dy, dz) != Blueprint.AIR && !isPlaced(world, x + dx, y + dy, z + dz)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int total() {
        return blueprint.blockCount();
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setInteger("rotation", rotation);
        tag.setBoolean("mirror", mirror);
        tag.setTag("blueprint", blueprint.writeToNBT());
        int[] packed = new int[scaffolds.size() * 3];
        for (int i = 0; i < scaffolds.size(); i++) {
            int[] scaffold = scaffolds.get(i);
            packed[i * 3] = scaffold[0];
            packed[i * 3 + 1] = scaffold[1];
            packed[i * 3 + 2] = scaffold[2];
        }
        tag.setIntArray("scaffolds", packed);
        return tag;
    }

    public static BuildSite readFromNBT(NBTTagCompound tag) {
        Blueprint blueprint = Blueprint.readFromNBT(tag.getCompoundTag("blueprint"));
        if (blueprint == null) {
            return null;
        }
        BuildSite site = new BuildSite();
        site.x = tag.getInteger("x");
        site.y = tag.getInteger("y");
        site.z = tag.getInteger("z");
        site.rotation = tag.getInteger("rotation");
        site.mirror = tag.getBoolean("mirror");
        site.blueprint = blueprint;
        int[] packed = tag.getIntArray("scaffolds");
        for (int i = 0; i + 2 < packed.length; i += 3) {
            site.scaffolds.add(new int[] { packed[i], packed[i + 1], packed[i + 2] });
        }
        return site;
    }
}
