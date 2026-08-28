package com.enn3developer.gregcolonies.client.gui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

public final class BlueprintMesh {

    public static final int[][] OFFSETS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };

    public static final int SIDES = OFFSETS.length;

    private static final float[] SHADE = { 0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F };

    private static final int META_SHIFT = 3;

    private static final int ID_SHIFT = 12;

    private static final Map<Integer, IIcon> ICONS = new HashMap<>();

    private BlueprintMesh() {}

    public static void forget() {
        ICONS.clear();
    }

    public static void face(Tessellator tessellator, Block block, int meta, int side, double x, double y, double z,
        float alpha) {
        IIcon icon = iconOf(block, meta, side);
        if (icon == null) {
            return;
        }
        int tint = renderColor(block, meta);
        float shade = SHADE[side];
        tessellator.setColorRGBA_F(
            (tint >> 16 & 0xFF) / 255.0F * shade,
            (tint >> 8 & 0xFF) / 255.0F * shade,
            (tint & 0xFF) / 255.0F * shade,
            alpha);
        double minU = icon.getMinU();
        double maxU = icon.getMaxU();
        double minV = icon.getMinV();
        double maxV = icon.getMaxV();
        double x1 = x + 1.0D;
        double y1 = y + 1.0D;
        double z1 = z + 1.0D;

        if (side == 0) {
            tessellator.addVertexWithUV(x, y, z, minU, minV);
            tessellator.addVertexWithUV(x, y, z1, minU, maxV);
            tessellator.addVertexWithUV(x1, y, z1, maxU, maxV);
            tessellator.addVertexWithUV(x1, y, z, maxU, minV);
        } else if (side == 1) {
            tessellator.addVertexWithUV(x, y1, z, minU, minV);
            tessellator.addVertexWithUV(x1, y1, z, maxU, minV);
            tessellator.addVertexWithUV(x1, y1, z1, maxU, maxV);
            tessellator.addVertexWithUV(x, y1, z1, minU, maxV);
        } else if (side == 2) {
            tessellator.addVertexWithUV(x, y, z, maxU, maxV);
            tessellator.addVertexWithUV(x1, y, z, minU, maxV);
            tessellator.addVertexWithUV(x1, y1, z, minU, minV);
            tessellator.addVertexWithUV(x, y1, z, maxU, minV);
        } else if (side == 3) {
            tessellator.addVertexWithUV(x, y, z1, minU, maxV);
            tessellator.addVertexWithUV(x, y1, z1, minU, minV);
            tessellator.addVertexWithUV(x1, y1, z1, maxU, minV);
            tessellator.addVertexWithUV(x1, y, z1, maxU, maxV);
        } else if (side == 4) {
            tessellator.addVertexWithUV(x, y, z, minU, maxV);
            tessellator.addVertexWithUV(x, y1, z, minU, minV);
            tessellator.addVertexWithUV(x, y1, z1, maxU, minV);
            tessellator.addVertexWithUV(x, y, z1, maxU, maxV);
        } else {
            tessellator.addVertexWithUV(x1, y, z, maxU, maxV);
            tessellator.addVertexWithUV(x1, y, z1, minU, maxV);
            tessellator.addVertexWithUV(x1, y1, z1, minU, minV);
            tessellator.addVertexWithUV(x1, y1, z, maxU, minV);
        }
    }

    private static IIcon iconOf(Block block, int meta, int side) {
        int key = Block.getIdFromBlock(block) << ID_SHIFT | (meta & 0xFF) << META_SHIFT | side;
        IIcon cached = ICONS.get(key);
        if (cached == null) {
            cached = lookup(block, meta, side);
            ICONS.put(key, cached);
        }
        return cached;
    }

    private static IIcon lookup(Block block, int meta, int side) {
        try {
            IIcon icon = block.getIcon(side, meta);
            return icon != null ? icon : Blocks.stone.getIcon(side, 0);
        } catch (RuntimeException error) {
            return Blocks.stone.getIcon(side, 0);
        }
    }

    private static int renderColor(Block block, int meta) {
        try {
            return block.getRenderColor(meta);
        } catch (RuntimeException error) {
            return 0xFFFFFF;
        }
    }
}
