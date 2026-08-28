package com.enn3developer.gregcolonies.client.gui;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

public final class GuiRow {

    private final int index;

    private final IntSupplier count;

    private int height = GuiStyle.ROW_HEIGHT;

    private int leftPadding;

    private int rightPadding = 3;

    private int bottomMargin;

    private Supplier<String> label = () -> "";

    private IntSupplier labelColor = () -> GuiStyle.TEXT_COLOR;

    private Supplier<String> hint = () -> "";

    private IntSupplier hintColor = () -> GuiStyle.HINT_COLOR;

    private IDrawable background;

    private IDrawable hoverBackground;

    private IntConsumer action;

    private GuiRow(int index, IntSupplier count) {
        this.index = index;
        this.count = count;
    }

    public static GuiRow at(int index, IntSupplier count) {
        return new GuiRow(index, count);
    }

    public GuiRow height(int height) {
        this.height = height;
        return this;
    }

    public GuiRow padding(int left, int right) {
        this.leftPadding = left;
        this.rightPadding = right;
        return this;
    }

    public GuiRow marginBottom(int margin) {
        this.bottomMargin = margin;
        return this;
    }

    public GuiRow label(Supplier<String> label) {
        this.label = label;
        return this;
    }

    public GuiRow label(Supplier<String> label, IntSupplier color) {
        this.label = label;
        this.labelColor = color;
        return this;
    }

    public GuiRow hint(Supplier<String> hint) {
        this.hint = hint;
        return this;
    }

    public GuiRow hint(Supplier<String> hint, IntSupplier color) {
        this.hint = hint;
        this.hintColor = color;
        return this;
    }

    public GuiRow skin(IDrawable background, IDrawable hoverBackground) {
        this.background = background;
        this.hoverBackground = hoverBackground;
        return this;
    }

    public GuiRow onClick(IntConsumer action) {
        this.action = action;
        return this;
    }

    private boolean filled() {
        return index < count.getAsInt();
    }

    private TextWidget<?> text(Supplier<String> value, IntSupplier color) {
        return IKey.dynamic(value::get)
            .asWidget()
            .color(color::getAsInt)
            .shadow(true);
    }

    private Flow content() {
        return Flow.row()
            .widthRel(1.0F)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .paddingLeft(leftPadding)
            .paddingRight(rightPadding)
            .child(text(label, labelColor))
            .child(text(hint, hintColor));
    }

    public IWidget build() {
        if (action == null) {
            Flow row = content().height(height)
                .marginBottom(bottomMargin);
            if (background != null) {
                row.background(background);
            }
            row.setEnabled(filled());
            row.onUpdateListener(widget -> widget.setEnabled(filled()));
            return row;
        }

        ButtonWidget<?> row = new ButtonWidget<>();
        row.widthRel(1.0F);
        row.height(height);
        row.marginBottom(bottomMargin);
        if (background != null) {
            row.background(background);
        }
        if (hoverBackground != null) {
            row.hoverBackground(hoverBackground);
        }
        row.child(content().heightRel(1.0F));
        row.onMousePressed(mouseButton -> {
            if (filled()) {
                action.accept(index);
            }
            return true;
        });
        row.setEnabled(filled());
        row.onUpdateListener(widget -> widget.setEnabled(filled()));
        return row;
    }
}
