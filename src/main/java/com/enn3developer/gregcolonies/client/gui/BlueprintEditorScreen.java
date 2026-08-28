package com.enn3developer.gregcolonies.client.gui;

import org.lwjgl.input.Keyboard;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;

public class BlueprintEditorScreen extends GCScreen<BlueprintEditorView> {

    private static boolean returning;

    public BlueprintEditorScreen(BlueprintEditorView view) {
        super(view, view.buildPanel());
    }

    public static BlueprintEditorScreen getOpen() {
        return current(BlueprintEditorScreen.class, returning);
    }

    public static void open(ColonySnapshot colony, int index, Blueprint source) {
        BlueprintEditor editor = new BlueprintEditor(colony, index, source);
        if (editor.getModel() == null) {
            return;
        }
        returning = false;
        openLater("the blueprint editor", () -> new BlueprintEditorScreen(new BlueprintEditorView(editor)));
    }

    public static void back() {
        returning = true;
        closeLater();
    }

    public static void accept(PacketColonyPalette palette) {
        BlueprintEditorScreen screen = getOpen();
        if (screen != null) {
            screen.getView()
                .acceptPalette(palette);
        }
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE && !getView().isEditingText()
            && !getView().isConfirming()
            && !getView().requestClose()) {
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public void onOpen() {
        super.onOpen();
        BlueprintGhost.forget();
        ColonyCamera.install(getView().getColony());
        getView().focusCamera();
        getView().getEditor()
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
