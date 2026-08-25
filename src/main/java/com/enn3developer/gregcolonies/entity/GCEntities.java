package com.enn3developer.gregcolonies.entity;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandRegistry;

import cpw.mods.fml.common.registry.EntityRegistry;

public final class GCEntities {

    private GCEntities() {}

    public static void register() {
        CitizenCommandRegistry.registerDefaults();
        EntityRegistry
            .registerModEntity(EntityCitizen.class, EntityCitizen.NAME, 0, GregColonies.instance, 64, 3, true);
    }
}
