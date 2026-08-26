package com.enn3developer.gregcolonies.compat;

import cpw.mods.fml.common.Loader;

public final class Mods {

    public static final String TINKERS_ID = "TConstruct";

    public static final String OPEN_BLOCKS_ID = "OpenBlocks";

    private static Boolean tinkers;

    private static Boolean openBlocks;

    private Mods() {}

    public static boolean tinkers() {
        if (tinkers == null) {
            tinkers = Loader.isModLoaded(TINKERS_ID);
        }
        return tinkers;
    }

    public static boolean openBlocks() {
        if (openBlocks == null) {
            openBlocks = Loader.isModLoaded(OPEN_BLOCKS_ID);
        }
        return openBlocks;
    }
}
