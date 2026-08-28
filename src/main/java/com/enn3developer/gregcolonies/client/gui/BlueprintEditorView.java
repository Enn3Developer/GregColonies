package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;

public class BlueprintEditorView extends OverlayView {

    private static final int SIDE_WIDTH = 118;

    private static final int PALETTE_WIDTH = 132;

    private static final int PALETTE_HEIGHT = 150;

    private static final int PALETTE_MIN_HEIGHT = 52;

    private static final int PALETTE_TOP = GuiStyle.SCREEN_MARGIN + 46;

    private static final int MAX_PALETTE_ROWS = 64;

    private static final int MAX_MATERIAL_ROWS = 24;

    private static final int MATERIALS_HEIGHT = 52;

    private static final int SMALL_BUTTON = 13;

    private static final int ICON_WIDTH = 16;

    private static final int PALETTE_INTERVAL = 40;

    private static final float FIT_DISTANCE = 2.4F;

    private static final int CONFIRM_WIDTH = 168;

    private static final int CONFIRM_MIN_WIDTH = 118;

    private static final int CONFIRM_GAP = 4;

    private static final int SHADE_COLOR = 0xB4060810;

    private static final String PAINT_HINT = "LMB paint   RMB erase   RMB drag pan";
    private static final String CAMERA_HINT = "MMB drag turn   wheel zoom   WASD pan   R recenter";
    private static final String LAYER_HINT = "shift wheel layer   X slice   Tab tool   1-9 brush";
    private static final String ANCHOR_HINT = "Anchor tool marks where a build order lands";
    private static final String EDIT_HINT = "ctrl+Z undo   ctrl+Y redo";

    private final BlueprintEditor editor;

    private BlueprintTrace.Hit hover;

    private ModularPanel panel;

    private ListWidget<IWidget, ?> sidePanel;

    private ListWidget<IWidget, ?> palettePanel;

    private TextFieldWidget nameField;

    private boolean confirming;

    private int paletteTicks;

    private final Map<Integer, Integer> missing = new LinkedHashMap<>();

    private final List<Integer> missingCells = new ArrayList<>();

    public BlueprintEditorView(BlueprintEditor editor) {
        super(true);
        this.editor = editor;
    }

    public BlueprintEditor getEditor() {
        return editor;
    }

    public ColonySnapshot getColony() {
        return editor.getColony();
    }

    public BlueprintTrace.Hit getHover() {
        return hover;
    }

    public void setHover(BlueprintTrace.Hit hover) {
        this.hover = hover;
    }

    public boolean isEditingText() {
        return nameField != null && nameField.isFocused();
    }

    public boolean isConfirming() {
        return confirming;
    }

    public boolean requestClose() {
        if (!editor.isDirty()) {
            return true;
        }
        confirming = true;
        return false;
    }

    public void acceptPalette(PacketColonyPalette message) {
        editor.acceptPalette(message);
    }

    public void focusCamera() {
        ColonyCamera camera = ColonyCamera.get();
        if (camera == null) {
            return;
        }
        Blueprint model = editor.getModel();
        float span = Math.max(model.getSizeX(), Math.max(model.getSizeY(), model.getSizeZ()));
        camera.focus(editor.centerX(), editor.centerY(), editor.centerZ(), span * FIT_DISTANCE);
    }

    public ModularPanel buildPanel() {
        panel = new ModularPanel("blueprint_editor").fullScreenInvisible();
        panel.child(new BlueprintEditorWidget(this).full());
        panel.child(buildHeader());
        panel.child(buildPalette());
        panel.child(buildSidePanel());
        panel.child(buildHints());
        panel.child(buildConfirm());
        panel.onUpdateListener(widget -> tick());
        return panel;
    }

    private void tick() {
        if (paletteTicks == 0 || ++paletteTicks >= PALETTE_INTERVAL) {
            paletteTicks = 1;
            editor.requestPalette();
        }
        missing.clear();
        missing.putAll(editor.shortfall());
        missingCells.clear();
        missingCells.addAll(missing.keySet());
    }

