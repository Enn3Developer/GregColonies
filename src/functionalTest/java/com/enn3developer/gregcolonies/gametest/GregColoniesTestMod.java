package com.enn3developer.gregcolonies.gametest;

import cpw.mods.fml.common.Mod;

@Mod(
    modid = GregColoniesTestMod.MODID,
    name = "GregColonies Game Tests",
    version = "1.0",
    dependencies = "required-after:gregcolonies;required-after:horizonqa",
    acceptableRemoteVersions = "*")
public class GregColoniesTestMod {

    public static final String MODID = "gregcolonies_tests";
}
