package com.enn3developer.gregcolonies.client.gui;

import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

public abstract class OverlayView {

    private boolean helpOpen;

    private Flow header;

    private Flow hints;

    protected OverlayView(boolean helpOpen) {
        this.helpOpen = helpOpen;
    }

    public boolean isHelpOpen() {
        return helpOpen;
    }

    public void toggleHelp() {
        helpOpen = !helpOpen;
    }

    protected abstract String title();

    protected abstract String headerLine();

    protected abstract String headerHint();

    protected abstract IKey[] hintLines();

    protected abstract String helpHint();

    protected Flow getHeader() {
        return header;
    }

    protected Flow getHints() {
        return hints;
    }

    protected Flow buildHeader() {
        header = GuiStyle.panelBox()
            .pos(GuiStyle.SCREEN_MARGIN, GuiStyle.SCREEN_MARGIN)
            .child(GuiStyle.label(IKey.dynamic(this::title), GuiStyle.TITLE_COLOR))
            .child(GuiStyle.label(IKey.dynamic(this::headerLine), GuiStyle.TEXT_COLOR))
            .child(GuiStyle.label(IKey.dynamic(this::headerHint), GuiStyle.HINT_COLOR));
        return header;
    }

    protected Flow buildHints() {
        hints = GuiStyle.panelBox()
            .left(GuiStyle.SCREEN_MARGIN)
            .bottom(GuiStyle.SCREEN_MARGIN)
            .collapseDisabledChild();
        for (IKey line : hintLines()) {
            hints.child(helpLine(line));
        }
        hints.child(GuiStyle.label(IKey.dynamic(this::helpHint), GuiStyle.TEXT_COLOR));
        return hints;
    }

    protected TextWidget<?> helpLine(IKey key) {
        TextWidget<?> line = GuiStyle.label(key, GuiStyle.HINT_COLOR);
        line.setEnabledIf(widget -> helpOpen);
        return line;
    }

    protected static IKey text(String value) {
        return IKey.str(value);
    }

    protected static IKey text(Supplier<String> value) {
        return IKey.dynamic(value::get);
    }
}
