package com.enn3developer.gregcolonies.colony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BlockPalette {

    private final List<String> names = new ArrayList<>();

    public int size() {
        return names.size();
    }

    public void add(String name) {
        names.add(name);
    }

    public void copyFrom(BlockPalette other) {
        names.addAll(other.names);
    }

    public List<String> names() {
        return Collections.unmodifiableList(names);
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

    public Block blockOf(int cell) {
        if (cell == Blueprint.AIR) {
            return null;
        }
        int slot = Blueprint.slotOf(cell);
        if (slot < 1 || slot >= names.size()) {
            return null;
        }
        return (Block) Block.blockRegistry.getObject(names.get(slot));
    }

    public ItemStack stackOf(int cell) {
        Block block = blockOf(cell);
        if (block == null) {
            return null;
        }
        Item item = Item.getItemFromBlock(block);
        return item == null ? null : new ItemStack(item, 1, itemMeta(block, Blueprint.metaOf(cell)));
    }

    public boolean matches(int cell, ItemStack stack) {
        Block block = blockOf(cell);
        return block != null && stack != null
            && Block.getBlockFromItem(stack.getItem()) == block
            && stack.getItemDamage() == itemMeta(block, Blueprint.metaOf(cell));
    }

    public int itemCell(int cell) {
        Block block = blockOf(cell);
        if (block == null) {
            return cell;
        }
        return Blueprint.cell(Blueprint.slotOf(cell), itemMeta(block, Blueprint.metaOf(cell)));
    }

    public int rotate(int cell, int steps, boolean mirror) {
        Block block = blockOf(cell);
        if (block == null) {
            return cell;
        }
        int meta = BlockRotation.transform(block, Blueprint.metaOf(cell), steps, mirror);
        return Blueprint.cell(Blueprint.slotOf(cell), meta);
    }

    public boolean isPlaceable(int cell) {
        return isBuildable(blockOf(cell), Blueprint.metaOf(cell));
    }

    public int cellFor(Block block, int meta) {
        if (!isBuildable(block, meta)) {
            return Blueprint.AIR;
        }
        String key = Block.blockRegistry.getNameForObject(block);
        if (key == null || key.isEmpty()) {
            return Blueprint.AIR;
        }
        int slot = names.indexOf(key);
        if (slot < 0) {
            slot = names.size();
            names.add(key);
        }
        return Blueprint.cell(slot, meta);
    }

    public int cellFor(ItemStack stack) {
        if (stack == null) {
            return Blueprint.AIR;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == null ? Blueprint.AIR : cellFor(block, stack.getItemDamage());
    }

    public int adopt(BlockPalette other, int cell) {
        return cell == Blueprint.AIR ? Blueprint.AIR : cellFor(other.blockOf(cell), Blueprint.metaOf(cell));
    }

    private static int itemMeta(Block block, int meta) {
        return block.damageDropped(meta) & Blueprint.META_MASK;
    }
}
