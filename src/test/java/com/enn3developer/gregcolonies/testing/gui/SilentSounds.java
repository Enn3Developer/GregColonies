package com.enn3developer.gregcolonies.testing.gui;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;

final class SilentSounds extends SoundHandler {

    private SilentSounds() {
        super(null, null);
    }

    @Override
    public void playSound(ISound sound) {}

    @Override
    public void playDelayedSound(ISound sound, int delay) {}
}
