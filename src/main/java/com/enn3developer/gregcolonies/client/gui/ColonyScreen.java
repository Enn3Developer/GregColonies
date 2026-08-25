package com.enn3developer.gregcolonies.client.gui;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.network.ColonySnapshot;

public class ColonyScreen extends ModularScreen {

    private final ColonyView view;

    public ColonyScreen(ColonyView view) {
        super(GregColonies.MODID, view.buildPanel());
        this.view = view;
        pausesGame(false);
        drawDarkBackground(false);
    }

    public ColonyView getView() {
        return view;
    }

    public static ColonyScreen getOpen() {
        ModularScreen current = ModularScreen.getCurrent();
        return current instanceof ColonyScreen ? (ColonyScreen) current : null;
    }

    public static void open(ColonySnapshot colony) {
        ColonyScreen current = getOpen();
        if (current != null) {
            current.view.setColony(colony);
            return;
        }
        ClientGUI.open(new ColonyScreen(new ColonyView(colony)));
    }

    @Override
    public void onOpen() {
        super.onOpen();
        ColonyCamera.install(view.getColony());
    }

    @Override
    public void onClose() {
        super.onClose();
        ColonyCamera.remove();
        ColonyWorldOverlay.invalidate();
    }
}
