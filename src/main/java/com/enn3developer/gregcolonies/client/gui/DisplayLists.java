package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

public final class DisplayLists {

    private DisplayLists() {}

    public static int allocate(int count) {
        return GLAllocation.generateDisplayLists(count);
    }

    public static void free(int base, int count) {
        if (base < 0) {
            return;
        }
        try {
            GLAllocation.deleteDisplayLists(base);
        } catch (RuntimeException error) {
            GL11.glDeleteLists(base, count);
        }
    }

    public static boolean compileQuads(int list, String subject, Runnable body) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        try {
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            body.run();
            tessellator.draw();
            return true;
        } catch (RuntimeException error) {
            GLScope.warn(subject, error);
            return false;
        } finally {
            GL11.glEndList();
        }
    }
}
