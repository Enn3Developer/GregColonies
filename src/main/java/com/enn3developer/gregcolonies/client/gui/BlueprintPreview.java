package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Blueprint;

public class BlueprintPreview extends Widget<BlueprintPreview> implements Interactable {

    private static final int BACKGROUND = 0xFF090C16;

    private static final int BORDER = 0xFF33405C;

    private static final int UNKNOWN_COLOR = 0xFF808080;

    private static final float DEFAULT_YAW = 45.0F;

    private static final float DEFAULT_PITCH = 30.0F;

    private static final float MIN_PITCH = -89.0F;

    private static final float MAX_PITCH = 89.0F;

    private static final float DRAG_SPEED = 0.6F;

    private static final float ZOOM_STEP = 1.15F;

    private static final float MIN_ZOOM = 0.35F;

    private static final float MAX_ZOOM = 4.0F;

    private static final float FILL = 0.92F;

    private static final float OPAQUE = 1.0F;

    private static final double RAY_BACKOFF = 512.0D;

    private static final int OCTANT_BITS = 3;

    private static int list = -1;

    private static int builtKey;

    private static boolean built;

    private static boolean warned;

    private final BlueprintView view;

    private float yaw = DEFAULT_YAW;

    private float pitch = DEFAULT_PITCH;

    private float zoom = 1.0F;

    private int dragX;

    private int dragY;

    private boolean dragging;

    private int hovered = Blueprint.AIR;

    public BlueprintPreview(BlueprintView view) {
        this.view = view;
    }

    public int getHovered() {
        return hovered;
    }

