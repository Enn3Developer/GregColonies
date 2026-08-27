package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.enn3developer.gregcolonies.client.ControllingCompat;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;
import com.enn3developer.gregcolonies.network.PacketCitizenGroup;
import com.enn3developer.gregcolonies.network.PacketColonyDropOff;
import com.enn3developer.gregcolonies.network.PacketColonyPickUp;
import com.enn3developer.gregcolonies.network.PacketOpenCitizen;

public class ColonyView {

    public static final String UNGROUPED = "ungrouped";

    public static final int TARGET_NONE = 0;

    public static final int TARGET_CHOP = 1;

    public static final int TARGET_MINE = 2;

    public static final int TARGET_FARM = 3;

    public static final int TARGET_DROP_OFF = 4;

    public static final int TARGET_PICK_UP = 5;

    private static final int MAX_GROUP_ROWS = 32;

    private static final int SIDE_WIDTH = 168;

    private static final int SIDE_MIN_WIDTH = 124;

    private static final float SIDE_MAX_FRACTION = 0.38F;

    private static final int SIDE_PADDING = 6;

    private static final int SCREEN_MARGIN = 6;

    private static final int ASSIGN_WIDTH = 46;

    private static final int EXPAND = -1;

    private static final int SWATCH_WIDTH = 3;

    private static final int SCROLL_THICKNESS = 4;

    private static final int ROW_TEXT_PADDING = 4;

    private static final int ROW_HEIGHT = 13;

    private static final int BUTTON_HEIGHT = 15;

    private static final int ROW_GAP = 3;

    private static final int SECTION_GAP = 5;

    private static final int TITLE_COLOR = 0xFFFFD060;

    private static final int TEXT_COLOR = 0xFFC6CEDC;

    private static final int HINT_COLOR = 0xFF8A93A6;

    private static final int DISABLED_COLOR = 0xFF5F6878;

    private static final int PANEL_BACKGROUND = 0xFF0E1220;

    private static final int PANEL_BORDER = 0xFF33405C;

    private static final int SECTION_LINE = 0xFF2A3448;

    private static final int BUTTON_BACKGROUND = 0xFF1E2637;

    private static final int BUTTON_BORDER = 0xFF39455E;

    private static final int BUTTON_HOVER = 0xFF2E3A54;

    private static final int DISABLED_BACKGROUND = 0xFF161C2A;

    private static final int DISABLED_BORDER = 0xFF262F43;

    private static final int ROW_BACKGROUND = 0xFF1B2233;

    private static final int ROW_SELECTED = 0xFF243352;

    private static final int ROW_SELECTED_HOVER = 0xFF2F4067;

    private static final int SCROLL_TRACK = 0xFF141A28;

    private static final int SCROLL_HANDLE = 0xFF3C4A66;

    private static final int ACTIVE_BACKGROUND = 0xFF27543A;

    private static final int ACTIVE_BORDER = 0xFF4FA05F;

    private static final int ACTIVE_COLOR = 0xFFA8F0A8;

    private static final int DROP_OFF_COLOR = 0xFFFF7CE0;

    private static final int PICK_UP_COLOR = 0xFF7CE0FF;

    private static final String SELECT_HINT = "LMB select   LMB drag box   shift add   LMB twice opens";

    private static final String MOVE_HINT = "RMB move   RMB drag pan   MMB drag turn";

    private static final String CAMERA_HINT = "scroll zoom   WASD pan   Q/E turn";

    private static final String GROUP_HINT = "click a group to select it   shift adds";

    private static final int MARKER_MARGIN = 7;

    private final ColonyViewWidget map = new ColonyViewWidget(this);

    private final Set<UUID> selection = new LinkedHashSet<>();

    private final List<String> groups = new ArrayList<>();

    private ColonySnapshot colony;

    private TextFieldWidget groupField;

    private final int[] pending = new int[6];

    private int targeting = TARGET_NONE;

    private boolean hasPending;

    private boolean helpOpen;

    private ModularPanel panel;

    private Flow header;

    private ListWidget<IWidget, ?> sidePanel;

    private Flow hints;

    public ColonyView(ColonySnapshot colony) {
        setColony(colony);
    }

    public ColonySnapshot getColony() {
        return colony;
    }

