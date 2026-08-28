package com.enn3developer.gregcolonies.client.gui;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;

import org.lwjgl.opengl.GL11;

import com.enn3developer.gregcolonies.GregColonies;

public final class GLScope {

    private static final int STATE_MASK = GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
        | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_LINE_BIT;

    private static final Set<String> WARNED = new HashSet<>();

    private GLScope() {}

    public static void run(String subject, Runnable body) {
        GL11.glPushAttrib(STATE_MASK);
        GL11.glPushMatrix();
        try {
            body.run();
        } catch (RuntimeException error) {
            warn(subject, error);
        } finally {
            // glPushAttrib does not record which texture unit is selected
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    public static void warn(String subject, RuntimeException error) {
        if (WARNED.add(subject)) {
            GregColonies.LOG.error("Failed to render " + subject, error);
        }
    }

    public static void blocks(boolean depthTest) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static void lines(float width) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(width);
    }

    public static void color(int rgba) {
        GL11.glColor4f(
            (rgba >> 16 & 0xFF) / 255.0F,
            (rgba >> 8 & 0xFF) / 255.0F,
            (rgba & 0xFF) / 255.0F,
            (rgba >>> 24) / 255.0F);
    }
}