    public void reset() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = 1.0F;
        built = false;
    }

    public static void forget() {
        if (list >= 0) {
            try {
                GLAllocation.deleteDisplayLists(list);
            } catch (RuntimeException error) {
                GL11.glDeleteLists(list, 1);
            }
            list = -1;
        }
        built = false;
    }

    private static void warnOnce(RuntimeException error) {
        if (!warned) {
            warned = true;
            GregColonies.LOG.error("Blueprint preview failed to render", error);
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Area area = getArea();
        int width = area.width;
        int height = area.height;
        GuiDraw.drawRect(0, 0, width, height, BACKGROUND);
        GuiDraw.drawRect(0, 0, width, 1, BORDER);
        GuiDraw.drawRect(0, height - 1, width, 1, BORDER);
        GuiDraw.drawRect(0, 0, 1, height, BORDER);
        GuiDraw.drawRect(width - 1, 0, 1, height, BORDER);
        hovered = Blueprint.AIR;

        Blueprint model = view.getPlaced();
        if (model == null) {
            return;
        }
        int top = Math.max(0, Math.min(view.getLayer(), model.getSizeY() - 1));
        float scale = scale(model, width, height);
        double[] look = look();

        if (isHovering()) {
            pick(model, top, look, width, height, scale, area);
        }
        Stencil.applyTransformed(1, 1, width - 2, height - 2);
        try {
            render(model, top, look, width / 2.0F, height / 2.0F, scale);
        } catch (RuntimeException error) {
            warnOnce(error);
        } finally {
            Stencil.remove();
        }
    }

    private float scale(Blueprint model, int width, int height) {
        double span = Math.sqrt(
            model.getSizeX() * model.getSizeX() + model.getSizeY() * (double) model.getSizeY()
                + model.getSizeZ() * (double) model.getSizeZ());
        return (float) (Math.min(width, height) / Math.max(1.0D, span)) * zoom;
    }

    private double[] look() {
        double radiansPitch = Math.toRadians(pitch);
        double radiansYaw = Math.toRadians(yaw);
        double cosPitch = Math.cos(radiansPitch);
        double sinPitch = Math.sin(radiansPitch);
        double cosYaw = Math.cos(radiansYaw);
        double sinYaw = Math.sin(radiansYaw);
        return new double[] { cosYaw, 0.0D, sinYaw, sinPitch * sinYaw, cosPitch, -sinPitch * cosYaw, cosPitch * sinYaw,
            -sinPitch, -cosPitch * cosYaw };
    }

    private void pick(Blueprint model, int top, double[] look, int width, int height, float scale, Area area) {
        if (scale <= 0.0F) {
            return;
        }
        double offsetX = (getContext().getMouseX() - area.x - width / 2.0D) / scale;
        double offsetY = -(getContext().getMouseY() - area.y - height / 2.0D) / scale;
        double centreX = model.getSizeX() / 2.0D;
        double centreY = model.getSizeY() / 2.0D;
        double centreZ = model.getSizeZ() / 2.0D;
        double originX = centreX + look[0] * offsetX + look[3] * offsetY - look[6] * RAY_BACKOFF;
        double originY = centreY + look[1] * offsetX + look[4] * offsetY - look[7] * RAY_BACKOFF;
        double originZ = centreZ + look[2] * offsetX + look[5] * offsetY - look[8] * RAY_BACKOFF;

        BlueprintTrace.Hit hit = BlueprintTrace
            .trace(model, top, -1, originX, originY, originZ, look[6], look[7], look[8]);
        if (hit != null && hit.solid) {
            hovered = model.cellAt(hit.hitX, hit.hitY, hit.hitZ);
        }
    }

    private void render(Blueprint model, int top, double[] look, float centreX, float centreY, float scale) {
        compile(model, top, look);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glPushMatrix();
        try {
            paint3D(model, top, look, centreX, centreY, scale);
        } finally {
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void paint3D(Blueprint model, int top, double[] look, float centreX, float centreY, float scale) {
        GL11.glTranslatef(centreX, centreY, 0.0F);
        GL11.glScalef(scale, -scale, scale);
        GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-model.getSizeX() / 2.0F, -model.getSizeY() / 2.0F, -model.getSizeZ() / 2.0F);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        GL11.glCallList(list);
    }

    private void compile(Blueprint model, int top, double[] look) {
        int octant = (look[6] > 0.0D ? 1 : 0) | (look[7] > 0.0D ? 2 : 0) | (look[8] > 0.0D ? 4 : 0);
        int key = ((view.getRevision() * 31 + top) << OCTANT_BITS) | octant;
        if (list >= 0 && built && builtKey == key) {
            return;
        }
        if (list < 0) {
            list = GLAllocation.generateDisplayLists(1);
        }
        builtKey = key;
        built = true;
        GL11.glNewList(list, GL11.GL_COMPILE);
        try {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            paint(model, top, look, tessellator);
            tessellator.draw();
        } catch (RuntimeException error) {
            built = false;
            warnOnce(error);
        } finally {
            GL11.glEndList();
        }
    }

    private void paint(Blueprint model, int top, double[] look, Tessellator tessellator) {
        boolean downX = look[6] > 0.0D;
        boolean downY = look[7] > 0.0D;
        boolean downZ = look[8] > 0.0D;
        for (int stepY = 0; stepY <= top; stepY++) {
            int y = downY ? top - stepY : stepY;
            for (int stepZ = 0; stepZ < model.getSizeZ(); stepZ++) {
                int z = downZ ? model.getSizeZ() - 1 - stepZ : stepZ;
                for (int stepX = 0; stepX < model.getSizeX(); stepX++) {
                    int x = downX ? model.getSizeX() - 1 - stepX : stepX;
                    cube(model, top, look, tessellator, x, y, z);
                }
            }
        }
    }

    private void cube(Blueprint model, int top, double[] look, Tessellator tessellator, int x, int y, int z) {
        int cell = model.cellAt(x, y, z);
        if (cell == Blueprint.AIR) {
            return;
        }
        Block block = model.blockOf(cell);
        if (block == null) {
            return;
        }
        int meta = Blueprint.metaOf(cell);
        for (int side = 0; side < BlueprintMesh.SIDES; side++) {
            int[] step = BlueprintMesh.OFFSETS[side];
            if (step[0] * look[6] + step[1] * look[7] + step[2] * look[8] >= 0.0D) {
                continue;
            }
            int neighbourY = y + step[1];
            if (neighbourY <= top && model.cellAt(x + step[0], neighbourY, z + step[2]) != Blueprint.AIR) {
                continue;
            }
            boolean cut = top < model.getSizeY() - 1 && y == top;
            BlueprintMesh.face(tessellator, block, meta, side, x, y, z, cut ? FILL : OPAQUE);
        }
    }

    public static int cellColor(Blueprint blueprint, int value) {
        Block block = blueprint.blockOf(value);
        if (block == null) {
            return UNKNOWN_COLOR;
        }
        try {
            MapColor mapColor = block.getMapColor(Blueprint.metaOf(value));
            return mapColor == null ? UNKNOWN_COLOR : 0xFF000000 | mapColor.colorValue;
        } catch (RuntimeException error) {
            return UNKNOWN_COLOR;
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (Interactable.hasShiftDown()) {
            view.stepLayer(scrollDirection == UpOrDown.UP ? 1 : -1);
            return true;
        }
        zoom = Math
            .max(MIN_ZOOM, Math.min(MAX_ZOOM, scrollDirection == UpOrDown.UP ? zoom * ZOOM_STEP : zoom / ZOOM_STEP));
        return true;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        dragging = true;
        dragX = getContext().getAbsMouseX();
        dragY = getContext().getAbsMouseY();
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        dragging = false;
        return true;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (!dragging) {
            return;
        }
        int mouseX = getContext().getAbsMouseX();
        int mouseY = getContext().getAbsMouseY();
        yaw += (mouseX - dragX) * DRAG_SPEED;
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + (mouseY - dragY) * DRAG_SPEED));
        dragX = mouseX;
        dragY = mouseY;
    }

    @Override
    public boolean canHover() {
        return true;
    }
}
