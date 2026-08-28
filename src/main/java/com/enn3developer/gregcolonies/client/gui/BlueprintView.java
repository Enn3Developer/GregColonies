package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketBlueprintAction;
import com.enn3developer.gregcolonies.network.PacketBlueprintSave;
import com.enn3developer.gregcolonies.network.PacketRequestColony;

public class BlueprintView {

    private static final int MAX_LIBRARY_ROWS = Colony.MAX_BLUEPRINTS;

    private static final int MAX_MATERIAL_ROWS = 32;

    private static final int LEFT_WIDTH = 118;

    private static final int LIBRARY_MIN_HEIGHT = 52;

    private static final int LIBRARY_MAX_HEIGHT = 208;

    private static final int LEFT_FIXED = 92;

    private static final int MATERIALS_HEIGHT = 65;

    private static final int COLUMN_GAP = 6;

    private static final int MIN_WIDTH = 300;

    private static final int MAX_WIDTH = 560;

    private static final int MIN_HEIGHT = 200;

    private static final int MAX_HEIGHT = 420;

    private static final int SCREEN_INSET = 10;

    private static final int SMALL_BUTTON = 13;

    private static final int BACK_WIDTH = 34;

    private static final int HEADER_HEIGHT = 18;

    private static final int DETAIL_FIXED = 144;

    private static final int MIN_PREVIEW = 60;

    private static final int COLONY_INTERVAL = 20;

    private static final int DETAIL_INTERVAL = 60;

    private final ColonyView colonyView;

    private ColonySnapshot colony;

    private Blueprint source;

    private Blueprint placed;

    private int detailIndex = -1;

    private int requested = -1;

    private final Map<Integer, Integer> stock = new LinkedHashMap<>();

    private final List<Integer> materials = new ArrayList<>();

    private final Map<Integer, Integer> needed = new LinkedHashMap<>();

    private int layer;

    private int revision;

    private Blueprint shown;

    private int shownRotation = Integer.MIN_VALUE;

    private boolean shownMirror;

    private int colonyTicks;

    private int detailTicks;

    private BlueprintPreview preview;

    private TextFieldWidget nameField;

    private boolean placementPending;

    private int pendingRotation;

    private boolean pendingMirror;

    public BlueprintView(ColonyView colonyView) {
        this.colonyView = colonyView;
        this.colony = colonyView.getColony();
    }

    public ColonyView getColonyView() {
        return colonyView;
    }

    public ColonySnapshot getColony() {
        return colony;
    }

    public void setColony(ColonySnapshot colony) {
        this.colony = colony;
        colonyView.setColony(colony);
        if (placementPending && normalize(colony.getPlaceRotation()) == pendingRotation
            && colony.isPlaceMirror() == pendingMirror) {
            placementPending = false;
        }
        int active = colony.getActiveBlueprint();
        if (active < 0) {
            clearDetail();
        } else if (active != detailIndex && active != requested) {
            request(active);
        }
        retransform();
    }

    public void accept(int index, Blueprint blueprint, Map<Integer, Integer> held) {
        if (colony == null || blueprint == null) {
            return;
        }
        detailIndex = index;
        requested = -1;
        source = blueprint;
        stock.clear();
        stock.putAll(held);
        needed.clear();
        needed.putAll(blueprint.materials());
        materials.clear();
        materials.addAll(needed.keySet());
        if (preview != null) {
            preview.reset();
        }
        retransform();
        layer = placed == null ? 0 : placed.getSizeY() - 1;
    }

    private void clearDetail() {
        detailIndex = -1;
        requested = -1;
        source = null;
        placed = null;
        stock.clear();
        needed.clear();
        materials.clear();
    }

    private void request(int index) {
        if (index < 0) {
            return;
        }
        requested = index;
        GCNetwork.CHANNEL.sendToServer(new PacketBlueprintAction(colony.getId(), PacketBlueprintAction.REQUEST, index));
    }

    private static int normalize(int rotation) {
        return ((rotation % Blueprint.ROTATIONS) + Blueprint.ROTATIONS) % Blueprint.ROTATIONS;
    }

    private int rotation() {
        return placementPending ? pendingRotation : normalize(colony.getPlaceRotation());
    }

    private boolean mirror() {
        return placementPending ? pendingMirror : colony.isPlaceMirror();
    }

    private void setPlacement(int rotation, boolean mirror) {
        pendingRotation = normalize(rotation);
        pendingMirror = mirror;
        placementPending = true;
        retransform();
        GCNetwork.CHANNEL.sendToServer(
            new PacketBlueprintAction(
                colony.getId(),
                PacketBlueprintAction.PLACEMENT,
                colony.getActiveBlueprint(),
                pendingRotation,
                pendingMirror,
                ""));
    }