    private Flow buildConfirm() {
        Flow prompt = Flow.column();
        prompt.width(this::confirmWidth, Unit.Measure.PIXEL);
        prompt.coverChildrenHeight();
        prompt.padding(GuiStyle.PADDING);
        prompt.childPadding(GuiStyle.ROW_GAP);
        prompt.crossAxisAlignment(Alignment.CrossAxis.START);
        prompt.background(GuiStyle.skin(GuiStyle.PANEL_BACKGROUND, GuiStyle.PANEL_BORDER));
        prompt.child(GuiStyle.label(IKey.str("Unsaved changes"), GuiStyle.TITLE_COLOR));
        prompt.child(GuiStyle.label(IKey.dynamic(this::confirmValue), GuiStyle.HINT_COLOR));
        prompt.child(
            GuiStyle.row()
                .child(GuiStyle.button("Save", GuiStyle.EXPAND, this::canSave, this::save))
                .child(GuiStyle.button("Discard", GuiStyle.EXPAND, () -> true, this::discard)));
        prompt.child(
            GuiStyle.row()
                .child(GuiStyle.button("Cancel", GuiStyle.EXPAND, () -> true, () -> confirming = false)));

        Flow overlay = Flow.row();
        overlay.full();
        overlay.mainAxisAlignment(Alignment.MainAxis.CENTER);
        overlay.crossAxisAlignment(Alignment.CrossAxis.CENTER);
        overlay.background(new Rectangle().color(SHADE_COLOR));
        overlay.child(prompt);
        overlay.setEnabledIf(widget -> confirming);
        return overlay;
    }

    // the help panel is bottom anchored and grows upwards, so the palette stops above it;
    // this resolves at layout time, so it is sized for the taller help-open case
    private double paletteHeight() {
        double floor = getHints() == null ? GuiStyle.screenHeight() - GuiStyle.SCREEN_MARGIN : getHints().getArea().y;
        double room = floor - PALETTE_TOP - CONFIRM_GAP;
        return Math.max(PALETTE_MIN_HEIGHT, Math.min(PALETTE_HEIGHT, room));
    }

    // the prompt is centred on the screen, so it must stay clear of the palette column
    private double confirmWidth() {
        double screen = GuiStyle.screenWidth();
        double room = (screen / 2.0D - GuiStyle.SCREEN_MARGIN - PALETTE_WIDTH - CONFIRM_GAP) * 2.0D;
        return Math.max(CONFIRM_MIN_WIDTH, Math.min(CONFIRM_WIDTH, room));
    }

    private void discard() {
        confirming = false;
        BlueprintEditorScreen.back();
    }

    private String confirmValue() {
        return editor.getModel()
            .blockCount() + " blocks would be lost";
    }

    private ListWidget<IWidget, ?> buildPalette() {
        ListWidget<IWidget, ?> list = GuiStyle.panelList();
        list.width(PALETTE_WIDTH);
        list.height(this::paletteHeight, Unit.Measure.PIXEL);
        list.left(GuiStyle.SCREEN_MARGIN);
        list.top(PALETTE_TOP);
        list.child(GuiStyle.section("Materials chest", this::paletteValue, 0));
        for (int index = 0; index < MAX_PALETTE_ROWS; index++) {
            list.child(buildPaletteRow(index));
        }
        TextWidget<?> empty = GuiStyle.label(IKey.dynamic(this::paletteHint), GuiStyle.HINT_COLOR);
        empty.widthRel(1.0F);
        empty.setEnabledIf(
            widget -> editor.getPalette()
                .isEmpty());
        list.child(empty);
        palettePanel = list;
        return list;
    }

    private IWidget buildPaletteRow(int index) {
        return GuiRow.at(
            index,
            () -> editor.getPalette()
                .size())
            .height(ICON_WIDTH)
            .padding(ICON_WIDTH + 2, 3)
            .label(
                () -> paletteLabel(index),
                () -> index == editor.getBrushIndex() ? GuiStyle.ACTIVE_COLOR : GuiStyle.TEXT_COLOR)
            .hint(() -> paletteCount(index))
            .skin(paletteRowSkin(index, GuiStyle.ROW_BACKGROUND), paletteRowSkin(index, GuiStyle.BUTTON_HOVER))
            .onClick(editor::setBrush)
            .build();
    }

    private IDrawable paletteRowSkin(int index, int fill) {
        return (context, x, y, width, height, theme) -> {
            boolean active = index == editor.getBrushIndex();
            GuiDraw.drawRect(x, y, width, height, active ? GuiStyle.ROW_SELECTED : fill);
            BlueprintBrush brush = brushAt(index);
            if (brush != null) {
                GuiDraw.drawItem(brush.stack(), x, y, ICON_WIDTH, ICON_WIDTH, context.getCurrentDrawingZ());
            }
        };
    }

