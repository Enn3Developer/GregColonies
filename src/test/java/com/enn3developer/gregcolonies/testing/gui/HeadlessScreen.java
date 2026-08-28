package com.enn3developer.gregcolonies.testing.gui;

import net.minecraft.client.gui.GuiScreen;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.screen.ModularScreen;

public final class HeadlessScreen extends GuiScreen implements IMuiScreen {

    private final ModularScreen screen;

    HeadlessScreen(ModularScreen screen, int width, int height) {
        this.screen = screen;
        this.mc = ClientBootstrap.minecraft();
        this.fontRendererObj = this.mc.fontRenderer;
        this.width = width;
        this.height = height;
    }

    @Override
    public ModularScreen getScreen() {
        return screen;
    }

    @Override
    public GuiScreen getGuiScreen() {
        return this;
    }

    void size(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
