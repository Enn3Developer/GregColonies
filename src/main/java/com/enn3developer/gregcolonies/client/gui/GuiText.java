package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public final class GuiText {

    private static final String ELLIPSIS = "...";

    private GuiText() {}

    public static int width(String text) {
        return font().getStringWidth(text);
    }

    public static String trim(String text, int width) {
        FontRenderer font = font();
        if (width <= 0 || font.getStringWidth(text) <= width) {
            return text;
        }
        int room = width - font.getStringWidth(ELLIPSIS);
        return room <= 0 ? "" : font.trimStringToWidth(text, room) + ELLIPSIS;
    }

    public static String fit(String label, String hint, int room) {
        return trim(label, room - width(hint));
    }

    private static FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }
}
