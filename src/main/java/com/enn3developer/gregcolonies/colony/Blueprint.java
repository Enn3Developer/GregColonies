package com.enn3developer.gregcolonies.colony;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class Blueprint {

    public static final int MAX_SIDE = 32;

    public static final int MAX_VOLUME = MAX_SIDE * MAX_SIDE * MAX_SIDE;

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
    private int originX;
    private int originY;
    private int originZ;

    private Blueprint() {}

    public static Blueprint empty(String name, int sizeX, int sizeY, int sizeZ) {
        if (!fits(sizeX, sizeY, sizeZ)) {
            return null;
        }
        Blueprint blueprint = new Blueprint();
        blueprint.name = cleanName(name);
        blueprint.sizeX = sizeX;
        blueprint.sizeY = sizeY;
        blueprint.sizeZ = sizeZ;
        blueprint.palette.add("");
        blueprint.cells = new int[blueprint.volume()];
        blueprint.centreOrigin();
        return blueprint;
    }

    public static boolean fits(int sizeX, int sizeY, int sizeZ) {
        return sizeX >= 1 && sizeY >= 1 && sizeZ >= 1 && sizeX <= MAX_SIDE && sizeY <= MAX_SIDE && sizeZ <= MAX_SIDE;
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

    public Blueprint trimmed() {
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
        trimmed.originX = originX - lowX;
        trimmed.originY = originY - lowY;
        trimmed.originZ = originZ - lowZ;
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
        trimmed.clampOrigin();
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
        int[] target = new int[2];
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    map(x, z, steps, mirror, target);
                    placed.cells[placed
                        .index(target[0], y, target[1])] = rotateCell(cells[index(x, y, z)], steps, mirror);
                }
            }
        }
        map(originX, originZ, steps, mirror, target);
        placed.originX = target[0];
        placed.originY = originY;
        placed.originZ = target[1];
        placed.clampOrigin();
        return placed;
    }

    private void map(int x, int z, int steps, boolean mirror, int[] target) {
        int sourceX = mirror ? sizeX - 1 - x : x;
        if (steps == 1) {
            target[0] = sizeZ - 1 - z;
            target[1] = sourceX;
        } else if (steps == 2) {
            target[0] = sizeX - 1 - sourceX;
            target[1] = sizeZ - 1 - z;
        } else if (steps == 3) {
            target[0] = z;
            target[1] = sizeX - 1 - sourceX;
        } else {
            target[0] = sourceX;
            target[1] = z;
        }
    }

    public static boolean isBuildable(Block block, int meta) {
        if (block == null || block == Blocks.air) {
            return false;
        }
        if (block.getMaterial()
            .isLiquid() || block.hasTileEntity(meta)) {
            return false;
        }
        return Item.getItemFromBlock(block) != null;
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
        return item == null ? null : new ItemStack(item, 1, itemMeta(block, metaOf(cell)));
    }

    private int rotateCell(int cell, int steps, boolean mirror) {
        Block block = blockOf(cell);
        if (block == null) {
            return cell;
        }
        int meta = BlockRotation.transform(block, metaOf(cell), steps, mirror) & META_MASK;
        return (cell & ~META_MASK) | meta;
    }

    public int itemCell(int cell) {
        Block block = blockOf(cell);
        if (block == null) {
            return cell;
        }
        return (cell & ~META_MASK) | itemMeta(block, metaOf(cell));
    }

    private static int itemMeta(Block block, int meta) {
        return block.damageDropped(meta) & META_MASK;
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
                counts.merge(itemCell(cell), 1, Integer::sum);
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
            && stack.getItemDamage() == itemMeta(block, metaOf(cell));
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public void setOrigin(int x, int y, int z) {
        originX = x;
        originY = y;
        originZ = z;
        clampOrigin();
    }

    private void centreOrigin() {
        originX = sizeX / 2;
        originY = 0;
        originZ = sizeZ / 2;
    }

    private void clampOrigin() {
        originX = clamp(originX, sizeX);
        originY = clamp(originY, sizeY);
        originZ = clamp(originZ, sizeZ);
    }

    private static int clamp(int value, int size) {
        return value < 0 ? 0 : Math.min(value, size - 1);
    }

    public Blueprint copy() {
        Blueprint clone = new Blueprint();
        clone.name = name;
        clone.sizeX = sizeX;
        clone.sizeY = sizeY;
        clone.sizeZ = sizeZ;
        clone.palette.addAll(palette);
        clone.cells = cells.clone();
        clone.originX = originX;
        clone.originY = originY;
        clone.originZ = originZ;
        return clone;
    }

    public Blueprint resized(int sizeX, int sizeY, int sizeZ, int shiftX, int shiftY, int shiftZ) {
        if (!fits(sizeX, sizeY, sizeZ)) {
            return null;
        }
        Blueprint resized = new Blueprint();
        resized.name = name;
        resized.sizeX = sizeX;
        resized.sizeY = sizeY;
        resized.sizeZ = sizeZ;
        resized.palette.addAll(palette);
        resized.cells = new int[resized.volume()];
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    resized.cells[resized.index(x, y, z)] = cellAt(x - shiftX, y - shiftY, z - shiftZ);
                }
            }
        }
        resized.originX = originX + shiftX;
        resized.originY = originY + shiftY;
        resized.originZ = originZ + shiftZ;
        resized.clampOrigin();
        return resized;
    }

    public int cellFor(Block block, int meta) {
        if (!isBuildable(block, meta)) {
            return AIR;
        }
        String key = Block.blockRegistry.getNameForObject(block);
        if (key == null || key.isEmpty()) {
            return AIR;
        }
        int slot = palette.indexOf(key);
        if (slot < 0) {
            slot = palette.size();
            palette.add(key);
        }
        return (slot << META_BITS) | (meta & META_MASK);
    }

    public int cellFor(ItemStack stack) {
        if (stack == null) {
            return AIR;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == null ? AIR : cellFor(block, stack.getItemDamage());
    }

    public void setCell(int x, int y, int z, int cell) {
        if (contains(x, y, z)) {
            cells[index(x, y, z)] = cell;
        }
    }

    public int adopt(Blueprint other, int cell) {
        return cell == AIR ? AIR : cellFor(other.blockOf(cell), metaOf(cell));
    }

    public boolean isPlaceable() {
        for (int cell : cells) {
            if (cell != AIR && !isBuildable(blockOf(cell), metaOf(cell))) {
                return false;
            }
        }
        return true;
    }

    public void write(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeShort(sizeX);
        buf.writeShort(sizeY);
        buf.writeShort(sizeZ);
        buf.writeShort(originX);
        buf.writeShort(originY);
        buf.writeShort(originZ);
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
        blueprint.name = cleanName(ByteBufUtils.readUTF8String(buf));
        blueprint.sizeX = buf.readShort();
        blueprint.sizeY = buf.readShort();
        blueprint.sizeZ = buf.readShort();
        blueprint.originX = buf.readShort();
        blueprint.originY = buf.readShort();
        blueprint.originZ = buf.readShort();
        int slots = buf.readShort();
        if (!fits(blueprint.sizeX, blueprint.sizeY, blueprint.sizeZ) || slots < 1 || slots > MAX_VOLUME) {
            return null;
        }
        for (int i = 0; i < slots; i++) {
            blueprint.palette.add(ByteBufUtils.readUTF8String(buf));
        }
        int length = ByteBufUtils.readVarInt(buf, VAR_INT_BYTES);
        if (length != blueprint.volume()) {
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
        blueprint.clampOrigin();
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
        tag.setInteger("ox", originX);
        tag.setInteger("oy", originY);
        tag.setInteger("oz", originZ);
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
        if (tag.hasKey("ox")) {
            blueprint.originX = tag.getInteger("ox");
            blueprint.originY = tag.getInteger("oy");
            blueprint.originZ = tag.getInteger("oz");
            blueprint.clampOrigin();
        } else {
            blueprint.centreOrigin();
        }
        return blueprint;
    }
}
