package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;

import org.lwjgl.opengl.GL11;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Blueprint;

public final class BlueprintGhost {

    private static final float ALPHA = 0.92F;

    private static final int BOUNDS_COLOR = 0xFF3FA9F5;

    private static final int PLANE_COLOR = 0x552FE0C0;

    private static final int PLANE_EDGE = 0xAA2FE0C0;

    private static final int PLACE_COLOR = 0xFFFFFFFF;

    private static final int ERASE_COLOR = 0xFFFF5050;

    private static final int ANCHOR_COLOR = 0xFFFFC46B;

    private static final float LINE_WIDTH = 2.0F;

    private static final double SWELL = 0.002D;

    private static final int ANCHOR_MARK_COLOR = 0xFFFFE45C;

    private static int listBase = -1;

    private static int listCount;

    private static int[] built = new int[0];

    private static int builtCeiling = -1;

    private static boolean warned;

    private BlueprintGhost() {}

    public static void forget() {
        BlueprintMesh.forget();
        if (listBase >= 0) {
            try {
                GLAllocation.deleteDisplayLists(listBase);
            } catch (RuntimeException error) {
                GL11.glDeleteLists(listBase, listCount);
            }
            listBase = -1;
            listCount = 0;
        }
        built = new int[0];
        builtCeiling = -1;
    }

    private static void warnOnce(RuntimeException error) {
        if (!warned) {
            warned = true;
            GregColonies.LOG.error("Blueprint ghost failed to render", error);
        }
    }

