package com.enn3developer.gregcolonies.block;

import com.enn3developer.gregcolonies.GregColonies;

import cpw.mods.fml.common.registry.GameRegistry;

public final class GCBlocks {

    public static BlockColonyCore colonyCore;

    private GCBlocks() {}

    public static void register() {
        colonyCore = new BlockColonyCore();
        GameRegistry.registerBlock(colonyCore, BlockColonyCore.NAME);
        GameRegistry.registerTileEntity(TileColonyCore.class, GregColonies.MODID + ":" + BlockColonyCore.NAME);
    }
}
