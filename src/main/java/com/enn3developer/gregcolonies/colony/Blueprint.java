package com.enn3developer.gregcolonies.colony;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class Blueprint {

    public static final int MAX_SIDE = 32;

    public static final int MAX_VOLUME = 8192;

    public static final int AIR = 0;

    private static final int META_BITS = 4;

    private static final int META_MASK = 0xF;

    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private final List<String> palette = new ArrayList<>();
    private int[] cells = new int[0];

    private Blueprint() {}

    public static Blueprint capture(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.min(Math.max(x1, x2), minX + MAX_SIDE - 1);
        int maxZ = Math.min(Math.max(z1, z2), minZ + MAX_SIDE - 1);

        Blueprint blueprint = new Blueprint();
        blueprint.sizeX = maxX - minX + 1;
        blueprint.sizeZ = maxZ - minZ + 1;
        int layers = Math.max(1, Math.min(MAX_SIDE, MAX_VOLUME / (blueprint.sizeX * blueprint.sizeZ)));
        int maxY = Math.min(Math.min(Math.max(y1, y2), minY + layers - 1), world.getHeight() - 1);
        blueprint.sizeY = maxY - minY + 1;
        blueprint.palette.add("");
        blueprint.cells = new int[blueprint.volume()];

        int top = -1;
        for (int y = 0; y < blueprint.sizeY; y++) {
            for (int z = 0; z < blueprint.sizeZ; z++) {
                for (int x = 0; x < blueprint.sizeX; x++) {
                    int cell = blueprint.encode(world, minX + x, minY + y, minZ + z);
                    blueprint.cells[blueprint.index(x, y, z)] = cell;
                    if (cell != AIR) {
                        top = y;
                    }
                }
            }
        }
        if (top < 0) {
            return null;
        }
        blueprint.sizeY = top + 1;
        blueprint.cells = Arrays.copyOf(blueprint.cells, blueprint.volume());
        return blueprint;
    }

    private int encode(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            return AIR;
        }
        ItemStack stack = new ItemStack(block, 1, world.getBlockMetadata(x, y, z));
        if (!WorkBlocks.isScaffold(stack)) {
            return AIR;
        }
        String name = Block.blockRegistry.getNameForObject(block);
        if (name == null || name.isEmpty()) {
            return AIR;
        }
        int slot = palette.indexOf(name);
        if (slot < 0) {
            slot = palette.size();
            palette.add(name);
        }
        return (slot << META_BITS) | (world.getBlockMetadata(x, y, z) & META_MASK);
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public int volume() {
        return sizeX * sizeY * sizeZ;
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    public boolean contains(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public int cellAt(int x, int y, int z) {
        return contains(x, y, z) ? cells[index(x, y, z)] : AIR;
    }

    public Block blockOf(int cell) {
        if (cell == AIR) {
            return null;
        }
        int slot = cell >> META_BITS;
        if (slot < 1 || slot >= palette.size()) {
            return null;
        }
        return (Block) Block.blockRegistry.getObject(palette.get(slot));
    }

    public static int metaOf(int cell) {
        return cell & META_MASK;
    }

    public int blockCount() {
        int count = 0;
        for (int cell : cells) {
            if (cell != AIR) {
                count++;
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return blockCount() == 0;
    }

    public boolean matches(int cell, ItemStack stack) {
        Block block = blockOf(cell);
        return block != null && stack != null
            && Block.getBlockFromItem(stack.getItem()) == block
            && stack.getItemDamage() == metaOf(cell);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("sx", sizeX);
        tag.setInteger("sy", sizeY);
        tag.setInteger("sz", sizeZ);
        NBTTagList names = new NBTTagList();
        for (String name : palette) {
            names.appendTag(new NBTTagString(name));
        }
        tag.setTag("palette", names);
        tag.setIntArray("cells", cells);
        return tag;
    }

    public static Blueprint readFromNBT(NBTTagCompound tag) {
        Blueprint blueprint = new Blueprint();
        blueprint.sizeX = tag.getInteger("sx");
        blueprint.sizeY = tag.getInteger("sy");
        blueprint.sizeZ = tag.getInteger("sz");
        NBTTagList names = tag.getTagList("palette", 8);
        for (int i = 0; i < names.tagCount(); i++) {
            blueprint.palette.add(names.getStringTagAt(i));
        }
        blueprint.cells = tag.getIntArray("cells");
        if (blueprint.volume() <= 0 || blueprint.cells.length != blueprint.volume()) {
            return null;
        }
        return blueprint;
    }
}