    public static void render(BlueprintEditor editor, BlueprintTrace.Hit hover, double cameraX, double cameraY,
        double cameraZ) {
        Blueprint model = editor.getModel();
        if (model == null) {
            return;
        }
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
            drawBlocks(editor, model);
            drawGuides(editor, model, hover);
        } catch (RuntimeException error) {
            warnOnce(error);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static void drawBlocks(BlueprintEditor editor, Blueprint model) {
        int top = editor.ceiling();
        compile(editor, model, top);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        for (int y = 0; y <= top && y < listCount; y++) {
            GL11.glCallList(listBase + y);
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    private static void compile(BlueprintEditor editor, Blueprint model, int top) {
        int layers = model.getSizeY();
        if (listBase < 0 || listCount < layers) {
            if (listBase >= 0) {
                GLAllocation.deleteDisplayLists(listBase);
            }
            listBase = GLAllocation.generateDisplayLists(layers);
            listCount = layers;
            built = new int[layers];
        }
        if (built.length < layers) {
            built = new int[layers];
        }

        boolean ceilingMoved = builtCeiling != top;
        for (int y = 0; y < layers; y++) {
            boolean stale = built[y] != editor.layerRevision(y);
            if (ceilingMoved && (y == top || y == builtCeiling)) {
                stale = true;
            }
            if (!stale) {
                continue;
            }
            built[y] = editor.layerRevision(y);
            GL11.glNewList(listBase + y, GL11.GL_COMPILE);
            try {
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                layer(editor, model, y, top, tessellator);
                tessellator.draw();
            } catch (RuntimeException error) {
                built[y] = 0;
                warnOnce(error);
            } finally {
                GL11.glEndList();
            }
        }
        builtCeiling = top;
    }

    private static void layer(BlueprintEditor editor, Blueprint model, int y, int top, Tessellator tessellator) {
        for (int z = 0; z < model.getSizeZ(); z++) {
            for (int x = 0; x < model.getSizeX(); x++) {
                int cell = model.cellAt(x, y, z);
                if (cell == Blueprint.AIR) {
                    continue;
                }
                Block block = model.blockOf(cell);
                if (block == null) {
                    continue;
                }
                int meta = Blueprint.metaOf(cell);
                for (int side = 0; side < BlueprintMesh.SIDES; side++) {
                    int[] step = BlueprintMesh.OFFSETS[side];
                    int neighbourY = y + step[1];
                    if (neighbourY <= top && model.cellAt(x + step[0], neighbourY, z + step[2]) != Blueprint.AIR) {
                        continue;
                    }
                    BlueprintMesh.face(
                        tessellator,
                        block,
                        meta,
                        side,
                        editor.getAnchorX() + x,
                        editor.getAnchorY() + y,
                        editor.getAnchorZ() + z,
                        ALPHA);
                }
            }
        }
    }

    private static void drawGuides(BlueprintEditor editor, Blueprint model, BlueprintTrace.Hit hover) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(LINE_WIDTH);

        double baseX = editor.getAnchorX();
        double baseY = editor.getAnchorY();
        double baseZ = editor.getAnchorZ();
        drawPlane(editor, model, baseX, baseY, baseZ);
        outline(
            baseX,
            baseY,
            baseZ,
            baseX + model.getSizeX(),
            baseY + model.getSizeY(),
            baseZ + model.getSizeZ(),
            BOUNDS_COLOR);

        cell(baseX + model.getOriginX(), baseY + model.getOriginY(), baseZ + model.getOriginZ(), ANCHOR_MARK_COLOR);

        int[] anchor = editor.getBoxAnchor();
        if (anchor != null) {
            cell(baseX + anchor[0], baseY + anchor[1], baseZ + anchor[2], ANCHOR_COLOR);
        }
        if (hover != null) {
            boolean erasing = editor.getTool() == BlueprintEditor.TOOL_ERASE
                || editor.getTool() == BlueprintEditor.TOOL_PICK;
            if (erasing && hover.solid) {
                cell(baseX + hover.hitX, baseY + hover.hitY, baseZ + hover.hitZ, ERASE_COLOR);
            } else if (!erasing && model.contains(hover.placeX, hover.placeY, hover.placeZ)) {
                cell(baseX + hover.placeX, baseY + hover.placeY, baseZ + hover.placeZ, PLACE_COLOR);
            }
        }

        GL11.glLineWidth(1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawPlane(BlueprintEditor editor, Blueprint model, double baseX, double baseY, double baseZ) {
        double y = baseY + editor.getLayer() + SWELL;
        double x1 = baseX + model.getSizeX();
        double z1 = baseZ + model.getSizeZ();
        color(PLANE_COLOR);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(baseX, y, baseZ);
        GL11.glVertex3d(baseX, y, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x1, y, baseZ);
        GL11.glEnd();

        color(PLANE_EDGE);
        GL11.glBegin(GL11.GL_LINES);
        for (int x = 0; x <= model.getSizeX(); x++) {
            GL11.glVertex3d(baseX + x, y, baseZ);
            GL11.glVertex3d(baseX + x, y, z1);
        }
        for (int z = 0; z <= model.getSizeZ(); z++) {
            GL11.glVertex3d(baseX, y, baseZ + z);
            GL11.glVertex3d(x1, y, baseZ + z);
        }
        GL11.glEnd();
    }

    private static void cell(double x, double y, double z, int rgba) {
        outline(x - SWELL, y - SWELL, z - SWELL, x + 1.0D + SWELL, y + 1.0D + SWELL, z + 1.0D + SWELL, rgba);
    }

    private static void outline(double x0, double y0, double z0, double x1, double y1, double z1, int rgba) {
        color(rgba);
        GL11.glBegin(GL11.GL_LINES);
        edge(x0, y0, z0, x1, y0, z0);
        edge(x1, y0, z0, x1, y0, z1);
        edge(x1, y0, z1, x0, y0, z1);
        edge(x0, y0, z1, x0, y0, z0);
        edge(x0, y1, z0, x1, y1, z0);
        edge(x1, y1, z0, x1, y1, z1);
        edge(x1, y1, z1, x0, y1, z1);
        edge(x0, y1, z1, x0, y1, z0);
        edge(x0, y0, z0, x0, y1, z0);
        edge(x1, y0, z0, x1, y1, z0);
        edge(x1, y0, z1, x1, y1, z1);
        edge(x0, y0, z1, x0, y1, z1);
        GL11.glEnd();
    }

    private static void edge(double x0, double y0, double z0, double x1, double y1, double z1) {
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x1, y1, z1);
    }

    private static void color(int rgba) {
        GL11.glColor4f(
            (rgba >> 16 & 0xFF) / 255.0F,
            (rgba >> 8 & 0xFF) / 255.0F,
            (rgba & 0xFF) / 255.0F,
            (rgba >>> 24) / 255.0F);
    }
}
