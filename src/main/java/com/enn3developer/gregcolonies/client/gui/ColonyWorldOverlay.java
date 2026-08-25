package com.enn3developer.gregcolonies.client.gui;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ColonyWorldOverlay {

    private static final int TERRITORY_COLOR = 0x8033CCFF;

    private static final int CENTER_COLOR = 0xC0FFCC44;

    private static final int UNGROUPED_COLOR = 0xC0D0D4DC;

    private static final int OFFLINE_ALPHA = 0x50;

    private static final double CITIZEN_RADIUS = 0.55D;

    private static final double CITIZEN_THICKNESS = 0.09D;

    private static final double CENTER_RADIUS = 1.6D;

    private static final double SELECTION_RADIUS = 0.85D;

    private static final double SELECTION_THICKNESS = 0.07D;

    private static final int SELECTION_COLOR = 0xE0FFFFFF;

    private static final double TERRITORY_THICKNESS = 0.35D;

    private static final double GROUND_OFFSET = 0.06D;

    private static final int CIRCLE_SEGMENTS = 48;

    private static final int TERRITORY_SEGMENTS = 160;

    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);

    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);

    private static final FloatBuffer PROJECTED = BufferUtils.createFloatBuffer(3);

    private static final FloatBuffer UNPROJECTED = BufferUtils.createFloatBuffer(3);

    private static final double PICK_RANGE = 512.0D;

    private static boolean matricesValid;

    private static double cameraX;

    private static double cameraY;

    private static double cameraZ;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL && ColonyScreen.getOpen() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (ColonyScreen.getOpen() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        ColonyScreen screen = ColonyScreen.getOpen();
        if (screen == null) {
            return;
        }
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return;
        }
        cameraX = camera.posX;
        cameraY = camera.posY;
        cameraZ = camera.posZ;

        MODELVIEW.clear();
        PROJECTION.clear();
        VIEWPORT.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        matricesValid = true;

        ColonySnapshot colony = screen.getView()
            .getColony();

        GL11.glPushMatrix();
        GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        double centerX = colony.getX() + 0.5D;
        double centerY = colony.getY() + GROUND_OFFSET;
        double centerZ = colony.getZ() + 0.5D;
        drawRing(
            centerX,
            centerY,
            centerZ,
            colony.getRadius(),
            TERRITORY_THICKNESS,
            TERRITORY_COLOR,
            TERRITORY_SEGMENTS);
        drawRing(centerX, centerY, centerZ, CENTER_RADIUS, CITIZEN_THICKNESS * 2.0D, CENTER_COLOR, CIRCLE_SEGMENTS);

        for (CitizenSnapshot citizen : colony.getCitizens()) {
            Entity entity = liveEntity(citizen);
            double x = entity == null ? citizen.getX() : entity.posX;
            double y = (entity == null ? citizen.getY() : entity.posY) + GROUND_OFFSET;
            double z = entity == null ? citizen.getZ() : entity.posZ;
            int color = groupColor(citizen.getGroup());
            if (entity == null) {
                color = color & 0x00FFFFFF | OFFLINE_ALPHA << 24;
            }
            drawRing(x, y, z, CITIZEN_RADIUS, CITIZEN_THICKNESS, color, CIRCLE_SEGMENTS);
            if (screen.getView()
                .isSelected(citizen.getId())) {
                drawRing(x, y, z, SELECTION_RADIUS, SELECTION_THICKNESS, SELECTION_COLOR, CIRCLE_SEGMENTS);
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    public static Entity liveEntity(CitizenSnapshot citizen) {
        if (!citizen.isLoaded() || Minecraft.getMinecraft().theWorld == null) {
            return null;
        }
        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(citizen.getEntityId());
        return entity instanceof EntityCitizen ? entity : null;
    }

    public static int groupColor(String group) {
        if (group == null || group.isEmpty()) {
            return UNGROUPED_COLOR;
        }
        int hash = group.hashCode();
        float hue = (float) (((hash % 360) + 360) % 360) / 360.0F;
        return 0xC0000000 | java.awt.Color.HSBtoRGB(hue, 0.6F, 1.0F) & 0xFFFFFF;
    }

    public static boolean project(double x, double y, double z, double[] out) {
        if (!matricesValid) {
            return false;
        }
        PROJECTED.clear();
        if (!GLU.gluProject(
            (float) (x - cameraX),
            (float) (y - cameraY),
            (float) (z - cameraZ),
            MODELVIEW,
            PROJECTION,
            VIEWPORT,
            PROJECTED)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        double scale = resolution.getScaleFactor();
        out[0] = PROJECTED.get(0) / scale;
        out[1] = (minecraft.displayHeight - PROJECTED.get(1)) / scale;
        out[2] = PROJECTED.get(2);
        return out[2] > 0.0D && out[2] < 1.0D;
    }

    public static void invalidate() {
        matricesValid = false;
    }

    public static MovingObjectPosition pick(double guiX, double guiY) {
        Vec3 near = unProject(guiX, guiY, 0.0F);
        Vec3 far = unProject(guiX, guiY, 1.0F);
        if (near == null || far == null || Minecraft.getMinecraft().theWorld == null) {
            return null;
        }
        Vec3 direction = Vec3
            .createVectorHelper(far.xCoord - near.xCoord, far.yCoord - near.yCoord, far.zCoord - near.zCoord)
            .normalize();
        Vec3 end = Vec3.createVectorHelper(
            near.xCoord + direction.xCoord * PICK_RANGE,
            near.yCoord + direction.yCoord * PICK_RANGE,
            near.zCoord + direction.zCoord * PICK_RANGE);
        return Minecraft.getMinecraft().theWorld.rayTraceBlocks(near, end);
    }

    private static Vec3 unProject(double guiX, double guiY, float winZ) {
        if (!matricesValid) {
            return null;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        float scale = resolution.getScaleFactor();
        UNPROJECTED.clear();
        if (!GLU.gluUnProject(
            (float) (guiX * scale),
            (float) (minecraft.displayHeight - guiY * scale),
            winZ,
            MODELVIEW,
            PROJECTION,
            VIEWPORT,
            UNPROJECTED)) {
            return null;
        }
        return Vec3.createVectorHelper(
            UNPROJECTED.get(0) + cameraX,
            UNPROJECTED.get(1) + cameraY,
            UNPROJECTED.get(2) + cameraZ);
    }

    private static void drawRing(double x, double y, double z, double radius, double thickness, int color,
        int segments) {
        float alpha = (color >>> 24) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        double inner = Math.max(0.0D, radius - thickness);
        double outer = radius + thickness;
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            GL11.glVertex3d(x + sin * inner, y, z + cos * inner);
            GL11.glVertex3d(x + sin * outer, y, z + cos * outer);
        }
        GL11.glEnd();
    }
}
