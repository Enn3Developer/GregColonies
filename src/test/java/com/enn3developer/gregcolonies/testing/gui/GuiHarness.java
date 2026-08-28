package com.enn3developer.gregcolonies.testing.gui;

import java.util.Queue;
import java.util.concurrent.FutureTask;

import net.minecraft.client.gui.FontRenderer;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widget.WidgetTree;

public final class GuiHarness {

    public static final int LEFT = 0;

    public static final int RIGHT = 1;

    public static final int MIDDLE = 2;

    private static final int DRAG_STEPS = 8;

    private final ModularScreen screen;

    private final HeadlessScreen wrapper;

    private int width;

    private int height;

    private int mouseX;

    private int mouseY;

    private long dragTicks;

    public static GuiHarness open(ModularScreen screen) {
        return open(screen, ClientBootstrap.WIDTH, ClientBootstrap.HEIGHT);
    }

    public static GuiHarness open(ModularScreen screen, int width, int height) {
        ClientBootstrap.ensure();
        GuiHarness harness = new GuiHarness(screen, width, height);
        screen.construct(harness.wrapper);
        if (!screen.getContext()
            .hasSettings()) {
            screen.getContext()
                .setSettings(new UISettings());
        }
        harness.resize(width, height);
        harness.tick();
        return harness;
    }

    private GuiHarness(ModularScreen screen, int width, int height) {
        this.screen = screen;
        this.width = width;
        this.height = height;
        this.wrapper = new HeadlessScreen(screen, width, height);
    }

    public ModularScreen screen() {
        return screen;
    }

    public ModularGuiContext context() {
        return screen.getContext();
    }

    public FontRenderer font() {
        return ClientBootstrap.minecraft().fontRenderer;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public GuiHarness resize(int width, int height) {
        this.width = width;
        this.height = height;
        wrapper.size(width, height);
        ClientBootstrap.display(width, height);
        screen.onResize(width, height);
        return moveMouse(mouseX, mouseY);
    }

    public GuiHarness tick() {
        screen.onUpdate();
        layout();
        return frame();
    }

    private void layout() {
        for (ModularPanel panel : screen.getPanelManager()
            .getOpenPanels()) {
            WidgetTree.resize(panel);
        }
    }

    public GuiHarness ticks(int count) {
        for (int tick = 0; tick < count; tick++) {
            tick();
        }
        return this;
    }

    @SuppressWarnings("deprecation")
    public GuiHarness frame() {
        context().updateState(mouseX, mouseY, 0.0F);
        screen.onFrameUpdate();
        return this;
    }

    public GuiHarness moveMouse(int x, int y) {
        mouseX = x;
        mouseY = y;
        return frame();
    }

    public GuiHarness moveTo(Rendered.Node node) {
        return moveMouse(node.centerX(), node.centerY());
    }

    public boolean press(int button) {
        dragTicks = 0;
        return screen.onMousePressed(button);
    }

    public boolean release(int button) {
        return screen.onMouseRelease(button);
    }

    public boolean click(int x, int y) {
        return click(x, y, LEFT);
    }

    public boolean click(int x, int y, int button) {
        moveMouse(x, y);
        boolean handled = press(button);
        release(button);
        return handled;
    }

    public boolean click(Rendered.Node node) {
        return click(node.centerX(), node.centerY(), LEFT);
    }

    public boolean click(Rendered.Node node, int button) {
        return click(node.centerX(), node.centerY(), button);
    }

    public GuiHarness drag(int fromX, int fromY, int toX, int toY) {
        return drag(fromX, fromY, toX, toY, LEFT);
    }

    public GuiHarness drag(int fromX, int fromY, int toX, int toY, int button) {
        moveMouse(fromX, fromY);
        press(button);
        for (int step = 1; step <= DRAG_STEPS; step++) {
            moveMouse(fromX + (toX - fromX) * step / DRAG_STEPS, fromY + (toY - fromY) * step / DRAG_STEPS);
            screen.onMouseDrag(button, ++dragTicks);
        }
        release(button);
        return this;
    }

    public boolean scroll(int x, int y, boolean up) {
        moveMouse(x, y);
        return screen.onMouseScroll(up ? UpOrDown.UP : UpOrDown.DOWN, 1);
    }

    public boolean scrollUp(int x, int y) {
        return scroll(x, y, true);
    }

    public boolean scrollDown(int x, int y) {
        return scroll(x, y, false);
    }

    public boolean key(int keyCode) {
        return key('\0', keyCode);
    }

    public boolean key(char typed, int keyCode) {
        boolean handled = screen.onKeyPressed(typed, keyCode);
        screen.onKeyRelease(typed, keyCode);
        return handled;
    }

    public GuiHarness type(String text) {
        for (int index = 0; index < text.length(); index++) {
            key(text.charAt(index), 0);
        }
        return this;
    }

    public int scheduled() {
        return ClientBootstrap.scheduled()
            .size();
    }

    public GuiHarness runScheduled() {
        Queue<FutureTask<?>> queue = ClientBootstrap.scheduled();
        for (FutureTask<?> task = queue.poll(); task != null; task = queue.poll()) {
            task.run();
        }
        return this;
    }

    public GuiHarness dropScheduled() {
        ClientBootstrap.scheduled()
            .clear();
        return this;
    }

    public Rendered render() {
        tick();
        return Rendered.of(screen);
    }

    public void close() {
        screen.onClose();
    }
}