    private BlueprintBrush brushAt(int index) {
        return index >= 0 && index < editor.getPalette()
            .size() ? editor.getPalette()
                .get(index) : null;
    }

    private String paletteLabel(int index) {
        BlueprintBrush brush = brushAt(index);
        if (brush == null) {
            return "";
        }
        int room = PALETTE_WIDTH - GuiStyle.PADDING * 2 - GuiStyle.SCROLL_THICKNESS - ICON_WIDTH - 8;
        return GuiText.fit(brush.label(), paletteCount(index), room);
    }

    private String paletteCount(int index) {
        BlueprintBrush brush = brushAt(index);
        return brush == null ? "" : String.valueOf(brush.getHeld());
    }

    private String paletteValue() {
        return editor.getPalette()
            .size() + " kinds";
    }

    private String paletteHint() {
        return getColony().site(ColonySiteKind.MATERIALS)
            .isPresent() ? "chest is empty" : "no materials chest set";
    }

    private ListWidget<IWidget, ?> buildSidePanel() {
        ListWidget<IWidget, ?> list = GuiStyle.panelList();
        list.maxSizeRelOffset(1.0F, -GuiStyle.SCREEN_MARGIN * 2);
        list.width(SIDE_WIDTH);
        list.right(GuiStyle.SCREEN_MARGIN);
        list.top(GuiStyle.SCREEN_MARGIN);

        list.child(GuiStyle.section("Tool", this::toolValue, 0));
        list.child(
            GuiStyle.row()
                .child(toolButton(EditorTool.PAINT))
                .child(toolButton(EditorTool.ERASE)));
        list.child(
            GuiStyle.row()
                .child(toolButton(EditorTool.BOX))
                .child(toolButton(EditorTool.PICK)));
        list.child(
            GuiStyle.row()
                .child(toolButton(EditorTool.ANCHOR)));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Undo", GuiStyle.EXPAND, editor::hasUndo, editor::undo))
                .child(GuiStyle.button("Redo", GuiStyle.EXPAND, editor::hasRedo, editor::redo)));

