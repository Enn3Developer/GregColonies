package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandChop;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandFarm;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class ColonyViewWidget extends Widget<ColonyViewWidget> implements Interactable {

    private static final int DRAG_PICK_INTERVAL = 2;

    private static final int DATA_INTERVAL = 20;

    private static final float ZOOM_STEP = 1.2F;

    private static final float ROTATE_STEP = 45.0F;

    private static final float DRAG_YAW = 0.4F;

    private static final float DRAG_PITCH = 0.3F;

    private static final float KEY_PAN_PIXELS = 12.0F;

    private static final int CLICK_SLOP = 4;

    private static final long DOUBLE_CLICK_MS = 400L;

    private static final double HIT_RADIUS = 12.0D;

    private static final double DEFAULT_FOV = 70.0D;

    private static final double MARKER_HEIGHT = 2.4D;

    private static final float MARKER_SIZE = 4.5F;

    private static final float MARKER_HOVER_SIZE = 7.0F;

    private static final float MARKER_RING = 1.5F;

    private static final float SELECTED_RING = 5.0F;

    private static final float SELECTED_HALO = 3.5F;

    private static final float OFFLINE_FRACTION = 0.45F;

    private static final int MARKER_OUTLINE = 0x99000000;

    private static final int SELECTED_OUTLINE = 0xFFFFFFFF;

    private static final int LABEL_BACKGROUND = 0xA0060810;

    private static final int LABEL_COLOR = 0xFFE8ECF4;

    private static final float LABEL_SCALE = 0.75F;

    private static final int LABEL_LIMIT = 8;

    private static final int BOX_FILL = 0x2033CCFF;

    private static final int BOX_EDGE = 0xC033CCFF;

    private final ColonyView view;

    private final double[] projected = new double[3];

    private int dataTicks;

    private int dragX;

    private int dragY;

    private int dragButton = -1;

    private int dragTravel;

    private java.util.UUID lastClicked;

    private long lastClickTime;

    private int boxStartX;

    private int boxStartY;

    private boolean boxSelecting;

    private boolean areaDragging;

    private int areaCornerX;

    private int areaCornerZ;

    private boolean hasAreaCorner;

    private int areaPickX;

    private int areaPickY;

    private int areaPickTicks;

    private int areaAnchorX;

    private int areaAnchorY;

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
            String name = citizen.getName()
                .isEmpty() ? "Citizen" : citizen.getName();
            String gender = citizen.describeGender();
            String status = gender.isEmpty() ? group : gender + ", " + group;
            tooltip.add(
                IKey.str(name)
                    .style(EnumChatFormatting.WHITE))
                .newLine();
            tooltip
                .add(
                    IKey.str(
                        status + "   "
                            + String.format("%.0f / %.0f / %.0f", citizen.getX(), citizen.getY(), citizen.getZ()))
                        .style(EnumChatFormatting.GRAY))
                .newLine();
            if (!citizen.isLoaded()) {
                tooltip.add(
                    IKey.str("last seen here, chunk not loaded")
                        .style(EnumChatFormatting.DARK_GRAY))
                    .newLine();
                return;
            }
            tooltip
                .add(
                    IKey.str(
                        String.format("health %.1f / %.1f", citizen.getHealth(), citizen.getMaxHealth()) + "   food "
                            + citizen.getFoodLevel()
                            + " / 20")
                        .style(EnumChatFormatting.GRAY))
                .newLine();
            String task = citizen.getTask()
                .isEmpty() ? "idle" : citizen.getTask();
            tooltip.add(
                IKey.str(task)
                    .style(EnumChatFormatting.AQUA))
                .newLine();
            if (citizen.getPendingCount() > 0) {
                tooltip.add(
                    IKey.str(citizen.getPendingCount() + " queued")
                        .style(EnumChatFormatting.DARK_GRAY))
                    .newLine();
            }
        });
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        CitizenSnapshot hovered = findHovered();
        boolean labelSelection = view.getSelection()
            .size() <= LABEL_LIMIT;
        for (CitizenSnapshot citizen : view.getColony()
            .getCitizens()) {
            if (!projectCitizen(citizen)) {
                continue;
            }
            boolean selected = view.isSelected(citizen.getId());
            float size = citizen == hovered ? MARKER_HOVER_SIZE : MARKER_SIZE;
            int color = ColonyWorldOverlay.groupColor(citizen.getGroup()) | 0xFF000000;
            float ring = size + (selected ? SELECTED_RING : MARKER_RING);
            float x = (float) projected[0];
            float y = (float) projected[1];
            if (view.isOverChrome((int) x, (int) y)) {
                continue;
            }
            if (selected) {
                drawDot(x, y, ring, MARKER_OUTLINE);
                drawDot(x, y, size + SELECTED_HALO, SELECTED_OUTLINE);
            }
            drawDot(x, y, size + MARKER_RING, MARKER_OUTLINE);
            if (citizen.isLoaded()) {
                drawDot(x, y, size, color);
            } else {
                drawDot(x, y, size, 0xC0121722);
                drawDot(x, y, size * OFFLINE_FRACTION, color);
            }
            if (citizen == hovered || selected && labelSelection) {
                drawLabel(citizen.getName(), x, y + ring / 2.0F + 4.0F, citizen == hovered ? LABEL_COLOR : color);
            }
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

    private static void drawDot(float x, float y, float size, int color) {
        GuiDraw.drawEllipse(x - size / 2.0F, y - size / 2.0F, size, size, color, color, 12);
    }

    private static void drawLabel(String text, float x, float y, int color) {
        if (text.isEmpty()) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float width = font.getStringWidth(text);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(LABEL_SCALE, LABEL_SCALE, 1.0F);
        GuiDraw.drawRect(-width / 2.0F - 2.0F, -1.0F, width + 4.0F, font.FONT_HEIGHT + 1.0F, LABEL_BACKGROUND);
        font.drawStringWithShadow(text, (int) (-width / 2.0F), 0, color);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
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
        if (mode == ColonyView.TARGET_MINE) {
            pickChunk();
            return;
        }
        if (isSpotMode(mode)) {
            pickBlock();
            return;
        }
        if (areaDragging) {
            updateDragArea(false);
            return;
        }
        pickBlock();
    }

    private void pickBlock() {
        MovingObjectPosition hit = ColonyWorldOverlay.pick(getContext().getMouseX(), getContext().getMouseY());
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            view.clearPending();
            return;
        }
        view.setPending(hit.blockX, hit.blockY, hit.blockZ, hit.blockX, hit.blockY, hit.blockZ);
    }

    private void pickChunk() {
        MovingObjectPosition hit = ColonyWorldOverlay.pick(getContext().getMouseX(), getContext().getMouseY());
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            view.clearPending();
            return;
        }
        int chunkX = hit.blockX >> 4;
        int chunkZ = hit.blockZ >> 4;
        view.setPending(chunkX << 4, hit.blockY, chunkZ << 4, (chunkX << 4) + 15, hit.blockY, (chunkZ << 4) + 15);
    }

    private void updateDragArea(boolean force) {
        int screenX = getContext().getMouseX();
        int screenY = getContext().getMouseY();
        if (force || areaPickTicks >= DRAG_PICK_INTERVAL && (screenX != areaPickX || screenY != areaPickY)) {
            MovingObjectPosition hit = ColonyWorldOverlay.pick(screenX, screenY);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                areaCornerX = hit.blockX;
                areaCornerZ = hit.blockZ;
                hasAreaCorner = true;
            }
            areaPickX = screenX;
            areaPickY = screenY;
            areaPickTicks = 0;
        }
        if (!hasAreaCorner) {
            return;
        }
        int x = clampSide(areaAnchorX, areaCornerX);
        int z = clampSide(areaAnchorZ, areaCornerZ);
        view.setPending(
            Math.min(areaAnchorX, x),
            areaAnchorY,
            Math.min(areaAnchorZ, z),
            Math.max(areaAnchorX, x),
            areaAnchorY,
            Math.max(areaAnchorZ, z));
    }

    private int clampSide(int anchor, int value) {
        int side = view.getTargeting() == ColonyView.TARGET_FARM ? CitizenCommandFarm.MAX_SIDE
            : CitizenCommandChop.MAX_SIDE;
        if (value - anchor > side - 1) {
            return anchor + side - 1;
        }
        if (anchor - value > side - 1) {
            return anchor - side + 1;
        }
        return value;
    }

    private void issueArea() {
        if (!view.hasPending()) {
            return;
        }
        int[] area = view.getPending();
        if (view.getTargeting() == ColonyView.TARGET_DROP_OFF) {
            view.sendDropOff(area[0], area[1], area[2]);
            view.setTargeting(ColonyView.TARGET_NONE);
            return;
        }
        if (view.getTargeting() == ColonyView.TARGET_PICK_UP) {
            view.sendPickUp(area[0], area[1], area[2]);
            view.setTargeting(ColonyView.TARGET_NONE);
            return;
        }
        if (view.getTargeting() == ColonyView.TARGET_MATERIALS) {
            view.sendMaterials(area[0], area[1], area[2]);
            view.setTargeting(ColonyView.TARGET_NONE);
            return;
        }
        if (view.getTargeting() == ColonyView.TARGET_BUILD) {
            view.sendBuild(area[0], area[1], area[2]);
            view.setTargeting(ColonyView.TARGET_NONE);
            return;
        }
        view.sendArea(
            areaAction(view.getTargeting()),
            Interactable.hasShiftDown(),
            area[0],
            area[1],
            area[2],
            area[3],
            area[4],
            area[5]);
        view.setTargeting(ColonyView.TARGET_NONE);
    }

    private static boolean isSpotMode(int mode) {
        return mode == ColonyView.TARGET_DROP_OFF || mode == ColonyView.TARGET_PICK_UP
            || mode == ColonyView.TARGET_MATERIALS
            || mode == ColonyView.TARGET_BUILD;
    }

    private static byte areaAction(int targeting) {
        if (targeting == ColonyView.TARGET_MINE) {
            return PacketCitizenCommand.MINE;
        }
        return targeting == ColonyView.TARGET_FARM ? PacketCitizenCommand.FARM : PacketCitizenCommand.CHOP;
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
        areaPickTicks++;
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
            lastClicked = null;
            if (!Interactable.hasShiftDown()) {
                view.clearSelection();
            }
            return;
        }
        if (Interactable.hasShiftDown()) {
            lastClicked = null;
            view.toggle(citizen.getId());
            return;
        }
        long now = System.currentTimeMillis();
        boolean again = citizen.getId()
            .equals(lastClicked) && now - lastClickTime <= DOUBLE_CLICK_MS;
        lastClicked = citizen.getId();
        lastClickTime = now;
        view.clearSelection();
        view.getSelection()
            .add(citizen.getId());
        if (again) {
            lastClicked = null;
            view.openCitizen(citizen);
        }
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
                areaAnchorY = hit.blockY;
                areaAnchorZ = hit.blockZ;
                areaCornerX = hit.blockX;
                areaCornerZ = hit.blockZ;
                hasAreaCorner = true;
                areaPickX = getContext().getMouseX();
                areaPickY = getContext().getMouseY();
                areaPickTicks = 0;
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
            int mode = view.getTargeting();
            if (mode == ColonyView.TARGET_MINE) {
                pickChunk();
            } else if (isSpotMode(mode)) {
                pickBlock();
            } else {
                updateDragArea(true);
            }
            areaDragging = false;
            hasAreaCorner = false;
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
        if (keyCode == Keyboard.KEY_I) {
            view.openCitizen(view.getSingleSelected());
            return Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_H) {
            view.toggleHelp();
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
