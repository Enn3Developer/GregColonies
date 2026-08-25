package com.enn3developer.gregcolonies.client;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import com.blamejared.controlling.api.ComboModifier;
import com.blamejared.controlling.api.ControllingApi;
import com.enn3developer.gregcolonies.GregColonies;

import cpw.mods.fml.common.Loader;

public final class ControllingCompat {

    public static final String MODID = "controlling";

    private static final boolean LOADED = Loader.isModLoaded(MODID);

    private ControllingCompat() {}

    public static boolean isAvailable() {
        return LOADED;
    }

    public static void bindAltModifier(KeyBinding binding) {
        if (!LOADED) {
            GregColonies.LOG
                .warn("Controlling is not installed, the colony keybind falls back to a plain key without Alt");
            return;
        }
        Api.bindAlt(binding);
    }

    public static String describe(KeyBinding binding) {
        String key = GameSettings.getKeyDisplayString(binding.getKeyCode());
        return LOADED ? Api.prefix(binding) + key : key;
    }

    private static final class Api {

        private Api() {}

        static void bindAlt(KeyBinding binding) {
            ControllingApi.setDefaultComboKeyBinding(binding, ComboModifier.ALT);
        }

        static String prefix(KeyBinding binding) {
            ComboModifier modifier = ControllingApi.getComboModifier(binding);
            return modifier == ComboModifier.NONE ? ""
                : modifier.toInternal()
                    .getDisplayName() + " + ";
        }
    }
}
