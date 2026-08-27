package com.enn3developer.gregcolonies.colony;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class BuildSite {

    private int x;
    private int y;
    private int z;
    private Blueprint blueprint;

    private BuildSite() {}

    public BuildSite(int x, int y, int z, Blueprint blueprint) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blueprint = blueprint;
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
        tag.setTag("blueprint", blueprint.writeToNBT());
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
        site.blueprint = blueprint;
        return site;
    }
}
