package com.enn3developer.gregcolonies.client.gui;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketBlueprintAction;
import com.enn3developer.gregcolonies.network.PacketBlueprintSave;

public final class NetworkEditorGateway implements EditorGateway {

    public static final NetworkEditorGateway INSTANCE = new NetworkEditorGateway();

    private NetworkEditorGateway() {}

    @Override
    public void requestPalette(int colonyId, int index) {
        GCNetwork.CHANNEL.sendToServer(new PacketBlueprintAction(colonyId, PacketBlueprintAction.PALETTE, index));
    }

    @Override
    public void save(int colonyId, int index, Blueprint model) {
        for (PacketBlueprintSave packet : PacketBlueprintSave.split(colonyId, index, model)) {
            GCNetwork.CHANNEL.sendToServer(packet);
        }
    }
}
