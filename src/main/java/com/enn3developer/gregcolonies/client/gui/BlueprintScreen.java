package com.enn3developer.gregcolonies.client.gui;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketBlueprintData;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class BlueprintScreen extends ModularScreen {

    private static boolean returning;

    private final BlueprintView view;

    public BlueprintScreen(BlueprintView view) {
        super(GregColonies.MODID, view.buildPanel());
        this.view = view;
        pausesGame(false);
        drawDarkBackground(false);
    }

    public BlueprintView getView() {
        return view;
    }

    public static BlueprintScreen getOpen() {
        ModularScreen current = ModularScreen.getCurrent();
        return !returning && current instanceof BlueprintScreen ? (BlueprintScreen) current : null;
    }

    public static void open(ColonyView colonyView) {
        returning = false;
        ClientGUI.open(new BlueprintScreen(new BlueprintView(colonyView)));
    }

    public static void back() {
        returning = true;
        ClientGUI.close();
        GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
    }

    public static void accept(PacketBlueprintData data) {
        BlueprintScreen screen = getOpen();
        if (screen != null && screen.view.getColony()
            .getId() == data.getColonyId()) {
            screen.view.accept(data.getIndex(), data.getBlueprint(), data.getStock());
        }
    }
}
