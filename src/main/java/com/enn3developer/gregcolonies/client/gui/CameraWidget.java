package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widget.Widget;

public abstract class CameraWidget<W extends CameraWidget<W>> extends Widget<W> implements Interactable {

    protected static final float ZOOM_STEP = 1.2F;

    protected static final float ROTATE_STEP = 45.0F;

    protected static final float DRAG_YAW = 0.4F;

    protected static final float DRAG_PITCH = 0.3F;

    protected static final float KEY_PAN_PIXELS = 12.0F;

    protected static final int CLICK_SLOP = 4;

    private static final double DEFAULT_FOV = 70.0D;

    private int dragX;

    private int dragY;

    private int dragButton = -1;

    private int dragTravel;

    protected boolean isCameraBusy() {
        return false;
    }

    protected boolean onPrimaryDrag() {
        return false;
    }

    protected boolean onCameraScroll(UpOrDown scrollDirection) {
        return false;
    }

    protected int getDragButton() {
        return dragButton;
    }

    protected int getDragTravel() {
        return dragTravel;
    }

    protected void beginDrag(int mouseButton) {
        dragButton = mouseButton;
        dragTravel = 0;
        dragX = getContext().getAbsMouseX();
        dragY = getContext().getAbsMouseY();
    }

    protected void endDrag() {
        dragButton = -1;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        keyboardPan();
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (dragButton < 0) {
            return;
        }
        int mouseX = getContext().getAbsMouseX();
        int mouseY = getContext().getAbsMouseY();
        int deltaX = mouseX - dragX;
        int deltaY = mouseY - dragY;
        dragX = mouseX;
        dragY = mouseY;
        dragTravel += Math.abs(deltaX) + Math.abs(deltaY);
        if (dragButton == 0) {
            onPrimaryDrag();
            return;
        }
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return;
        }
        if (dragButton == 1) {
            camera.pan(deltaX, deltaY, panScale());
        } else if (dragButton == 2) {
            camera.rotate(deltaX * DRAG_YAW, -deltaY * DRAG_PITCH);
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (onCameraScroll(scrollDirection)) {
            return true;
        }
        ColonyCamera camera = ColonyCamera.get();
        if (camera != null) {
            camera.zoom(scrollDirection.isUp() ? 1.0F / ZOOM_STEP : ZOOM_STEP);
        }
        return true;
    }

    @Override
    public @NotNull Result onKeyPressed(char typedChar, int keyCode) {
        return turnKey(keyCode) ? Result.SUCCESS : Result.IGNORE;
    }

    protected boolean turnKey(int keyCode) {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return false;
        }
        if (keyCode == Keyboard.KEY_Q) {
            camera.rotate(-ROTATE_STEP, 0.0F);
            return true;
        }
        if (keyCode == Keyboard.KEY_E) {
            camera.rotate(ROTATE_STEP, 0.0F);
            return true;
        }
        return false;
    }

    protected void keyboardPan() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null || isCameraBusy()) {
            return;
        }
        double x = 0.0D;
        double y = 0.0D;
        if (Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            x += KEY_PAN_PIXELS;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            x -= KEY_PAN_PIXELS;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            y += KEY_PAN_PIXELS;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            y -= KEY_PAN_PIXELS;
        }
        if (x != 0.0D || y != 0.0D) {
            camera.pan(x, y, panScale());
        }
    }

    protected double panScale() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return 0.0D;
        }
        float setting = Minecraft.getMinecraft().gameSettings.fovSetting;
        double fov = Math.toRadians(setting > 0.0F ? setting : DEFAULT_FOV);
        double height = Math.max(1, getArea().h());
        return 2.0D * camera.getDistance() * Math.tan(fov / 2.0D) / height;
    }
}