    public void setColony(ColonySnapshot colony) {
        this.colony = colony;
        Set<UUID> known = new LinkedHashSet<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            known.add(citizen.getId());
            counts.merge(
                citizen.getGroup()
                    .isEmpty() ? UNGROUPED : citizen.getGroup(),
                1,
                Integer::sum);
        }
        selection.retainAll(known);
        groups.clear();
        groups.addAll(counts.keySet());
    }

    public boolean isSelected(UUID id) {
        return selection.contains(id);
    }

    public Set<UUID> getSelection() {
        return selection;
    }

    public void clearSelection() {
        selection.clear();
    }

    public void toggle(UUID id) {
        if (!selection.remove(id)) {
            selection.add(id);
        }
    }

    public void selectAll() {
        selection.clear();
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            selection.add(citizen.getId());
        }
    }

    public void selectGroup(String group, boolean add) {
        if (!add) {
            selection.clear();
        }
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (groupLabel(citizen).equals(group)) {
                selection.add(citizen.getId());
            }
        }
    }

    public int getSelectedLoaded() {
        int loaded = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.isLoaded() && selection.contains(citizen.getId())) {
                loaded++;
            }
        }
        return loaded;
    }

    public int getTargeting() {
        return targeting;
    }

    public void setTargeting(int mode) {
        targeting = targeting == mode ? TARGET_NONE : mode;
        hasPending = false;
    }

    public boolean isHelpOpen() {
        return helpOpen;
    }

    public void toggleHelp() {
        helpOpen = !helpOpen;
    }

    public boolean hasPending() {
        return targeting != TARGET_NONE && hasPending;
    }

    public int[] getPending() {
        return pending;
    }

    public void setPending(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        pending[0] = minX;
        pending[1] = minY;
        pending[2] = minZ;
        pending[3] = maxX;
        pending[4] = maxY;
        pending[5] = maxZ;
        hasPending = true;
    }

    public void clearPending() {
        hasPending = false;
    }

    public boolean isEditing() {
        return groupField != null && groupField.isFocused();
    }

    public void sendCommand(byte action, boolean append, int x, int y, int z) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenCommand(colony.getId(), action, append, x, y, z, selection));
    }

    public void sendArea(byte action, boolean append, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL
            .sendToServer(new PacketCitizenCommand(colony.getId(), action, append, x1, y1, z1, x2, y2, z2, selection));
    }

    public void sendDropOff(int x, int y, int z) {
        boolean clear = colony.isDropOffAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonyDropOff(colony.getId(), x, y, z, clear));
    }

    public void sendPickUp(int x, int y, int z) {
        boolean clear = colony.isPickUpAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonyPickUp(colony.getId(), x, y, z, clear));
    }

    public CitizenSnapshot getSingleSelected() {
        if (selection.size() != 1) {
            return null;
        }
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (selection.contains(citizen.getId())) {
                return canOpen(citizen) ? citizen : null;
            }
        }
        return null;
    }

    public static boolean canOpen(CitizenSnapshot citizen) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (citizen == null || !citizen.isLoaded() || player == null) {
            return false;
        }
        double range = PacketOpenCitizen.OPEN_RANGE;
        return player.getDistanceSq(citizen.getX(), citizen.getY(), citizen.getZ()) <= range * range;
    }

    public void openCitizen(CitizenSnapshot citizen) {
        if (!canOpen(citizen)) {
            return;
        }
        ColonyScreen.armReturn();
        GCNetwork.CHANNEL.sendToServer(new PacketOpenCitizen(colony.getId(), citizen.getId()));
    }

    public void sendGroup(String group) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenGroup(colony.getId(), group, selection));
    }

    public ModularPanel buildPanel() {
        panel = new ModularPanel("colony_view").fullScreenInvisible();
        panel.child(map.full());
        panel.child(buildHeader());
        panel.child(buildSidePanel());
        panel.child(buildHints());
        return panel;
    }

    public boolean isOverChrome(int x, int y) {
        return covers(header, x, y) || covers(sidePanel, x, y) || covers(hints, x, y);
    }

    private static boolean covers(IWidget widget, int x, int y) {
        if (widget == null || !widget.isEnabled()) {
            return false;
        }
        Area area = widget.getArea();
        return x >= area.x - MARKER_MARGIN && x <= area.x + area.width + MARKER_MARGIN
            && y >= area.y - MARKER_MARGIN
            && y <= area.y + area.height + MARKER_MARGIN;
    }

    private double sideWidth() {
        if (panel == null) {
            return SIDE_WIDTH;
        }
        double fraction = panel.getArea()
            .w() * SIDE_MAX_FRACTION;
        return Math.max(SIDE_MIN_WIDTH, Math.min(SIDE_WIDTH, fraction));
    }

    private Flow buildHeader() {
        header = Flow.column()
            .coverChildren()
            .childPadding(2)
            .padding(SIDE_PADDING)
            .pos(SCREEN_MARGIN, SCREEN_MARGIN)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(skin(PANEL_BACKGROUND, PANEL_BORDER))
            .child(label(IKey.dynamic(this::title), TITLE_COLOR))
            .child(label(IKey.dynamic(this::census), TEXT_COLOR))
            .child(label(IKey.dynamic(this::status), HINT_COLOR));
        return header;
    }

    private Flow buildHints() {
        hints = Flow.column()
            .coverChildren()
            .childPadding(2)
            .padding(SIDE_PADDING)
            .left(SCREEN_MARGIN)
            .bottom(SCREEN_MARGIN)
            .collapseDisabledChild()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(skin(PANEL_BACKGROUND, PANEL_BORDER))
            .child(helpLine(IKey.str(SELECT_HINT)))
            .child(helpLine(IKey.str(MOVE_HINT)))
            .child(helpLine(IKey.str(CAMERA_HINT)))
            .child(helpLine(IKey.str(GROUP_HINT)))
            .child(helpLine(IKey.dynamic(this::keyHint)))
            .child(label(IKey.dynamic(this::helpHint), TEXT_COLOR));
        return hints;
    }

    private TextWidget<?> helpLine(IKey key) {
        TextWidget<?> line = label(key, HINT_COLOR);
        line.setEnabledIf(widget -> helpOpen);
        return line;
    }

    private ListWidget<IWidget, ?> buildSidePanel() {
        VerticalScrollData scroll = new VerticalScrollData();
        scroll.texture(scrollHandle());
        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.scrollDirection(scroll);
        list.collapseDisabledChild();
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        list.maxSizeRelOffset(1.0F, -SCREEN_MARGIN * 2);
        list.width(this::sideWidth, Unit.Measure.PIXEL);
        list.right(SCREEN_MARGIN);
        list.top(SCREEN_MARGIN);
        list.padding(SIDE_PADDING);
        list.background(skin(PANEL_BACKGROUND, PANEL_BORDER));
        list.getScrollArea()
            .setScrollBarBackgroundColor(SCROLL_TRACK);

        list.child(section("Groups", this::groupValue, 0));
        for (int index = 0; index < MAX_GROUP_ROWS; index++) {
            list.child(buildGroupRow(index));
        }
        list.child(
            row().child(groupField())
                .child(button("Assign", ASSIGN_WIDTH, this::hasSelection, () -> sendGroup(groupField.getText()))));
        list.child(
            row().child(button("Ungroup", EXPAND, this::hasSelection, () -> sendGroup("")))
                .child(button("Select all", EXPAND, this::hasCitizens, this::selectAll)));

        list.child(section("Selection", this::selectionValue, SECTION_GAP));
        TextWidget<?> loadedLine = label(IKey.dynamic(this::loadedValue), HINT_COLOR);
        loadedLine.widthRel(1.0F);
        loadedLine.setEnabledIf(widget -> hasSelection());
        loadedLine.marginBottom(ROW_GAP);
        list.child(loadedLine);
        list.child(
            row()
                .child(
                    button(
                        "Guard",
                        EXPAND,
                        this::hasSelection,
                        () -> sendCommand(PacketCitizenCommand.GUARD, false, 0, 0, 0)))
                .child(
                    button(
                        "Cancel",
                        EXPAND,
                        this::hasSelection,
                        () -> sendCommand(PacketCitizenCommand.CANCEL, false, 0, 0, 0))));
        list.child(
            row().child(modeButton("Chop", EXPAND, TARGET_CHOP))
                .child(modeButton("Mine", EXPAND, TARGET_MINE)));
        list.child(row().child(modeButton("Farm", EXPAND, TARGET_FARM)));
        list.child(
            row().child(
                button(
                    "Inventory",
                    EXPAND,
                    () -> getSingleSelected() != null,
                    () -> openCitizen(getSingleSelected()))));

        list.child(section("Colony", () -> "", SECTION_GAP));
        list.child(
            row().child(modeButton("Drop-off", EXPAND, TARGET_DROP_OFF))
                .child(modeButton("Pick-up", EXPAND, TARGET_PICK_UP)));
        list.child(entry("drop-off", this::dropOffValue, DROP_OFF_COLOR));
        list.child(entry("pick-up", this::pickUpValue, PICK_UP_COLOR));

        TextWidget<?> targetHint = label(IKey.dynamic(this::targetingLabel), HINT_COLOR);
        targetHint.widthRel(1.0F);
        targetHint.setEnabledIf(widget -> targeting != TARGET_NONE);
        targetHint.marginTop(ROW_GAP);
        list.child(targetHint);

        sidePanel = list;
        return list;
    }

    private Flow entry(String name, Supplier<String> value, int color) {
        return Flow.row()
            .widthRel(1.0F)
            .coverChildrenHeight()
            .marginBottom(1)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(label(IKey.str(name), HINT_COLOR))
            .child(label(IKey.dynamic(() -> entryValue(name, value)), color));
    }

    private String entryValue(String name, Supplier<String> value) {
        return GuiText.trim(value.get(), innerWidth() - GuiText.width(name) - ROW_TEXT_PADDING);
    }

    private static Flow row() {
        return Flow.row()
            .widthRel(1.0F)
            .height(BUTTON_HEIGHT)
            .marginBottom(ROW_GAP)
            .childPadding(ROW_GAP);
    }

    private Flow section(String title, Supplier<String> value, int gap) {
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

    private static TextWidget<?> label(IKey key, int color) {
        return key.asWidget()
            .color(color)
            .shadow(true);
    }

    private static IDrawable skin(int fill, int border) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, fill);
            GuiDraw.drawRect(x, y, width, 1, border);
            GuiDraw.drawRect(x, y + height - 1, width, 1, border);
            GuiDraw.drawRect(x, y, 1, height, border);
            GuiDraw.drawRect(x + width - 1, y, 1, height, border);
        };
    }

    private static IDrawable scrollHandle() {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x + 1, y, width - 2, height, SCROLL_HANDLE);
        };
    }

    private static IDrawable dynamicSkin(IntSupplier fill, IntSupplier border) {
        return (context, x, y, width, height, theme) -> {
            skin(fill.getAsInt(), border.getAsInt()).draw(context, x, y, width, height, theme);
        };
    }

    private boolean hasSelection() {
        return !selection.isEmpty();
    }

    private boolean hasCitizens() {
        return !colony.getCitizens()
            .isEmpty();
    }

    private ButtonWidget<?> modeButton(String label, int width, int mode) {
        ButtonWidget<?> widget = new ButtonWidget<>();
        sizeButton(widget, width);
        widget.background(
            dynamicSkin(
                () -> targeting == mode ? ACTIVE_BACKGROUND : BUTTON_BACKGROUND,
                () -> targeting == mode ? ACTIVE_BORDER : BUTTON_BORDER));
        widget.hoverBackground(
            dynamicSkin(
                () -> targeting == mode ? ACTIVE_BACKGROUND : BUTTON_HOVER,
                () -> targeting == mode ? ACTIVE_BORDER : BUTTON_BORDER));
        widget.child(
            IKey.str(label)
                .asWidget()
                .color(() -> targeting == mode ? ACTIVE_COLOR : TEXT_COLOR)
                .shadow(true)
                .posRel(Alignment.Center));
        widget.onMousePressed(mouseButton -> {
            setTargeting(mode);
            return true;
        });
        return widget;
    }

    private ButtonWidget<?> button(String label, int width, BooleanSupplier enabled, Runnable action) {
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

    private static void sizeButton(ButtonWidget<?> widget, int width) {
        widget.height(BUTTON_HEIGHT);
        if (width == EXPAND) {
            widget.expanded();
        } else {
            widget.width(width);
        }
    }

    private String targetingLabel() {
        if (targeting == TARGET_CHOP || targeting == TARGET_FARM) {
            return "drag a region, RMB cancels";
        }
        if (targeting == TARGET_MINE) {
            return "click a chunk, RMB cancels";
        }
        if (targeting == TARGET_DROP_OFF || targeting == TARGET_PICK_UP) {
            return "click a chest, again to clear";
        }
        return "";
    }

    private String dropOffValue() {
        if (!colony.hasDropOff()) {
            return "not set";
        }
        return colony.getDropOffX() + "/" + colony.getDropOffY() + "/" + colony.getDropOffZ();
    }

    private String pickUpValue() {
        if (!colony.hasPickUp()) {
            return "not set";
        }
        return colony.getPickUpX() + "/" + colony.getPickUpY() + "/" + colony.getPickUpZ();
    }

    private TextFieldWidget groupField() {
        groupField = new TextFieldWidget();
        groupField.setMaxLength(PacketCitizenGroup.MAX_GROUP_LENGTH);
        groupField.background(skin(ROW_BACKGROUND, BUTTON_BORDER));
        groupField.height(BUTTON_HEIGHT);
        groupField.expanded();
        return groupField;
    }

    private ButtonWidget<?> buildGroupRow(int index) {
        ButtonWidget<?> row = new ButtonWidget<>();
        row.widthRel(1.0F);
        row.height(ROW_HEIGHT);
        row.marginBottom(1);
        row.background(groupRowSkin(index, ROW_BACKGROUND, ROW_SELECTED));
        row.hoverBackground(groupRowSkin(index, BUTTON_HOVER, ROW_SELECTED_HOVER));
        row.child(
            Flow.row()
                .widthRel(1.0F)
                .heightRel(1.0F)
                .paddingLeft(SWATCH_WIDTH + 4)
                .paddingRight(4)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .child(
                    IKey.dynamic(() -> groupRowLabel(index))
                        .asWidget()
                        .color(TEXT_COLOR)
                        .shadow(true))
                .child(
                    IKey.dynamic(() -> groupRowCount(index))
                        .asWidget()
                        .color(() -> groupRowCountColor(index))
                        .shadow(true)));
        row.onMousePressed(mouseButton -> {
            String group = groupAt(index);
            if (group != null) {
                selectGroup(group, Interactable.hasShiftDown());
            }
            return true;
        });
        row.setEnabled(index < groups.size());
        row.onUpdateListener(widget -> widget.setEnabled(index < groups.size()));
        return row;
    }

    private IDrawable groupRowSkin(int index, int fill, int selected) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, groupRowSelected(index) ? selected : fill);
            GuiDraw.drawRect(x, y, SWATCH_WIDTH, height, groupRowColor(index));
        };
    }

    private String groupAt(int index) {
        return index < groups.size() ? groups.get(index) : null;
    }

    private String groupRowLabel(int index) {
        String group = groupAt(index);
        if (group == null) {
            return "";
        }
        return GuiText
            .trim(group, innerWidth() - SWATCH_WIDTH - ROW_TEXT_PADDING * 3 - GuiText.width(groupRowCount(index)));
    }

    private int innerWidth() {
        return (int) sideWidth() - SIDE_PADDING * 2 - SCROLL_THICKNESS;
    }

    private int groupRowCounted(int index, boolean selectedOnly) {
        String group = groupAt(index);
        if (group == null) {
            return 0;
        }
        int count = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (groupLabel(citizen).equals(group) && (!selectedOnly || selection.contains(citizen.getId()))) {
                count++;
            }
        }
        return count;
    }

    private String groupRowCount(int index) {
        if (groupAt(index) == null) {
            return "";
        }
        return groupRowCounted(index, true) + "/" + groupRowCounted(index, false);
    }

    private boolean groupRowSelected(int index) {
        int total = groupRowCounted(index, false);
        return total > 0 && groupRowCounted(index, true) == total;
    }

    private int groupRowCountColor(int index) {
        return groupRowCounted(index, true) > 0 ? ACTIVE_COLOR : HINT_COLOR;
    }

    private int groupRowColor(int index) {
        String group = groupAt(index);
        if (group == null) {
            return SECTION_LINE;
        }
        return ColonyWorldOverlay.groupColor(UNGROUPED.equals(group) ? "" : group) | 0xFF000000;
    }

    private static String groupLabel(CitizenSnapshot citizen) {
        return citizen.getGroup()
            .isEmpty() ? UNGROUPED : citizen.getGroup();
    }

    private String title() {
        return colony.getName() + " #" + colony.getId();
    }

    private String status() {
        return "owner " + colony.getOwnerName()
            + "   dim "
            + colony.getDimension()
            + "   "
            + colony.getX()
            + "/"
            + colony.getY()
            + "/"
            + colony.getZ()
            + "   r"
            + colony.getRadius();
    }

    private String census() {
        int loaded = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.isLoaded()) {
                loaded++;
            }
        }
        return colony.getCitizens()
            .size() + " citizens   "
            + loaded
            + " loaded   "
            + colony.getOrderCount()
            + " orders";
    }

    private String groupValue() {
        return groups.size() + " groups";
    }

    private String selectionValue() {
        if (selection.isEmpty()) {
            return "none";
        }
        return selection.size() + "/"
            + colony.getCitizens()
                .size();
    }

    private String loadedValue() {
        return getSelectedLoaded() + " of " + selection.size() + " loaded";
    }

    private String helpHint() {
        return (helpOpen ? "H hide help   " : "H help   ") + ControllingCompat.describe(GCKeyBindings.openColony)
            + " close";
    }

    private String keyHint() {
        return "R recenter   ctrl+A all   G guard   C cancel   I inventory";
    }
}
