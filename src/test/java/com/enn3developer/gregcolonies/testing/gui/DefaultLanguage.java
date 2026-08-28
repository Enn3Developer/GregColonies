package com.enn3developer.gregcolonies.testing.gui;

import net.minecraft.client.resources.LanguageManager;

final class DefaultLanguage extends LanguageManager {

    private DefaultLanguage() {
        super(null, "en_US");
    }

    @Override
    public boolean isCurrentLocaleUnicode() {
        return false;
    }

    @Override
    public boolean isCurrentLanguageBidirectional() {
        return false;
    }
}
