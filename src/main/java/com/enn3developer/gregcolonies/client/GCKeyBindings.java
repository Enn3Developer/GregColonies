package com.enn3developer.gregcolonies.client;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class GCKeyBindings {

    public static final String CATEGORY = "key.categories.gregcolonies";

    public static KeyBinding openColony;

    private GCKeyBindings() {}

    public static void register() {
        openColony = new KeyBinding("key.gregcolonies.colony", Keyboard.KEY_PERIOD, CATEGORY);
        ClientRegistry.registerKeyBinding(openColony);
        ControllingCompat.bindAltModifier(openColony);
    }
}
