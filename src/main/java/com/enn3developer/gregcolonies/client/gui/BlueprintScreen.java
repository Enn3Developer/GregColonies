package com.enn3developer.gregcolonies.client.gui;

import com.enn3developer.gregcolonies.network.PacketBlueprintData;

public class BlueprintScreen extends GCScreen<BlueprintView> {

    private static boolean returning;

    public BlueprintScreen(BlueprintView view) {
        super(view, view.buildPanel());
    }

    public static BlueprintScreen getOpen() {
        return current(BlueprintScreen.class, returning);
    }

    public static void open(ColonyView colonyView) {
        returning = false;
        openLater("the blueprint library", () -> new BlueprintScreen(new BlueprintView(colonyView)));
    }

    public static void back() {
        BlueprintScreen screen = getOpen();
        returning = true;
        ColonyScreen.restore(
            screen == null ? null
                : screen.getView()
                    .getColonyView()
                    .getSelection());
        closeLater();
    }

    @Override
    public void onClose() {
        super.onClose();
        BlueprintPreview.forget();
    }

    public static void accept(PacketBlueprintData data) {
        BlueprintScreen screen = getOpen();
        if (screen != null && screen.getView()
            .getColony()
            .getId() == data.getColonyId()) {
            screen.getView()
                .accept(data.getIndex(), data.getBlueprint(), data.getStock());
        }
    }
}
