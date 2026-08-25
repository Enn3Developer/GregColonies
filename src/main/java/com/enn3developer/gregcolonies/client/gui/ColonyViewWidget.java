package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;

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
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandChop;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class ColonyViewWidget extends Widget<ColonyViewWidget> implements Interactable {

    private static final int DATA_INTERVAL = 20;

    private static final float ZOOM_STEP = 1.2F;

    private static final float ROTATE_STEP = 45.0F;

    private static final float DRAG_YAW = 0.4F;

    private static final float DRAG_PITCH = 0.3F;

    private static final float KEY_PAN_PIXELS = 12.0F;

    private static final int CLICK_SLOP = 4;

    private static final double HIT_RADIUS = 12.0D;

    private static final double DEFAULT_FOV = 70.0D;

    private static final double MARKER_HEIGHT = 2.4D;

    private static final float MARKER_SIZE = 5.0F;

    private static final float MARKER_HOVER_SIZE = 8.0F;

    private static final int MARKER_OUTLINE = 0xC0000000;

    private static final int SELECTED_OUTLINE = 0xFFFFFFFF;

    private static final int BOX_FILL = 0x2033CCFF;

    private static final int BOX_EDGE = 0xC033CCFF;

    private final ColonyView view;

    private final double[] projected = new double[3];

    private int dataTicks;

    private int dragX;

    private int dragY;

    private int dragButton = -1;

    private int dragTravel;

    private int boxStartX;

    private int boxStartY;

    private boolean boxSelecting;

    private boolean areaDragging;

    private int areaAnchorX;

    private int areaAnchorZ;

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
            if (!projectCitizen(citizen)) {
                continue;
            }
            boolean selected = view.isSelected(citizen.getId());
            float size = citizen == hovered ? MARKER_HOVER_SIZE : MARKER_SIZE;
            int color = ColonyWorldOverlay.groupColor(citizen.getGroup());
            if (!citizen.isLoaded()) {
                color = color & 0x00FFFFFF | 0x60000000;
            }
            int outline = selected ? SELECTED_OUTLINE : MARKER_OUTLINE;
            float x = (float) projected[0];
            float y = (float) projected[1];
            GuiDraw.drawEllipse(
                x - size / 2.0F - 1.5F,
                y - size / 2.0F - 1.5F,
                size + 3.0F,
                size + 3.0F,
                outline,
                outline,
                12);
            GuiDraw.drawEllipse(x - size / 2.0F, y - size / 2.0F, size, size, color, color, 12);
        }
        updateTargetPreview();
        if (boxSelecting) {
            int x0 = Math.min(boxStartX, getContext().getMouseX());
            int y0 = Math.min(boxStartY, getContext().getMouseY());
            int x1 = Math.max(boxStartX, getContext().getMouseX());
            int y1 = Math.max(boxStartY, getContext().getMouseY());
            GuiDraw.drawRect(x0, y0, x1 - x0, y1 - y0, BOX_FILL);
            GuiDraw.drawRect(x0, y0, x1 - x0, 1, BOX_EDGE);
            GuiDraw.drawRect(x0, y1 - 1, x1 - x0, 1, BOX_EDGE);
            GuiDraw.drawRect(x0, y0, 1, y1 - y0, BOX_EDGE);
            GuiDraw.drawRect(x1 - 1, y0, 1, y1 - y0, BOX_EDGE);
        }
    }

    private void updateTargetPreview() {
        int mode = view.getTargeting();
        if (mode == ColonyView.TARGET_NONE) {
            return;
        }
        if (!isHovering() && !areaDragging) {
            view.clearPending();
            return;
        }
        MovingObjectPosition hit = ColonyWorldOverlay.pick(getContext().getMouseX(), getContext().getMouseY());
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            if (!areaDragging) {
                view.clearPending();
            }
            return;
        }
        if (mode == ColonyView.TARGET_MINE) {
            int chunkX = hit.blockX >> 4;
            int chunkZ = hit.blockZ >> 4;
            view.setPending(chunkX << 4, hit.blockY, chunkZ << 4, (chunkX << 4) + 15, hit.blockY, (chunkZ << 4) + 15);
            return;
        }
        int anchorX = areaDragging ? areaAnchorX : hit.blockX;
        int anchorZ = areaDragging ? areaAnchorZ : hit.blockZ;
        int x = clampSide(anchorX, hit.blockX);
        int z = clampSide(anchorZ, hit.blockZ);
        view.setPending(
            Math.min(anchorX, x),
            hit.blockY,
            Math.min(anchorZ, z),
            Math.max(anchorX, x),
            hit.blockY,
            Math.max(anchorZ, z));
    }

    private static int clampSide(int anchor, int value) {
        if (value - anchor > CitizenCommandChop.MAX_SIDE - 1) {
            return anchor + CitizenCommandChop.MAX_SIDE - 1;
        }
        if (anchor - value > CitizenCommandChop.MAX_SIDE - 1) {
            return anchor - CitizenCommandChop.MAX_SIDE + 1;
        }
        return value;
    }

    private void issueArea() {
        if (!view.hasPending()) {
            return;
        }
        int[] area = view.getPending();
        byte action = view.getTargeting() == ColonyView.TARGET_MINE ? PacketCitizenCommand.MINE
            : PacketCitizenCommand.CHOP;
        view.sendArea(action, Interactable.hasShiftDown(), area[0], area[1], area[2], area[3], area[4], area[5]);
        view.setTargeting(ColonyView.TARGET_NONE);
    }

    private boolean projectCitizen(CitizenSnapshot citizen) {
        Entity entity = ColonyWorldOverlay.liveEntity(citizen);
        double x = entity == null ? citizen.getX() : entity.posX;
        double y = (entity == null ? citizen.getY() : entity.posY) + MARKER_HEIGHT;
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
        keyboardPan();
    }

    private void keyboardPan() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null || view.isEditing()) {
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

    private CitizenSnapshot findHovered() {
        if (!isHovering()) {
            return null;
        }
        return citizenAt(getContext().getMouseX(), getContext().getMouseY());
    }

    private CitizenSnapshot citizenAt(int mouseX, int mouseY) {
        CitizenSnapshot best = null;
        double bestDistance = HIT_RADIUS * HIT_RADIUS;
        for (CitizenSnapshot citizen : view.getColony()
            .getCitizens()) {
            if (!projectCitizen(citizen)) {
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
        float setting = Minecraft.getMinecraft().gameSettings.fovSetting;
        double fov = Math.toRadians(setting > 0.0F ? setting : DEFAULT_FOV);
        double height = Math.max(1, getArea().h());
        return 2.0D * camera.getDistance() * Math.tan(fov / 2.0D) / height;
    }

    private void selectBox() {
        int x0 = Math.min(boxStartX, getContext().getMouseX());
        int y0 = Math.min(boxStartY, getContext().getMouseY());
        int x1 = Math.max(boxStartX, getContext().getMouseX());
        int y1 = Math.max(boxStartY, getContext().getMouseY());
        if (!Interactable.hasShiftDown()) {
            view.clearSelection();
        }
        for (CitizenSnapshot citizen : view.getColony()
            .getCitizens()) {
            if (!projectCitizen(citizen)) {
                continue;
            }
            if (projected[0] >= x0 && projected[0] <= x1 && projected[1] >= y0 && projected[1] <= y1) {
                view.getSelection()
                    .add(citizen.getId());
            }
        }
    }

    private void selectAt(int mouseX, int mouseY) {
        CitizenSnapshot citizen = citizenAt(mouseX, mouseY);
        if (citizen == null) {
            if (!Interactable.hasShiftDown()) {
                view.clearSelection();
            }
            return;
        }
        if (Interactable.hasShiftDown()) {
            view.toggle(citizen.getId());
            return;
        }
        view.clearSelection();
        view.getSelection()
            .add(citizen.getId());
    }

    private void issueMove(int mouseX, int mouseY) {
        MovingObjectPosition hit = ColonyWorldOverlay.pick(mouseX, mouseY);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        view.sendCommand(
            PacketCitizenCommand.MOVE,
            Interactable.hasShiftDown(),
            hit.blockX,
            hit.blockY + 1,
            hit.blockZ);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        dragButton = mouseButton;
        dragTravel = 0;
        dragX = getContext().getAbsMouseX();
        dragY = getContext().getAbsMouseY();
        if (mouseButton == 0 && view.getTargeting() != ColonyView.TARGET_NONE) {
            MovingObjectPosition hit = ColonyWorldOverlay.pick(getContext().getMouseX(), getContext().getMouseY());
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                areaAnchorX = hit.blockX;
                areaAnchorZ = hit.blockZ;
                areaDragging = true;
            }
            return Result.SUCCESS;
        }
        if (mouseButton == 0) {
            boxStartX = getContext().getMouseX();
            boxStartY = getContext().getMouseY();
            boxSelecting = true;
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        if (mouseButton == 0 && areaDragging) {
            updateTargetPreview();
            areaDragging = false;
            issueArea();
            dragButton = -1;
            return true;
        }
        if (mouseButton == 1 && dragTravel <= CLICK_SLOP && view.getTargeting() != ColonyView.TARGET_NONE) {
            view.setTargeting(ColonyView.TARGET_NONE);
            dragButton = -1;
            return true;
        }
        if (mouseButton == 0) {
            if (boxSelecting) {
                if (dragTravel <= CLICK_SLOP) {
                    selectAt(getContext().getMouseX(), getContext().getMouseY());
                } else {
                    selectBox();
                }
            }
            boxSelecting = false;
        } else if (mouseButton == 1 && dragTravel <= CLICK_SLOP) {
            issueMove(getContext().getMouseX(), getContext().getMouseY());
        }
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
        dragTravel += Math.abs(deltaX) + Math.abs(deltaY);
        if (dragButton == 1) {
            camera.pan(deltaX, deltaY, panScale());
        } else if (dragButton == 2) {
            camera.rotate(deltaX * DRAG_YAW, -deltaY * DRAG_PITCH);
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
        if (view.isEditing()) {
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
        if (camera != null && keyCode == Keyboard.KEY_R) {
            camera.reset(view.getColony());
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_G) {
            view.sendCommand(PacketCitizenCommand.GUARD, false, 0, 0, 0);
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_C) {
            view.sendCommand(PacketCitizenCommand.CANCEL, false, 0, 0, 0);
            return Result.SUCCESS;
        }
        if (Interactable.isKeyComboCtrlA(keyCode)) {
            view.selectAll();
            return Result.SUCCESS;
        }
        if (GCKeyBindings.openColony != null && keyCode == GCKeyBindings.openColony.getKeyCode()) {
            getPanel().closeIfOpen();
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }
}