    private void retransform() {
        int rotation = rotation();
        boolean mirror = mirror();
        if (source != shown || rotation != shownRotation || mirror != shownMirror) {
            shown = source;
            shownRotation = rotation;
            shownMirror = mirror;
            placed = source == null ? null : source.transformed(rotation, mirror);
            revision++;
        }
        if (placed == null) {
            layer = 0;
        } else {
            layer = Math.max(0, Math.min(layer, placed.getSizeY() - 1));
        }
    }

    public int getRevision() {
        return revision;
    }

    public Blueprint getPlaced() {
        return placed;
    }

    public int getLayer() {
        return layer;
    }

    public void stepLayer(int by) {
        if (placed == null) {
            return;
        }
        layer = Math.max(0, Math.min(layer + by, placed.getSizeY() - 1));
    }

    public boolean isEditing() {
        return nameField != null && nameField.isFocused();
    }

    public ModularPanel buildPanel() {
        ModularPanel panel = new ModularPanel("colony_blueprints");
        panel.width(this::panelWidth, Unit.Measure.PIXEL);
        panel.height(this::panelHeight, Unit.Measure.PIXEL);
        panel.center();
        panel.padding(GuiStyle.PADDING);
        panel.background(GuiStyle.skin(GuiStyle.PANEL_BACKGROUND, GuiStyle.PANEL_BORDER));
        panel.child(
            Flow.column()
                .full()
                .child(
                    Flow.row()
                        .widthRel(1.0F)
                        .coverChildrenHeight()
                        .marginBottom(GuiStyle.ROW_GAP)
                        .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                        .child(GuiStyle.label(IKey.dynamic(this::title), GuiStyle.TITLE_COLOR))
                        .child(GuiStyle.button("Back", BACK_WIDTH, () -> true, this::back)))
                .child(
                    Flow.row()
                        .widthRel(1.0F)
                        .expanded()
                        .childPadding(COLUMN_GAP)
                        .child(buildLibrary())
                        .child(buildDetail())));
        panel.onUpdateListener(widget -> tick());
        return panel;
    }

    private void tick() {
        if (++colonyTicks >= COLONY_INTERVAL) {
            colonyTicks = 0;
            GCNetwork.CHANNEL.sendToServer(new PacketRequestColony());
        }
        if (++detailTicks >= DETAIL_INTERVAL) {
            detailTicks = 0;
            request(colony.getActiveBlueprint());
        }
    }

    private double libraryHeight() {
        double room = panelHeight() - GuiStyle.PADDING * 2 - HEADER_HEIGHT - LEFT_FIXED;
        return Math.max(LIBRARY_MIN_HEIGHT, Math.min(LIBRARY_MAX_HEIGHT, room));
    }

    private static ScaledResolution resolution() {
        Minecraft mc = Minecraft.getMinecraft();
        return new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
    }

