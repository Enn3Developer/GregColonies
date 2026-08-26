package com.enn3developer.gregcolonies.client.gui;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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

    private static final int CHOP_COLOR = 0xB07CE07C;

    private static final int MINE_COLOR = 0xB0FFB040;

    private static final int DROP_OFF_COLOR = 0xB0FF7CE0;

    private static final int DROP_OFF_MARK_COLOR = 0x70FF7CE0;

    private static final int PICK_UP_COLOR = 0xB07CE0FF;

    private static final int PICK_UP_MARK_COLOR = 0x707CE0FF;

    private static final double AREA_HEIGHT = 4.0D;

    private static final double AREA_EDGE = 0.3D;

    private static final double TERRITORY_THICKNESS = 0.35D;

    private static final double GROUND_OFFSET = 0.06D;

    private static final int CIRCLE_SEGMENTS = 48;

    private static final int TERRITORY_SEGMENTS = 160;

    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);

    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);

    private static final FloatBuffer PROJECTED = BufferUtils.createFloatBuffer(3);

    private static final FloatBuffer UNPROJECTED = BufferUtils.createFloatBuffer(3);

    private static final double PICK_RANGE = 640.0D;

    private static final double PICK_STEP = 1.0D;

    private static final double PICK_REFINE = 0.05D;

    private static boolean matricesValid;

    private static double cameraX;

    private static double cameraY;

    private static double cameraZ;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (ColonyScreen.getOpen() == null || event.type == RenderGameOverlayEvent.ElementType.ALL
            || event.type == RenderGameOverlayEvent.ElementType.CHAT) {
            return;
        }
        event.setCanceled(true);
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
        GL11.glDisable(GL11.GL_CULL_FACE);
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

        if (colony.hasDropOff()) {
            drawArea(
                colony.getDropOffX(),
                colony.getDropOffY(),
                colony.getDropOffZ(),
                colony.getDropOffX() + 1,
                colony.getDropOffZ() + 1,
                DROP_OFF_MARK_COLOR);
        }

        if (colony.hasPickUp()) {
            drawArea(
                colony.getPickUpX(),
                colony.getPickUpY(),
                colony.getPickUpZ(),
                colony.getPickUpX() + 1,
                colony.getPickUpZ() + 1,
                PICK_UP_MARK_COLOR);
        }

        ColonyView view = screen.getView();
        if (view.hasPending()) {
            int[] area = view.getPending();
            drawArea(area[0], area[1], area[2], area[3] + 1, area[5] + 1, targetColor(view.getTargeting()));
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private static int targetColor(int targeting) {
        if (targeting == ColonyView.TARGET_MINE) {
            return MINE_COLOR;
        }
        if (targeting == ColonyView.TARGET_DROP_OFF) {
            return DROP_OFF_COLOR;
        }
        return targeting == ColonyView.TARGET_PICK_UP ? PICK_UP_COLOR : CHOP_COLOR;
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
        World world = Minecraft.getMinecraft().theWorld;
        Vec3 near = unProject(guiX, guiY, 0.0F);
        Vec3 far = unProject(guiX, guiY, 1.0F);
        if (near == null || far == null || world == null) {
            return null;
        }
        Vec3 direction = Vec3
            .createVectorHelper(far.xCoord - near.xCoord, far.yCoord - near.yCoord, far.zCoord - near.zCoord)
            .normalize();

        double hit = -1.0D;
        for (double distance = 0.0D; distance <= PICK_RANGE; distance += PICK_STEP) {
            if (isPickable(world, near, direction, distance)) {
                hit = distance;
                break;
            }
        }
        if (hit < 0.0D) {
            return null;
        }
        for (double distance = hit - PICK_STEP + PICK_REFINE; distance < hit; distance += PICK_REFINE) {
            if (isPickable(world, near, direction, distance)) {
                hit = distance;
                break;
            }
        }

        double x = near.xCoord + direction.xCoord * hit;
        double y = near.yCoord + direction.yCoord * hit;
        double z = near.zCoord + direction.zCoord * hit;
        return new MovingObjectPosition(
            MathHelper.floor_double(x),
            MathHelper.floor_double(y),
            MathHelper.floor_double(z),
            1,
            Vec3.createVectorHelper(x, y, z));
    }

    private static boolean isPickable(World world, Vec3 origin, Vec3 direction, double distance) {
        int y = MathHelper.floor_double(origin.yCoord + direction.yCoord * distance);
        if (y < 0 || y >= world.getHeight()) {
            return false;
        }
        int x = MathHelper.floor_double(origin.xCoord + direction.xCoord * distance);
        int z = MathHelper.floor_double(origin.zCoord + direction.zCoord * distance);
        if (!world.blockExists(x, y, z)) {
            return false;
        }
        Block block = world.getBlock(x, y, z);
        return block != null && !block.isAir(world, x, y, z)
            && block.getMaterial()
                .blocksMovement();
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

    private static void drawArea(double x0, double y, double z0, double x1, double z1, int color) {
        float alpha = (color >>> 24) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        double ground = y + GROUND_OFFSET;
        double top = ground + AREA_HEIGHT;

        GL11.glBegin(GL11.GL_QUADS);
        drawWall(x0, z0, x1, z0, ground, top, red, green, blue, alpha);
        drawWall(x1, z0, x1, z1, ground, top, red, green, blue, alpha);
        drawWall(x1, z1, x0, z1, ground, top, red, green, blue, alpha);
        drawWall(x0, z1, x0, z0, ground, top, red, green, blue, alpha);

        GL11.glColor4f(red, green, blue, alpha);
        drawGroundQuad(x0, z0, x1, z0 + AREA_EDGE, ground);
        drawGroundQuad(x0, z1 - AREA_EDGE, x1, z1, ground);
        drawGroundQuad(x0, z0, x0 + AREA_EDGE, z1, ground);
        drawGroundQuad(x1 - AREA_EDGE, z0, x1, z1, ground);
        GL11.glEnd();
    }

    private static void drawWall(double x0, double z0, double x1, double z1, double bottom, double top, float red,
        float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glVertex3d(x0, bottom, z0);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glVertex3d(x1, bottom, z1);
        GL11.glColor4f(red, green, blue, 0.0F);
        GL11.glVertex3d(x1, top, z1);
        GL11.glColor4f(red, green, blue, 0.0F);
        GL11.glVertex3d(x0, top, z0);
    }

    private static void drawGroundQuad(double x0, double z0, double x1, double z1, double y) {
        GL11.glVertex3d(x0, y, z0);
        GL11.glVertex3d(x0, y, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x1, y, z0);
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
