package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class BlueprintEditorScreen extends ModularScreen {

    private static boolean returning;

    private final BlueprintEditorView view;

    public BlueprintEditorScreen(BlueprintEditorView view) {
        super(GregColonies.MODID, view.buildPanel());
        this.view = view;
        pausesGame(false);
        drawDarkBackground(false);
    }

    public BlueprintEditorView getView() {
        return view;
    }

    public static BlueprintEditorScreen getOpen() {
        ModularScreen current = ModularScreen.getCurrent();
        return !returning && current instanceof BlueprintEditorScreen ? (BlueprintEditorScreen) current : null;
    }

    public static void open(ColonySnapshot colony, int index, Blueprint source) {
        BlueprintEditor editor = new BlueprintEditor(colony, index, source);
        if (editor.getModel() == null) {
            return;
        }
        returning = false;
        Minecraft.getMinecraft()
            .func_152344_a(() -> ClientGUI.open(new BlueprintEditorScreen(new BlueprintEditorView(editor))));
    }

    public static void back() {
        returning = true;
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                ClientGUI.close();
                GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
            });
    }

    public static void accept(PacketColonyPalette palette) {
        BlueprintEditorScreen screen = getOpen();
        if (screen != null) {
            screen.view.acceptPalette(palette);
        }
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && !view.isEditingText() && !view.isConfirming() && !view.requestClose()) {
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public void onOpen() {
        super.onOpen();
        BlueprintGhost.forget();
        ColonyCamera.install(view.getColony());
        view.focusCamera();
        view.getEditor()
            .requestPalette();
    }

    @Override
    public void onClose() {
        super.onClose();
        BlueprintGhost.forget();
        ColonyCamera.remove();
        ColonyWorldOverlay.invalidate();
    }
}