    private double panelWidth() {
        int screen = resolution().getScaledWidth();
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, screen - SCREEN_INSET * 2));
    }

    private double panelHeight() {
        int screen = resolution().getScaledHeight();
        return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, screen - SCREEN_INSET * 2));
    }

    private double previewHeight() {
        return Math.max(MIN_PREVIEW, panelHeight() - GuiStyle.PADDING * 2 - HEADER_HEIGHT - DETAIL_FIXED);
    }

    private Flow buildLibrary() {
        VerticalScrollData scroll = new VerticalScrollData();
        scroll.texture(GuiStyle.scrollHandle());
        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.scrollDirection(scroll);
        list.collapseDisabledChild();
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        list.widthRel(1.0F);
        list.height(this::libraryHeight, Unit.Measure.PIXEL);
        list.marginBottom(GuiStyle.ROW_GAP);
        list.background(GuiStyle.skin(GuiStyle.SCROLL_TRACK, GuiStyle.SECTION_LINE));
        list.getScrollArea()
            .setScrollBarBackgroundColor(GuiStyle.SCROLL_TRACK);
        for (int index = 0; index < MAX_LIBRARY_ROWS; index++) {
            list.child(buildLibraryRow(index));
        }

        nameField = GuiStyle.field(Blueprint.MAX_NAME_LENGTH);
        nameField.expanded();

        return Flow.column()
            .width(LEFT_WIDTH)
            .heightRel(1.0F)
            .child(GuiStyle.section("Library", this::libraryValue, 0))
            .child(list)
            .child(
                GuiStyle.row()
                    .child(nameField))
            .child(
                GuiStyle.row()
                    .child(GuiStyle.button("Rename", GuiStyle.EXPAND, this::hasSelected, this::sendRename))
                    .child(GuiStyle.button("Delete", GuiStyle.EXPAND, this::hasSelected, this::sendDelete)))
            .child(GuiStyle.section("Design", this::designValue, GuiStyle.SECTION_GAP))
            .child(
                GuiStyle.row()
                    .child(GuiStyle.button("New", GuiStyle.EXPAND, this::canCreate, this::openNew))
                    .child(GuiStyle.button("Edit", GuiStyle.EXPAND, this::hasDetail, this::openEdit)));
    }

    private Flow buildDetail() {
        preview = new BlueprintPreview(this);
        preview.widthRel(1.0F);
        preview.height(this::previewHeight, Unit.Measure.PIXEL);
        preview.marginBottom(GuiStyle.ROW_GAP);

        VerticalScrollData scroll = new VerticalScrollData();
        scroll.texture(GuiStyle.scrollHandle());
        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.scrollDirection(scroll);
        list.collapseDisabledChild();
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        list.widthRel(1.0F);
        list.height(MATERIALS_HEIGHT);
        list.getScrollArea()
            .setScrollBarBackgroundColor(GuiStyle.SCROLL_TRACK);
        for (int index = 0; index < MAX_MATERIAL_ROWS; index++) {
            list.child(buildMaterialRow(index));
        }

        Flow column = Flow.column();
        column.width(this::detailWidthValue, Unit.Measure.PIXEL);
        return column.heightRel(1.0F)
            .child(GuiStyle.section("Selected", this::detailValue, 0))
            .child(
                GuiStyle.row()
                    .child(
                        GuiStyle.toggleButton(
                            this::rotationLabel,
                            GuiStyle.EXPAND,
                            () -> rotation() != 0,
                            this::sendRotate))
                    .child(GuiStyle.toggleButton(() -> "Mirror", GuiStyle.EXPAND, this::mirror, this::sendMirror)))
            .child(preview)
            .child(
                GuiStyle.row()
                    .child(GuiStyle.button("-", SMALL_BUTTON, this::hasDetail, () -> stepLayer(-1)))
                    .child(layerLabel())
                    .child(GuiStyle.button("+", SMALL_BUTTON, this::hasDetail, () -> stepLayer(1))))
            .child(hoverLabel())
            .child(GuiStyle.section("Materials", this::materialsValue, 0))
            .child(list);
    }

    private TextWidget<?> layerLabel() {
        TextWidget<?> line = GuiStyle.label(IKey.dynamic(this::layerValue), GuiStyle.TEXT_COLOR);
        line.textAlign(Alignment.Center);
        line.expanded();
        return line;
    }

    private TextWidget<?> hoverLabel() {
        TextWidget<?> line = GuiStyle.label(IKey.dynamic(this::hoverValue), GuiStyle.HINT_COLOR);
        line.widthRel(1.0F);
        line.marginBottom(1);
        return line;
    }

    private IWidget buildLibraryRow(int index) {
        return GuiRow.at(
            index,
            () -> colony.getBlueprints()
                .size())
            .padding(GuiStyle.SWATCH_WIDTH + 3, 3)
            .label(() -> libraryRowLabel(index))
            .hint(() -> libraryRowSize(index))
            .skin(
                libraryRowSkin(index, GuiStyle.ROW_BACKGROUND, GuiStyle.ROW_SELECTED),
                libraryRowSkin(index, GuiStyle.BUTTON_HOVER, GuiStyle.ROW_SELECTED_HOVER))
            .onClick(this::sendSelect)
            .build();
    }

    private IDrawable libraryRowSkin(int index, int fill, int selected) {
        return (context, x, y, width, height, theme) -> {
            boolean active = index == colony.getActiveBlueprint();
            GuiDraw.drawRect(x, y, width, height, active ? selected : fill);
            GuiDraw
                .drawRect(x, y, GuiStyle.SWATCH_WIDTH, height, active ? GuiStyle.ACTIVE_BORDER : GuiStyle.SECTION_LINE);
        };
    }

    private ColonySnapshot.BlueprintEntry entryAt(int index) {
        return colony.getBlueprint(index);
    }

    private String libraryRowLabel(int index) {
        ColonySnapshot.BlueprintEntry entry = entryAt(index);
        if (entry == null) {
            return "";
        }
        int room = LEFT_WIDTH - GuiStyle.SWATCH_WIDTH
            - GuiStyle.SCROLL_THICKNESS
            - 12
            - GuiText.width(libraryRowSize(index));
        return GuiText.trim(entry.getLabel(index), room);
    }

    private String libraryRowSize(int index) {
        ColonySnapshot.BlueprintEntry entry = entryAt(index);
        return entry == null ? "" : entry.getSizeLabel();
    }

    private IWidget buildMaterialRow(int index) {
        return GuiRow.at(index, materials::size)
            .height(GuiStyle.ROW_HEIGHT - 2)
            .padding(GuiStyle.SWATCH_WIDTH + 3, 3)
            .label(() -> materialName(index))
            .hint(() -> materialCount(index), () -> materialColor(index))
            .skin(materialRowSkin(index), null)
            .build();
    }

    private IDrawable materialRowSkin(int index) {
        return (context, x, y, width, height, theme) -> {
            if (index >= materials.size()) {
                return;
            }
            GuiDraw.drawRect(x, y, GuiStyle.SWATCH_WIDTH, height, materialColorOf(materials.get(index)));
        };
    }

    private int materialColorOf(int cell) {
        return placed == null ? GuiStyle.SECTION_LINE : BlueprintPreview.cellColor(placed, cell);
    }

    private String materialName(int index) {
        if (index >= materials.size()) {
            return "";
        }
        String name = stackName(materials.get(index));
        return GuiText.trim(name, detailWidth() - GuiText.width(materialCount(index)) - 20);
    }

    private String stackName(int cell) {
        ItemStack stack = placed == null ? null : placed.stackOf(cell);
        if (stack == null) {
            return "unknown block";
        }
        try {
            return stack.getDisplayName();
        } catch (RuntimeException error) {
            return "unknown block";
        }
    }

    private String materialCount(int index) {
        if (index >= materials.size()) {
            return "";
        }
        int cell = materials.get(index);
        int have = stock.getOrDefault(cell, 0);
        return have + "/" + needed.getOrDefault(cell, 0);
    }

    private int materialColor(int index) {
        if (index >= materials.size()) {
            return GuiStyle.HINT_COLOR;
        }
        int cell = materials.get(index);
        return stock.getOrDefault(cell, 0) >= needed.getOrDefault(cell, 0) ? GuiStyle.ACTIVE_COLOR
            : GuiStyle.WARN_COLOR;
    }

    private double detailWidthValue() {
        return panelWidth() - GuiStyle.PADDING * 2 - LEFT_WIDTH - COLUMN_GAP;
    }

    private int detailWidth() {
        return (int) detailWidthValue() - GuiStyle.SCROLL_THICKNESS;
    }

    private boolean hasSelected() {
        return colony.getActiveBlueprint() >= 0;
    }

    private boolean hasDetail() {
        return placed != null;
    }

    private boolean canCreate() {
        return colony.getBlueprints()
            .size() < Colony.MAX_BLUEPRINTS;
    }

    private String title() {
        return "Blueprints - " + colony.getName();
    }

    private String libraryValue() {
        return colony.getBlueprints()
            .size() + "/"
            + Colony.MAX_BLUEPRINTS;
    }

    private String designValue() {
        if (colony.getBlueprints()
            .size() >= Colony.MAX_BLUEPRINTS) {
            return "library full";
        }
        return placed == null ? "editor" : "editor ready";
    }

    private String detailValue() {
        if (placed == null) {
            return "none";
        }
        return placed.getSizeX() + "x"
            + placed.getSizeY()
            + "x"
            + placed.getSizeZ()
            + ", "
            + placed.blockCount()
            + " blocks";
    }

    private String rotationLabel() {
        return "Turn " + rotation() * 90 + "°";
    }

    private String layerValue() {
        if (placed == null) {
            return "no slice";
        }
        return "slice " + (layer + 1) + "/" + placed.getSizeY();
    }

    private String hoverValue() {
        if (preview == null || preview.getHovered() == Blueprint.AIR) {
            return placed == null ? "" : "drag to turn, wheel zooms, shift wheel slices";
        }
        return GuiText.trim(stackName(preview.getHovered()), detailWidth());
    }

    private String materialsValue() {
        return materials.size() + " kinds";
    }

    private void back() {
        BlueprintScreen.back();
    }

    private void sendSelect(int index) {
        GCNetwork.CHANNEL.sendToServer(new PacketBlueprintAction(colony.getId(), PacketBlueprintAction.SELECT, index));
    }

    private void sendRename() {
        int index = colony.getActiveBlueprint();
        if (index < 0) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(
            new PacketBlueprintAction(colony.getId(), PacketBlueprintAction.RENAME, index, nameField.getText()));
    }

    private void sendDelete() {
        int index = colony.getActiveBlueprint();
        if (index < 0) {
            return;
        }
        clearDetail();
        GCNetwork.CHANNEL.sendToServer(new PacketBlueprintAction(colony.getId(), PacketBlueprintAction.DELETE, index));
    }

    private void sendRotate() {
        setPlacement(rotation() + 1, mirror());
    }

    private void sendMirror() {
        setPlacement(rotation(), !mirror());
    }

    private void openNew() {
        if (canCreate()) {
            BlueprintEditorScreen.open(colony, PacketBlueprintSave.NEW, null);
        }
    }

    private void openEdit() {
        if (source != null) {
            BlueprintEditorScreen.open(colony, detailIndex, source);
        }
    }

}
