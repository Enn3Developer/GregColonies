package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class ColonyViewWidget extends Widget<ColonyViewWidget> implements Interactable {

    private static final int DATA_INTERVAL = 20;

    private static final float ZOOM_STEP = 1.2F;

    private static final float ROTATE_STEP = 45.0F;

    private static final float DRAG_YAW = 0.4F;

    private static final float DRAG_PITCH = 0.3F;

    private static final double HIT_RADIUS = 12.0D;

    private static final float MARKER_SIZE = 5.0F;

    private static final float MARKER_HOVER_SIZE = 8.0F;

    private static final int MARKER_OUTLINE = 0xC0000000;

    private static final double MARKER_HEIGHT = 2.4D;

    private static final double DEFAULT_FOV = 70.0D;

    private final ColonyView view;

    private final double[] projected = new double[3];

    private int dataTicks;

    private int dragX;

    private int dragY;

    private int dragButton = -1;

    public ColonyViewWidget(ColonyView view) {
        this.view = view;
        tooltipAutoUpdate(true);
        tooltipDynamic(tooltip -> {
            CitizenSnapshot citizen = findHovered();
            if (citizen == null) {
                return;
            }
            String group = citizen.getGroup()
                .isEmpty() ? "no group" : citizen.getGroup();
            tooltip.add(IKey.str("Citizen (" + group + ")"))
                .newLine();
            tooltip.add(IKey.str(String.format("%.0f / %.0f / %.0f", citizen.getX(), citizen.getY(), citizen.getZ())))
                .newLine();
            if (!citizen.isLoaded()) {
                tooltip.add(IKey.str("last seen, chunk not loaded"))
                    .newLine();
                return;
            }
            tooltip.add(IKey.str(String.format("health %.1f / %.1f", citizen.getHealth(), citizen.getMaxHealth())))
                .newLine();
            tooltip.add(IKey.str("food " + citizen.getFoodLevel() + " / 20"))
                .newLine();
            String task = citizen.getTask()
                .isEmpty() ? "idle" : citizen.getTask();
            tooltip.add(IKey.str("task " + task + " (+" + citizen.getPendingCount() + " queued)"))
                .newLine();
        });
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        CitizenSnapshot hovered = findHovered();
        for (CitizenSnapshot citizen : view.getColony()
            .getCitizens()) {
            if (!projectCitizen(citizen, MARKER_HEIGHT)) {
                continue;
            }
            float size = citizen == hovered ? MARKER_HOVER_SIZE : MARKER_SIZE;
            int color = ColonyWorldOverlay.groupColor(citizen.getGroup());
            if (!citizen.isLoaded()) {
                color = color & 0x00FFFFFF | 0x60000000;
            }
            float x = (float) projected[0];
            float y = (float) projected[1];
            GuiDraw.drawEllipse(
                x - size / 2.0F - 1.0F,
                y - size / 2.0F - 1.0F,
                size + 2.0F,
                size + 2.0F,
                MARKER_OUTLINE,
                MARKER_OUTLINE,
                12);
            GuiDraw.drawEllipse(x - size / 2.0F, y - size / 2.0F, size, size, color, color, 12);
        }
    }

    private boolean projectCitizen(CitizenSnapshot citizen, double heightOffset) {
        Entity entity = ColonyWorldOverlay.liveEntity(citizen);
        double x = entity == null ? citizen.getX() : entity.posX;
        double y = (entity == null ? citizen.getY() : entity.posY) + heightOffset;
        double z = entity == null ? citizen.getZ() : entity.posZ;
        return ColonyWorldOverlay.project(x, y, z, projected);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (++dataTicks >= DATA_INTERVAL) {
            dataTicks = 0;
            GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
        }
    }

    private CitizenSnapshot findHovered() {
        if (!isHovering()) {
            return null;
        }
        int mouseX = getContext().getMouseX();
        int mouseY = getContext().getMouseY();
        CitizenSnapshot best = null;
        double bestDistance = HIT_RADIUS * HIT_RADIUS;
        for (CitizenSnapshot citizen : view.getColony()
            .getCitizens()) {
            if (!projectCitizen(citizen, MARKER_HEIGHT)) {
                continue;
            }
            double dx = projected[0] - mouseX;
            double dy = projected[1] - mouseY;
            double distance = dx * dx + dy * dy;
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = citizen;
            }
        }
        return best;
    }

    private double panScale() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return 0.0D;
        }
        double fov = Math.toRadians(
            Minecraft.getMinecraft().gameSettings.fovSetting > 0.0F ? Minecraft.getMinecraft().gameSettings.fovSetting
                : DEFAULT_FOV);
        double height = Math.max(1, getArea().h());
        return 2.0D * camera.getDistance() * Math.tan(fov / 2.0D) / height;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        dragButton = mouseButton;
        dragX = getContext().getAbsMouseX();
        dragY = getContext().getAbsMouseY();
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        dragButton = -1;
        return true;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null || dragButton < 0) {
            return;
        }
        int mouseX = getContext().getAbsMouseX();
        int mouseY = getContext().getAbsMouseY();
        int deltaX = mouseX - dragX;
        int deltaY = mouseY - dragY;
        dragX = mouseX;
        dragY = mouseY;
        if (dragButton == 1) {
            camera.rotate(deltaX * DRAG_YAW, -deltaY * DRAG_PITCH);
        } else {
            camera.pan(deltaX, deltaY, panScale());
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        ColonyCamera camera = ColonyCamera.get();
        if (camera != null) {
            camera.zoom(scrollDirection.isUp() ? 1.0F / ZOOM_STEP : ZOOM_STEP);
        }
        return true;
    }

    @Override
    public @NotNull Result onKeyPressed(char typedChar, int keyCode) {
        ColonyCamera camera = ColonyCamera.get();
        if (camera != null && keyCode == Keyboard.KEY_Q) {
            camera.rotate(-ROTATE_STEP, 0.0F);
            return Result.SUCCESS;
        }
        if (camera != null && keyCode == Keyboard.KEY_E) {
            camera.rotate(ROTATE_STEP, 0.0F);
            return Result.SUCCESS;
        }
        if (camera != null && keyCode == Keyboard.KEY_R) {
            camera.reset(view.getColony());
            return Result.SUCCESS;
        }
        if (GCKeyBindings.openColony != null && keyCode == GCKeyBindings.openColony.getKeyCode()) {
            getPanel().closeIfOpen();
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }
}
