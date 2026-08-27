package com.enn3developer.gregcolonies.colony;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class Blueprint {

    public static final int MAX_SIDE = 32;

    public static final int MAX_VOLUME = 8192;

    public static final int MAX_NAME_LENGTH = 24;

    public static final int ROTATIONS = 4;

    public static final int AIR = 0;

    private static final int META_BITS = 4;

    private static final int META_MASK = 0xF;

    private static final int VAR_INT_BYTES = 5;

    private String name = "";
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private final List<String> palette = new ArrayList<>();
    private int[] cells = new int[0];

    private Blueprint() {}

    public static Blueprint capture(World world, String name, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.min(Math.max(x1, x2), minX + MAX_SIDE - 1);
        int maxZ = Math.min(Math.max(z1, z2), minZ + MAX_SIDE - 1);

        Blueprint blueprint = new Blueprint();
        blueprint.name = cleanName(name);
        blueprint.sizeX = maxX - minX + 1;
        blueprint.sizeZ = maxZ - minZ + 1;

        int layers = Math.max(1, Math.min(MAX_SIDE, MAX_VOLUME / (blueprint.sizeX * blueprint.sizeZ)));
        int minY = Math.max(0, Math.min(y1, y2));
        int maxY = Math.min(Math.min(Math.max(y1, y2), minY + layers - 1), world.getHeight() - 1);
        if (maxY < minY) {
            return null;
        }
        blueprint.sizeY = maxY - minY + 1;
        blueprint.palette.add("");
        blueprint.cells = new int[blueprint.volume()];

        for (int y = 0; y < blueprint.sizeY; y++) {
            for (int z = 0; z < blueprint.sizeZ; z++) {
                for (int x = 0; x < blueprint.sizeX; x++) {
                    blueprint.cells[blueprint.index(x, y, z)] = blueprint.encode(world, minX + x, minY + y, minZ + z);
                }
            }
        }
        return blueprint.trimmed();
    }

    public static String cleanName(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < name.length() && cleaned.length() < MAX_NAME_LENGTH; i++) {
            char letter = name.charAt(i);
            if (letter >= ' ' && letter != 127) {
                cleaned.append(letter);
            }
        }
        return cleaned.toString()
            .trim();
    }

    private Blueprint trimmed() {
        int lowX = sizeX;
        int lowY = sizeY;
        int lowZ = sizeZ;
        int highX = -1;
        int highY = -1;
        int highZ = -1;
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    if (cells[index(x, y, z)] == AIR) {
                        continue;
                    }
                    lowX = Math.min(lowX, x);
                    lowY = Math.min(lowY, y);
                    lowZ = Math.min(lowZ, z);
                    highX = Math.max(highX, x);
                    highY = Math.max(highY, y);
                    highZ = Math.max(highZ, z);
                }
            }
        }
        if (highX < 0) {
            return null;
        }
        if (lowX == 0 && lowY == 0 && lowZ == 0 && highX == sizeX - 1 && highY == sizeY - 1 && highZ == sizeZ - 1) {
            return this;
        }
        Blueprint trimmed = new Blueprint();
        trimmed.name = name;
        trimmed.sizeX = highX - lowX + 1;
        trimmed.sizeY = highY - lowY + 1;
        trimmed.sizeZ = highZ - lowZ + 1;
        trimmed.palette.addAll(palette);
        trimmed.cells = new int[trimmed.volume()];
        for (int y = 0; y < trimmed.sizeY; y++) {
            for (int z = 0; z < trimmed.sizeZ; z++) {
                for (int x = 0; x < trimmed.sizeX; x++) {
                    trimmed.cells[trimmed.index(x, y, z)] = cells[index(lowX + x, lowY + y, lowZ + z)];
                }
            }
        }
        return trimmed;
    }

    public Blueprint transformed(int rotation, boolean mirror) {
        int steps = ((rotation % ROTATIONS) + ROTATIONS) % ROTATIONS;
        if (steps == 0 && !mirror) {
            return this;
        }
        Blueprint placed = new Blueprint();
        placed.name = name;
        placed.sizeY = sizeY;
        placed.sizeX = steps % 2 == 0 ? sizeX : sizeZ;
        placed.sizeZ = steps % 2 == 0 ? sizeZ : sizeX;
        placed.palette.addAll(palette);
        placed.cells = new int[placed.volume()];
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    int sourceX = mirror ? sizeX - 1 - x : x;
                    int targetX;
                    int targetZ;
                    if (steps == 1) {
                        targetX = sizeZ - 1 - z;
                        targetZ = sourceX;
                    } else if (steps == 2) {
                        targetX = sizeX - 1 - sourceX;
                        targetZ = sizeZ - 1 - z;
                    } else if (steps == 3) {
                        targetX = z;
                        targetZ = sizeX - 1 - sourceX;
                    } else {
                        targetX = sourceX;
                        targetZ = z;
                    }
                    placed.cells[placed.index(targetX, y, targetZ)] = cells[index(x, y, z)];
                }
            }
        }
        return placed;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = cleanName(name);
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

    public ItemStack stackOf(int cell) {
        Block block = blockOf(cell);
        if (block == null) {
            return null;
        }
        Item item = Item.getItemFromBlock(block);
        return item == null ? null : new ItemStack(item, 1, metaOf(cell));
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

    public Map<Integer, Integer> materials() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int cell : cells) {
            if (cell != AIR) {
                counts.merge(cell, 1, Integer::sum);
            }
        }
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(
            Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue)
                .reversed());
        Map<Integer, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    public boolean matches(int cell, ItemStack stack) {
        Block block = blockOf(cell);
        return block != null && stack != null
            && Block.getBlockFromItem(stack.getItem()) == block
            && stack.getItemDamage() == metaOf(cell);
    }

    public void write(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeShort(sizeX);
        buf.writeShort(sizeY);
        buf.writeShort(sizeZ);
        buf.writeShort(palette.size());
        for (String entry : palette) {
            ByteBufUtils.writeUTF8String(buf, entry);
        }
        ByteBufUtils.writeVarInt(buf, cells.length, VAR_INT_BYTES);
        int at = 0;
        while (at < cells.length) {
            int value = cells[at];
            int run = 1;
            while (at + run < cells.length && cells[at + run] == value) {
                run++;
            }
            ByteBufUtils.writeVarInt(buf, run, VAR_INT_BYTES);
            ByteBufUtils.writeVarInt(buf, value, VAR_INT_BYTES);
            at += run;
        }
    }

    public static Blueprint read(ByteBuf buf) {
        Blueprint blueprint = new Blueprint();
        blueprint.name = ByteBufUtils.readUTF8String(buf);
        blueprint.sizeX = buf.readShort();
        blueprint.sizeY = buf.readShort();
        blueprint.sizeZ = buf.readShort();
        int slots = buf.readShort();
        for (int i = 0; i < slots; i++) {
            blueprint.palette.add(ByteBufUtils.readUTF8String(buf));
        }
        int length = ByteBufUtils.readVarInt(buf, VAR_INT_BYTES);
        if (length <= 0 || length > MAX_VOLUME || length != blueprint.volume()) {
            return null;
        }
        blueprint.cells = new int[length];
        int at = 0;
        while (at < length) {
            int run = ByteBufUtils.readVarInt(buf, VAR_INT_BYTES);
            int value = ByteBufUtils.readVarInt(buf, VAR_INT_BYTES);
            if (run <= 0 || at + run > length) {
                return null;
            }
            for (int i = 0; i < run; i++) {
                blueprint.cells[at++] = value;
            }
        }
        return blueprint;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", name);
        tag.setInteger("sx", sizeX);
        tag.setInteger("sy", sizeY);
        tag.setInteger("sz", sizeZ);
        NBTTagList names = new NBTTagList();
        for (String entry : palette) {
            names.appendTag(new NBTTagString(entry));
        }
        tag.setTag("palette", names);
        tag.setIntArray("cells", cells);
        return tag;
    }

    public static Blueprint readFromNBT(NBTTagCompound tag) {
        Blueprint blueprint = new Blueprint();
        blueprint.name = cleanName(tag.getString("name"));
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
