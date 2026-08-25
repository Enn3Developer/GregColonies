package com.enn3developer.gregcolonies.client;

import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class GCClientEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (GCKeyBindings.openColony.isPressed()) {
            GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
        }
    }
}
