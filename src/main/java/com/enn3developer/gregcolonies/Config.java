package com.enn3developer.gregcolonies;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";
    public static int minColonyDistance = 128;
    public static int colonyRadius = 64;

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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
