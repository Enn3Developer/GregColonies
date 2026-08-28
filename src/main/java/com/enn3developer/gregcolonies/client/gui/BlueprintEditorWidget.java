package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;

public class BlueprintEditorWidget extends Widget<BlueprintEditorWidget> implements Interactable {

    private static final float ZOOM_STEP = 1.2F;

    private static final float ROTATE_STEP = 45.0F;

    private static final float DRAG_YAW = 0.4F;

    private static final float DRAG_PITCH = 0.3F;

    private static final float KEY_PAN_PIXELS = 12.0F;

    private static final int CLICK_SLOP = 4;

    private static final double DEFAULT_FOV = 70.0D;

    private final BlueprintEditorView view;

    private final double[] ray = new double[6];

    private int dragX;

    private int dragY;

    private int dragButton = -1;

    private int dragTravel;

    private boolean stroking;

    public BlueprintEditorWidget(BlueprintEditorView view) {
        this.view = view;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        boolean live = isHovering() && !view.isConfirming();
        view.setHover(live ? traceAt(getContext().getMouseX(), getContext().getMouseY()) : null);
    }

    private BlueprintEditor.Hit traceAt(int mouseX, int mouseY) {
        if (!ColonyWorldOverlay.ray(mouseX, mouseY, ray)) {
            return null;
        }
        return view.getEditor()
            .trace(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        keyboardPan();
    }

    private void keyboardPan() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null || view.isEditingText() || view.isConfirming()) {
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

    private double panScale() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return 0.0D;
        }
        float setting = Minecraft.getMinecraft().gameSettings.fovSetting;
        double fov = Math.toRadians(setting > 0.0F ? setting : DEFAULT_FOV);
        double height = Math.max(1, getArea().h());
        return 2.0D * camera.getDistance() * Math.tan(fov / 2.0D) / height;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (view.isConfirming()) {
            return Result.IGNORE;
        }
        dragButton = mouseButton;
        dragTravel = 0;
        dragX = getContext().getAbsMouseX();
        dragY = getContext().getAbsMouseY();
        if (mouseButton != 0) {
            return Result.SUCCESS;
        }
        BlueprintEditor.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
        if (hit == null) {
            return Result.SUCCESS;
        }
        BlueprintEditor editor = view.getEditor();
        if (editor.getTool() == BlueprintEditor.TOOL_PAINT || editor.getTool() == BlueprintEditor.TOOL_ERASE) {
            editor.pushUndo();
            stroking = true;
        }
        editor.apply(hit, false);
        view.setHover(hit);
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        if (mouseButton == 0) {
            stroking = false;
        } else if (mouseButton == 1 && dragTravel <= CLICK_SLOP) {
            BlueprintEditor.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
            BlueprintEditor editor = view.getEditor();
            if (hit != null && hit.solid) {
                int tool = editor.getTool();
                if (tool == BlueprintEditor.TOOL_PAINT || tool == BlueprintEditor.TOOL_ERASE) {
                    editor.pushUndo();
                }
                editor.apply(hit, true);
            }
        }
        dragButton = -1;
        return true;
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
            if (stroking) {
                BlueprintEditor.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
                if (hit != null) {
                    view.getEditor()
                        .apply(hit, false);
                    view.setHover(hit);
                }
            }
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
        if (Interactable.hasShiftDown()) {
            view.getEditor()
                .stepLayer(scrollDirection.isUp() ? 1 : -1);
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
        if (view.isEditingText() || view.isConfirming()) {
            return Result.IGNORE;
        }
        BlueprintEditor editor = view.getEditor();
        if (Interactable.hasControlDown()) {
            if (keyCode == Keyboard.KEY_Z) {
                editor.undo();
                return Result.SUCCESS;
            }
            if (keyCode == Keyboard.KEY_Y) {
                editor.redo();
                return Result.SUCCESS;
            }
            return Result.IGNORE;
        }
        ColonyCamera camera = ColonyCamera.get();
        if (camera != null && keyCode == Keyboard.KEY_Q) {
            camera.rotate(-ROTATE_STEP, 0.0F);
            return Result.SUCCESS;
        }
        if (camera != null && keyCode == Keyboard.KEY_E) {
            camera.rotate(ROTATE_STEP, 0.0F);
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_R) {
            view.focusCamera();
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_X) {
            editor.toggleSlice();
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            editor.setTool((editor.getTool() + 1) % BlueprintEditor.TOOL_COUNT);
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_H) {
            view.toggleHelp();
            return Result.SUCCESS;
        }
        if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
            editor.setBrush(keyCode - Keyboard.KEY_1);
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }
}
