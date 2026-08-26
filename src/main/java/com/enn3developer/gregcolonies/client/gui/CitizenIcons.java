package com.enn3developer.gregcolonies.client.gui;

import java.util.function.BooleanSupplier;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;

public final class CitizenIcons {

    private static final float GHOST_ALPHA = 0.35F;

    private static final int ICON_SIZE = 16;

    private CitizenIcons() {}

    public static IDrawable armor(int armorType, BooleanSupplier visible) {
        return (context, x, y, width, height, theme) -> {
            if (visible.getAsBoolean()) {
                draw(ItemArmor.func_94602_b(armorType), x, y, width, height, 1.0F);
            }
        };
    }

    public static IDrawable item(Item item, BooleanSupplier visible) {
        return (context, x, y, width, height, theme) -> {
            if (visible.getAsBoolean()) {
                draw(item.getIconFromDamage(0), x, y, width, height, GHOST_ALPHA);
            }
        };
    }

    private static void draw(IIcon icon, int x, int y, int width, int height, float alpha) {
        if (icon == null) {
            return;
        }
        int size = Math.min(ICON_SIZE, Math.min(width, height));
        float left = x + (width - size) / 2.0F;
        float top = y + (height - size) / 2.0F;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        GuiDraw.drawTexture(
            TextureMap.locationItemsTexture,
            left,
            top,
            left + size,
            top + size,
            icon.getMinU(),
            icon.getMinV(),
            icon.getMaxU(),
            icon.getMaxV(),
            true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
