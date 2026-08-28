package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.enn3developer.gregcolonies.colony.BlockPalette;

public class BlueprintBrush {

    private final Block block;

    private final int meta;

    private final int held;

    private BlueprintBrush(Block block, int meta, int held) {
        this.block = block;
        this.meta = meta;
        this.held = held;
    }

    public static BlueprintBrush of(String name, int meta, int held) {
        Block block = (Block) Block.blockRegistry.getObject(name);
        return of(block, meta, held);
    }

    public static BlueprintBrush of(Block block, int meta, int held) {
        return BlockPalette.isBuildable(block, meta) ? new BlueprintBrush(block, meta, held) : null;
    }

    public Block getBlock() {
        return block;
    }

    public int getMeta() {
        return meta;
    }

    public int getHeld() {
        return held;
    }

    public boolean is(Block block, int meta) {
        return this.block == block && this.meta == meta;
    }

    public ItemStack stack() {
        Item item = Item.getItemFromBlock(block);
        return item == null ? null : new ItemStack(item, 1, meta);
    }

    public String label() {
        ItemStack stack = stack();
        if (stack == null) {
            return "unknown block";
        }
        try {
            return stack.getDisplayName();
        } catch (RuntimeException error) {
            return "unknown block";
        }
    }
}
