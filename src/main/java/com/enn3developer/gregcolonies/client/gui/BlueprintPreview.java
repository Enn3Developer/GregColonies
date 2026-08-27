package com.enn3developer.gregcolonies.client.gui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.enn3developer.gregcolonies.colony.Blueprint;

public class BlueprintPreview extends Widget<BlueprintPreview> implements Interactable {

    private static final int GRID_BACKGROUND = 0xFF090C16;

    private static final int GRID_BORDER = 0xFF33405C;

    private static final int GRID_LINE = 0x30000000;

    private static final int EMPTY_COLOR = 0xFF131A28;

    private static final int GHOST_COLOR = 0x40FFFFFF;

    private static final int NORTH_COLOR = 0xFFFF6060;

    private static final int UNKNOWN_COLOR = 0xFF808080;

    private static final int MIN_CELL = 1;

    private static final int LINE_CELL = 5;

    private static final int NORTH_BAR = 2;

    private final BlueprintView view;

    private final Map<Integer, Integer> colors = new HashMap<>();

    private int hovered = Blueprint.AIR;

    public BlueprintPreview(BlueprintView view) {
        this.view = view;
    }

    public int getHovered() {
        return hovered;
    }

    public void forgetColors() {
        colors.clear();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        Area area = getArea();
        int width = area.width;
        int height = area.height;
        GuiDraw.drawRect(0, 0, width, height, GRID_BACKGROUND);
        hovered = Blueprint.AIR;

        Blueprint blueprint = view.getPlaced();
        if (blueprint == null) {
            return;
        }
        int sizeX = blueprint.getSizeX();
        int sizeZ = blueprint.getSizeZ();
        int cell = Math.max(MIN_CELL, Math.min((width - NORTH_BAR * 2) / sizeX, (height - NORTH_BAR * 2) / sizeZ));
        int gridWidth = cell * sizeX;
        int gridHeight = cell * sizeZ;
        int originX = (width - gridWidth) / 2;
        int originY = (height - gridHeight) / 2;
        int layer = view.getLayer();

        GuiDraw.drawRect(originX - 1, originY - 1, gridWidth + 2, gridHeight + 2, GRID_BORDER);
        for (int z = 0; z < sizeZ; z++) {
            for (int x = 0; x < sizeX; x++) {
                int value = blueprint.cellAt(x, layer, z);
                int color = value == Blueprint.AIR ? EMPTY_COLOR : color(blueprint, value);
                GuiDraw.drawRect(originX + x * cell, originY + z * cell, cell, cell, color);
                if (value == Blueprint.AIR && layer > 0 && blueprint.cellAt(x, layer - 1, z) != Blueprint.AIR) {
                    GuiDraw.drawRect(originX + x * cell + cell / 2, originY + z * cell + cell / 2, 1, 1, GHOST_COLOR);
                }
            }
        }
        if (cell >= LINE_CELL) {
            for (int x = 1; x < sizeX; x++) {
                GuiDraw.drawRect(originX + x * cell, originY, 1, gridHeight, GRID_LINE);
            }
            for (int z = 1; z < sizeZ; z++) {
                GuiDraw.drawRect(originX, originY + z * cell, gridWidth, 1, GRID_LINE);
            }
        }
        GuiDraw.drawRect(originX, originY - NORTH_BAR - 1, gridWidth, NORTH_BAR, NORTH_COLOR);

        if (isHovering()) {
            int localX = (getContext().getMouseX() - area.x - originX) / cell;
            int localZ = (getContext().getMouseY() - area.y - originY) / cell;
            if (localX >= 0 && localZ >= 0 && localX < sizeX && localZ < sizeZ) {
                hovered = blueprint.cellAt(localX, layer, localZ);
                GuiDraw.drawRect(originX + localX * cell, originY + localZ * cell, cell, 1, 0xFFFFFFFF);
                GuiDraw.drawRect(originX + localX * cell, originY + localZ * cell + cell - 1, cell, 1, 0xFFFFFFFF);
                GuiDraw.drawRect(originX + localX * cell, originY + localZ * cell, 1, cell, 0xFFFFFFFF);
                GuiDraw.drawRect(originX + localX * cell + cell - 1, originY + localZ * cell, 1, cell, 0xFFFFFFFF);
            }
        }
    }

    private int color(Blueprint blueprint, int value) {
        Integer cached = colors.get(value);
        if (cached != null) {
            return cached;
        }
        int color = cellColor(blueprint, value);
        colors.put(value, color);
        return color;
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
        view.stepLayer(scrollDirection == UpOrDown.UP ? 1 : -1);
        return true;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        return Result.SUCCESS;
    }

    @Override
    public boolean canHover() {
        return true;
    }
}
