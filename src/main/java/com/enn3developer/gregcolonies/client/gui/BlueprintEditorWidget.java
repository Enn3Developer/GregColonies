package com.enn3developer.gregcolonies.client.gui;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;

public class BlueprintEditorWidget extends CameraWidget<BlueprintEditorWidget> {

    private final BlueprintEditorView view;

    private final double[] ray = new double[6];

    private boolean stroking;

    public BlueprintEditorWidget(BlueprintEditorView view) {
        this.view = view;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        boolean live = isHovering() && !view.isConfirming();
        view.setHover(live ? traceAt(getContext().getMouseX(), getContext().getMouseY()) : null);
    }

    private BlueprintTrace.Hit traceAt(int mouseX, int mouseY) {
        if (!ColonyWorldOverlay.ray(mouseX, mouseY, ray)) {
            return null;
        }
        return view.getEditor()
            .trace(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5]);
    }

    @Override
    protected boolean isCameraBusy() {
        return view.isEditingText() || view.isConfirming();
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (view.isConfirming()) {
            return Result.IGNORE;
        }
        beginDrag(mouseButton);
        if (mouseButton != 0) {
            return Result.SUCCESS;
        }
        BlueprintTrace.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
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
        } else if (mouseButton == 1 && getDragTravel() <= CLICK_SLOP) {
            BlueprintTrace.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
            BlueprintEditor editor = view.getEditor();
            if (hit != null && hit.solid) {
                int tool = editor.getTool();
                if (tool == BlueprintEditor.TOOL_PAINT || tool == BlueprintEditor.TOOL_ERASE) {
                    editor.pushUndo();
                }
                editor.apply(hit, true);
            }
        }
        endDrag();
        return true;
    }

    @Override
    protected boolean onPrimaryDrag() {
        if (!stroking) {
            return false;
        }
        BlueprintTrace.Hit hit = traceAt(getContext().getMouseX(), getContext().getMouseY());
        if (hit != null) {
            view.getEditor()
                .apply(hit, false);
            view.setHover(hit);
        }
        return true;
    }

    @Override
    protected boolean onCameraScroll(UpOrDown scrollDirection) {
        if (!Interactable.hasShiftDown()) {
            return false;
        }
        view.getEditor()
            .stepLayer(scrollDirection.isUp() ? 1 : -1);
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
        if (turnKey(keyCode)) {
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
