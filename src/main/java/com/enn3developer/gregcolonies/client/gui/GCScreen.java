package com.enn3developer.gregcolonies.client.gui;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public abstract class GCScreen<V> extends ModularScreen {

    private final V view;

    protected GCScreen(V view, ModularPanel panel) {
        super(GregColonies.MODID, panel);
        this.view = view;
        pausesGame(false);
        drawDarkBackground(false);
    }

    public V getView() {
        return view;
    }

    protected static <S extends ModularScreen> S current(Class<S> type, boolean returning) {
        ModularScreen screen = ModularScreen.getCurrent();
        return !returning && type.isInstance(screen) ? type.cast(screen) : null;
    }

    protected static void openLater(String subject, Supplier<ModularScreen> factory) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                try {
                    ClientGUI.open(factory.get());
                } catch (RuntimeException error) {
                    GregColonies.LOG.error("Failed to open " + subject, error);
                }
            });
    }

    protected static void closeLater() {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                ClientGUI.close();
                GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
            });
    }
}
