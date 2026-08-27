package com.enn3developer.gregcolonies.client.gui;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

public final class GuiStyle {

    public static final int EXPAND = -1;

    public static final int PADDING = 6;

    public static final int SCREEN_MARGIN = 6;

    public static final int SCROLL_THICKNESS = 4;

    public static final int ROW_TEXT_PADDING = 4;

    public static final int ROW_HEIGHT = 13;

    public static final int BUTTON_HEIGHT = 15;

    public static final int ROW_GAP = 3;

    public static final int SECTION_GAP = 5;

    public static final int SWATCH_WIDTH = 3;

    public static final int TITLE_COLOR = 0xFFFFD060;

    public static final int TEXT_COLOR = 0xFFC6CEDC;

    public static final int HINT_COLOR = 0xFF8A93A6;

    public static final int DISABLED_COLOR = 0xFF5F6878;

    public static final int PANEL_BACKGROUND = 0xFF0E1220;

    public static final int PANEL_BORDER = 0xFF33405C;

    public static final int SECTION_LINE = 0xFF2A3448;

    public static final int BUTTON_BACKGROUND = 0xFF1E2637;

    public static final int BUTTON_BORDER = 0xFF39455E;

    public static final int BUTTON_HOVER = 0xFF2E3A54;

    public static final int DISABLED_BACKGROUND = 0xFF161C2A;

    public static final int DISABLED_BORDER = 0xFF262F43;

    public static final int ROW_BACKGROUND = 0xFF1B2233;

    public static final int ROW_SELECTED = 0xFF243352;

    public static final int ROW_SELECTED_HOVER = 0xFF2F4067;

    public static final int SCROLL_TRACK = 0xFF141A28;

    public static final int SCROLL_HANDLE = 0xFF3C4A66;

    public static final int ACTIVE_BACKGROUND = 0xFF27543A;

    public static final int ACTIVE_BORDER = 0xFF4FA05F;

    public static final int ACTIVE_COLOR = 0xFFA8F0A8;

    public static final int WARN_COLOR = 0xFFE08A6B;

    private GuiStyle() {}

    public static IDrawable skin(int fill, int border) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, fill);
            GuiDraw.drawRect(x, y, width, 1, border);
            GuiDraw.drawRect(x, y + height - 1, width, 1, border);
            GuiDraw.drawRect(x, y, 1, height, border);
            GuiDraw.drawRect(x + width - 1, y, 1, height, border);
        };
    }

    public static IDrawable dynamicSkin(IntSupplier fill, IntSupplier border) {
        return (context, x, y, width, height, theme) -> skin(fill.getAsInt(), border.getAsInt())
            .draw(context, x, y, width, height, theme);
    }

    public static IDrawable scrollHandle() {
        return (context, x, y, width, height, theme) -> GuiDraw.drawRect(x + 1, y, width - 2, height, SCROLL_HANDLE);
    }

    public static TextWidget<?> label(IKey key, int color) {
        return key.asWidget()
            .color(color)
            .shadow(true);
    }

    public static Flow row() {
        return Flow.row()
            .widthRel(1.0F)
            .height(BUTTON_HEIGHT)
            .marginBottom(ROW_GAP)
            .childPadding(ROW_GAP);
    }

    public static Flow section(String title, Supplier<String> value, int gap) {
        return Flow.column()
            .widthRel(1.0F)
            .coverChildrenHeight()
            .marginTop(gap)
            .marginBottom(ROW_GAP)
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .coverChildrenHeight()
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(label(IKey.str(title), TITLE_COLOR))
                    .child(label(IKey.dynamic(value), HINT_COLOR)))
            .child(
                new Widget<>().widthRel(1.0F)
                    .height(1)
                    .marginTop(2)
                    .background(new Rectangle().color(SECTION_LINE)));
    }

    public static void sizeButton(ButtonWidget<?> widget, int width) {
        widget.height(BUTTON_HEIGHT);
        if (width == EXPAND) {
            widget.expanded();
        } else {
            widget.width(width);
        }
    }

    public static ButtonWidget<?> button(String label, int width, BooleanSupplier enabled, Runnable action) {
        ButtonWidget<?> widget = new ButtonWidget<>();
        sizeButton(widget, width);
        widget.background(
            dynamicSkin(
                () -> enabled.getAsBoolean() ? BUTTON_BACKGROUND : DISABLED_BACKGROUND,
                () -> enabled.getAsBoolean() ? BUTTON_BORDER : DISABLED_BORDER));
        widget.hoverBackground(
            dynamicSkin(
                () -> enabled.getAsBoolean() ? BUTTON_HOVER : DISABLED_BACKGROUND,
                () -> enabled.getAsBoolean() ? BUTTON_BORDER : DISABLED_BORDER));
        widget.child(
            IKey.str(label)
                .asWidget()
                .color(() -> enabled.getAsBoolean() ? TEXT_COLOR : DISABLED_COLOR)
                .shadow(true)
                .posRel(Alignment.Center));
        widget.onMousePressed(mouseButton -> {
            if (enabled.getAsBoolean()) {
                action.run();
            }
            return true;
        });
        return widget;
    }

    public static ButtonWidget<?> toggleButton(Supplier<String> label, int width, BooleanSupplier active,
        Runnable action) {
        ButtonWidget<?> widget = new ButtonWidget<>();
        sizeButton(widget, width);
        widget.background(
            dynamicSkin(
                () -> active.getAsBoolean() ? ACTIVE_BACKGROUND : BUTTON_BACKGROUND,
                () -> active.getAsBoolean() ? ACTIVE_BORDER : BUTTON_BORDER));
        widget.hoverBackground(
            dynamicSkin(
                () -> active.getAsBoolean() ? ACTIVE_BACKGROUND : BUTTON_HOVER,
                () -> active.getAsBoolean() ? ACTIVE_BORDER : BUTTON_BORDER));
        widget.child(
            IKey.dynamic(label::get)
                .asWidget()
                .color(() -> active.getAsBoolean() ? ACTIVE_COLOR : TEXT_COLOR)
                .shadow(true)
                .posRel(Alignment.Center));
        widget.onMousePressed(mouseButton -> {
            action.run();
            return true;
        });
        return widget;
    }

    public static TextFieldWidget field(int maxLength) {
        TextFieldWidget widget = new TextFieldWidget();
        widget.setMaxLength(maxLength);
        widget.background(skin(ROW_BACKGROUND, BUTTON_BORDER));
        widget.height(BUTTON_HEIGHT);
        return widget;
    }
}
