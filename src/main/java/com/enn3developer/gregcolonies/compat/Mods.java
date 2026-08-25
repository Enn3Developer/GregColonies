package com.enn3developer.gregcolonies.compat;

import cpw.mods.fml.common.Loader;

public final class Mods {

    public static final String TINKERS_ID = "TConstruct";

    private static Boolean tinkers;

    private Mods() {}

    public static boolean tinkers() {
        if (tinkers == null) {
            tinkers = Loader.isModLoaded(TINKERS_ID);
        }
        return tinkers;
    }
}
