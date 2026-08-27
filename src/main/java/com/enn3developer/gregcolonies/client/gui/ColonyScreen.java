package com.enn3developer.gregcolonies.client.gui;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class ColonyScreen extends ModularScreen {

    private static boolean armed;

    private static boolean waiting;

    private static Set<UUID> saved;

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

    public static void armReturn() {
        armed = true;
        waiting = false;
        ColonyScreen screen = getOpen();
        saved = screen == null ? null : new LinkedHashSet<>(screen.view.getSelection());
    }

    public static void onGuiChanged(boolean opened) {
        if (armed) {
            armed = false;
            waiting = opened;
            if (!opened) {
                saved = null;
            }
            return;
        }
        if (waiting && !opened) {
            waiting = false;
            GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
        }
    }

    public static void open(ColonySnapshot colony) {
        BlueprintScreen blueprints = BlueprintScreen.getOpen();
        if (blueprints != null) {
            blueprints.getView()
                .setColony(colony);
            return;
        }
        ColonyScreen current = getOpen();
        if (current != null) {
            current.view.setColony(colony);
            return;
        }
        ColonyView view = new ColonyView(colony);
        if (saved != null) {
            view.getSelection()
                .addAll(saved);
            view.setColony(colony);
            saved = null;
        }
        ClientGUI.open(new ColonyScreen(view));
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
