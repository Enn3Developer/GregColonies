package com.enn3developer.gregcolonies;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";
    public static int minColonyDistance = 128;
    public static int colonyRadius = 64;
    public static int birthChance = 200000;
    public static int childGrowthTicks = 24000;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        minColonyDistance = configuration.getInt(
            "minColonyDistance",
            Configuration.CATEGORY_GENERAL,
            minColonyDistance,
            16,
            4096,
            "Minimum distance in blocks between two colony centers");

        colonyRadius = configuration.getInt(
            "colonyRadius",
            Configuration.CATEGORY_GENERAL,
            colonyRadius,
            8,
            1024,
            "Radius in blocks of the region a colony owns and guards");

        birthChance = configuration.getInt(
            "birthChance",
            Configuration.CATEGORY_GENERAL,
            birthChance,
            1,
            100000000,
            "One in this many chances for an idle woman to look for a partner, higher means rarer births");

        childGrowthTicks = configuration.getInt(
            "childGrowthTicks",
            Configuration.CATEGORY_GENERAL,
            childGrowthTicks,
            20,
            10000000,
            "Ticks a newborn citizen stays a child before growing up and being able to work");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
