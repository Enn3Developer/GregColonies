package com.enn3developer.gregcolonies.client.gui;

import com.enn3developer.gregcolonies.colony.Blueprint;

public interface EditorGateway {

    void requestPalette(int colonyId, int index);

    void save(int colonyId, int index, Blueprint model);
}