        list.child(GuiStyle.section("Layer", this::layerValue, GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("-", SMALL_BUTTON, () -> true, () -> editor.stepLayer(-1)))
                .child(GuiStyle.toggleButton(() -> "Slice", GuiStyle.EXPAND, editor::isSliced, editor::toggleSlice))
                .child(GuiStyle.button("+", SMALL_BUTTON, () -> true, () -> editor.stepLayer(1))));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Fill layer", GuiStyle.EXPAND, this::hasBrush, editor::fillLayer))
                .child(GuiStyle.button("Clear", GuiStyle.EXPAND, () -> true, editor::clearLayer)));

        list.child(GuiStyle.section("Canvas", this::canvasValue, GuiStyle.SECTION_GAP));
        list.child(axisRow("X", 0));
        list.child(axisRow("Y", 1));
        list.child(axisRow("Z", 2));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Turn", GuiStyle.EXPAND, () -> true, editor::turn))
                .child(GuiStyle.button("Flip", GuiStyle.EXPAND, () -> true, editor::flip)));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Wipe", GuiStyle.EXPAND, () -> true, editor::wipe)));

        nameField = GuiStyle.field(Blueprint.MAX_NAME_LENGTH);
        nameField.expanded();
        nameField.setText(
            editor.getModel()
                .getName());
        list.child(GuiStyle.section("Save", this::saveValue, GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(nameField));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Save", GuiStyle.EXPAND, this::canSave, this::save)));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Close", GuiStyle.EXPAND, () -> true, this::close)));

        list.child(GuiStyle.section("Missing", this::missingValue, GuiStyle.SECTION_GAP));
        ListWidget<IWidget, ?> shortfall = GuiStyle.scrollList();
        shortfall.widthRel(1.0F);
        shortfall.height(MATERIALS_HEIGHT);
        for (int index = 0; index < MAX_MATERIAL_ROWS; index++) {
            shortfall.child(buildMissingRow(index));
        }
        list.child(shortfall);

        sidePanel = list;
        return list;
    }

    private Flow axisRow(String label, int axis) {
        return GuiStyle.row()
            .child(GuiStyle.button("-", SMALL_BUTTON, () -> true, () -> editor.resize(axis, -1)))
            .child(axisLabel(label, axis))
            .child(GuiStyle.button("+", SMALL_BUTTON, () -> true, () -> editor.resize(axis, 1)));
    }

    private TextWidget<?> axisLabel(String label, int axis) {
        TextWidget<?> line = GuiStyle.label(IKey.dynamic(() -> label + " " + axisSize(axis)), GuiStyle.TEXT_COLOR);
        line.textAlign(Alignment.Center);
        line.expanded();
        return line;
    }

    private int axisSize(int axis) {
        Blueprint model = editor.getModel();
        return axis == 0 ? model.getSizeX() : axis == 1 ? model.getSizeY() : model.getSizeZ();
    }

    private ButtonWidget<?> toolButton(EditorTool tool) {
        return GuiStyle
            .toggleButton(tool::getLabel, GuiStyle.EXPAND, () -> editor.getTool() == tool, () -> editor.setTool(tool));
    }

    private IWidget buildMissingRow(int index) {
        return GuiRow.at(index, missingCells::size)
            .height(GuiStyle.ROW_HEIGHT - 2)
            .padding(0, 3)
            .label(() -> missingName(index))
            .hint(() -> missingCount(index), () -> GuiStyle.WARN_COLOR)
            .build();
    }

    private String missingName(int index) {
        Integer cell = cellAt(index);
        if (cell == null) {
            return "";
        }
        ItemStack stack = editor.getModel()
            .getPalette()
            .stackOf(cell);
        String name = stack == null ? "unknown block" : safeName(stack);
        return GuiText.fit(name, missingCount(index), SIDE_WIDTH - GuiStyle.PADDING * 2 - 10);
    }

    private static String safeName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (RuntimeException error) {
            return "unknown block";
        }
    }

    private String missingCount(int index) {
        Integer cell = cellAt(index);
        return cell == null ? "" : "-" + missing.get(cell);
    }

    private Integer cellAt(int index) {
        return index >= 0 && index < missingCells.size() ? missingCells.get(index) : null;
    }

    private boolean hasBrush() {
        return editor.getBrush() != null;
    }

    private boolean canSave() {
        return !editor.getModel()
            .isEmpty();
    }

    private void close() {
        if (requestClose()) {
            BlueprintEditorScreen.back();
        }
    }

    private void save() {
        confirming = false;
        editor.getModel()
            .setName(nameField.getText());
        editor.save();
        BlueprintEditorScreen.back();
    }

    @Override
    protected String title() {
        String name = editor.getModel()
            .getName();
        return "Editor - " + (name.isEmpty() ? "untitled" : name);
    }

    @Override
    protected String headerLine() {
        Blueprint model = editor.getModel();
        return model.getSizeX() + "x"
            + model.getSizeY()
            + "x"
            + model.getSizeZ()
            + ", "
            + model.blockCount()
            + " blocks";
    }

    @Override
    protected String headerHint() {
        if (hover == null) {
            return "aim at the build plane";
        }
        int[] spot = hover.solid ? new int[] { hover.hitX, hover.hitY, hover.hitZ }
            : new int[] { hover.placeX, hover.placeY, hover.placeZ };
        return spot[0] + " / " + spot[1] + " / " + spot[2];
    }

    private String toolValue() {
        EditorTool tool = editor.getTool();
        if (tool == EditorTool.BOX) {
            return editor.getBoxAnchor() == null ? "box: first corner" : "box: second corner";
        }
        return tool.getState();
    }

    private String layerValue() {
        return editor.getLayer() + 1
            + "/"
            + editor.getModel()
                .getSizeY();
    }

    private String canvasValue() {
        return editor.getModel()
            .volume() + "/"
            + Blueprint.MAX_VOLUME;
    }

    private String saveValue() {
        return editor.getIndex() < 0 ? "new" : "slot " + (editor.getIndex() + 1);
    }

    private String missingValue() {
        return missingCells.isEmpty() ? "stocked" : missingCells.size() + " short";
    }

    @Override
    protected String helpHint() {
        return isHelpOpen() ? "H hides help" : "H shows help";
    }

    @Override
    protected IKey[] hintLines() {
        return new IKey[] { text(PAINT_HINT), text(CAMERA_HINT), text(LAYER_HINT), text(EDIT_HINT), text(ANCHOR_HINT) };
    }

}
