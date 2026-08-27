package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
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
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;
import com.enn3developer.gregcolonies.network.PacketCitizenGroup;
import com.enn3developer.gregcolonies.network.PacketCitizenJob;
import com.enn3developer.gregcolonies.network.PacketColonyBuild;
import com.enn3developer.gregcolonies.network.PacketColonyDropOff;
import com.enn3developer.gregcolonies.network.PacketColonyMaterials;
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

    public static final int TARGET_MATERIALS = 6;

    public static final int TARGET_BLUEPRINT = 7;

    public static final int TARGET_BUILD = 8;

    private static final int MAX_GROUP_ROWS = 32;

    private static final int SIDE_WIDTH = 168;

    private static final int SIDE_MIN_WIDTH = 124;

    private static final float SIDE_MAX_FRACTION = 0.38F;

    private static final int ASSIGN_WIDTH = 46;

    private static final int DROP_OFF_COLOR = 0xFFFF7CE0;

    private static final int PICK_UP_COLOR = 0xFF7CE0FF;

    private static final int MATERIALS_COLOR = 0xFFFFC46B;

    private static final int BUILD_COLOR = 0xFF9CE06B;

    private static final String SELECT_HINT = "LMB select   LMB drag box   shift add   LMB twice opens";

    private static final String MOVE_HINT = "RMB move   RMB drag pan   MMB drag turn";

    private static final String CAMERA_HINT = "scroll zoom   WASD pan   Q/E turn";

    private static final String GROUP_HINT = "click a group to select it   shift adds";

    private static final int MARKER_MARGIN = 7;

    private static final int[] REGION = new int[5];

    private static boolean hasRegion;

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

    public void sendMaterials(int x, int y, int z) {
        boolean clear = colony.isMaterialsAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonyMaterials(colony.getId(), x, y, z, clear));
    }

    public void setRegion(int x1, int y, int z1, int x2, int z2) {
        REGION[0] = Math.min(x1, x2);
        REGION[1] = Math.min(z1, z2);
        REGION[2] = Math.max(x1, x2);
        REGION[3] = Math.max(z1, z2);
        REGION[4] = y;
        hasRegion = true;
    }

    public boolean hasRegion() {
        return hasRegion;
    }

    public int getRegionX1() {
        return REGION[0];
    }

    public int getRegionZ1() {
        return REGION[1];
    }

    public int getRegionX2() {
        return REGION[2];
    }

    public int getRegionZ2() {
        return REGION[3];
    }

    public int getRegionY() {
        return hasRegion ? REGION[4] : colony.getY();
    }

    public int getRegionWidth() {
        return REGION[2] - REGION[0] + 1;
    }

    public int getRegionDepth() {
        return REGION[3] - REGION[1] + 1;
    }

    public void openBlueprints() {
        BlueprintScreen.open(this);
    }

    public void sendBuild(int x, int y, int z) {
        boolean clear = colony.isBuildSiteAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonyBuild(colony.getId(), x, y, z, clear));
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

    public void sendJob(CitizenJob job) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenJob(colony.getId(), job, selection));
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
            .padding(GuiStyle.PADDING)
            .pos(GuiStyle.SCREEN_MARGIN, GuiStyle.SCREEN_MARGIN)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(GuiStyle.skin(GuiStyle.PANEL_BACKGROUND, GuiStyle.PANEL_BORDER))
            .child(GuiStyle.label(IKey.dynamic(this::title), GuiStyle.TITLE_COLOR))
            .child(GuiStyle.label(IKey.dynamic(this::census), GuiStyle.TEXT_COLOR))
            .child(GuiStyle.label(IKey.dynamic(this::status), GuiStyle.HINT_COLOR));
        return header;
    }

    private Flow buildHints() {
        hints = Flow.column()
            .coverChildren()
            .childPadding(2)
            .padding(GuiStyle.PADDING)
            .left(GuiStyle.SCREEN_MARGIN)
            .bottom(GuiStyle.SCREEN_MARGIN)
            .collapseDisabledChild()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(GuiStyle.skin(GuiStyle.PANEL_BACKGROUND, GuiStyle.PANEL_BORDER))
            .child(helpLine(IKey.str(SELECT_HINT)))
            .child(helpLine(IKey.str(MOVE_HINT)))
            .child(helpLine(IKey.str(CAMERA_HINT)))
            .child(helpLine(IKey.str(GROUP_HINT)))
            .child(helpLine(IKey.dynamic(this::keyHint)))
            .child(GuiStyle.label(IKey.dynamic(this::helpHint), GuiStyle.TEXT_COLOR));
        return hints;
    }

    private TextWidget<?> helpLine(IKey key) {
        TextWidget<?> line = GuiStyle.label(key, GuiStyle.HINT_COLOR);
        line.setEnabledIf(widget -> helpOpen);
        return line;
    }

    private ListWidget<IWidget, ?> buildSidePanel() {
        VerticalScrollData scroll = new VerticalScrollData();
        scroll.texture(GuiStyle.scrollHandle());
        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.scrollDirection(scroll);
        list.collapseDisabledChild();
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        list.maxSizeRelOffset(1.0F, -GuiStyle.SCREEN_MARGIN * 2);
        list.width(this::sideWidth, Unit.Measure.PIXEL);
        list.right(GuiStyle.SCREEN_MARGIN);
        list.top(GuiStyle.SCREEN_MARGIN);
        list.padding(GuiStyle.PADDING);
        list.background(GuiStyle.skin(GuiStyle.PANEL_BACKGROUND, GuiStyle.PANEL_BORDER));
        list.getScrollArea()
            .setScrollBarBackgroundColor(GuiStyle.SCROLL_TRACK);

        list.child(GuiStyle.section("Groups", this::groupValue, 0));
        for (int index = 0; index < MAX_GROUP_ROWS; index++) {
            list.child(buildGroupRow(index));
        }
        list.child(
            GuiStyle.row()
                .child(groupField())
                .child(
                    GuiStyle
                        .button("Assign", ASSIGN_WIDTH, this::hasSelection, () -> sendGroup(groupField.getText()))));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Ungroup", GuiStyle.EXPAND, this::hasSelection, () -> sendGroup("")))
                .child(GuiStyle.button("Select all", GuiStyle.EXPAND, this::hasCitizens, this::selectAll)));

        list.child(GuiStyle.section("Selection", this::selectionValue, GuiStyle.SECTION_GAP));
        TextWidget<?> loadedLine = GuiStyle.label(IKey.dynamic(this::loadedValue), GuiStyle.HINT_COLOR);
        loadedLine.widthRel(1.0F);
        loadedLine.setEnabledIf(widget -> hasSelection());
        loadedLine.marginBottom(GuiStyle.ROW_GAP);
        list.child(loadedLine);
        list.child(
            GuiStyle.row()
                .child(
                    GuiStyle.button(
                        "Guard",
                        GuiStyle.EXPAND,
                        this::hasSelection,
                        () -> sendCommand(PacketCitizenCommand.GUARD, false, 0, 0, 0)))
                .child(
                    GuiStyle.button(
                        "Cancel",
                        GuiStyle.EXPAND,
                        this::hasSelection,
                        () -> sendCommand(PacketCitizenCommand.CANCEL, false, 0, 0, 0))));
        list.child(
            GuiStyle.row()
                .child(modeButton("Chop", GuiStyle.EXPAND, TARGET_CHOP))
                .child(modeButton("Mine", GuiStyle.EXPAND, TARGET_MINE)));
        list.child(
            GuiStyle.row()
                .child(modeButton("Farm", GuiStyle.EXPAND, TARGET_FARM)));
        list.child(
            GuiStyle.row()
                .child(
                    GuiStyle.button(
                        "Inventory",
                        GuiStyle.EXPAND,
                        () -> getSingleSelected() != null,
                        () -> openCitizen(getSingleSelected()))));

        list.child(GuiStyle.section("Colony", () -> "", GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(modeButton("Drop-off", GuiStyle.EXPAND, TARGET_DROP_OFF))
                .child(modeButton("Pick-up", GuiStyle.EXPAND, TARGET_PICK_UP)));
        list.child(
            GuiStyle.row()
                .child(modeButton("Materials", GuiStyle.EXPAND, TARGET_MATERIALS)));
        list.child(entry("drop-off", this::dropOffValue, DROP_OFF_COLOR));
        list.child(entry("pick-up", this::pickUpValue, PICK_UP_COLOR));
        list.child(entry("materials", this::materialsValue, MATERIALS_COLOR));

        list.child(GuiStyle.section("Build", this::buildValue, GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(modeButton("Blueprint", GuiStyle.EXPAND, TARGET_BLUEPRINT))
                .child(modeButton("Build", GuiStyle.EXPAND, TARGET_BUILD)));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Blueprints", GuiStyle.EXPAND, () -> true, this::openBlueprints)));
        list.child(
            GuiStyle.row()
                .child(
                    GuiStyle.button("Builder", GuiStyle.EXPAND, this::hasSelection, () -> sendJob(CitizenJob.BUILDER)))
                .child(GuiStyle.button("No job", GuiStyle.EXPAND, this::hasSelection, () -> sendJob(CitizenJob.NONE))));
        list.child(entry("blueprint", this::blueprintValue, BUILD_COLOR));
        list.child(entry("builders", this::buildersValue, BUILD_COLOR));
        list.child(entry("site", this::buildSiteValue, BUILD_COLOR));

        TextWidget<?> targetHint = GuiStyle.label(IKey.dynamic(this::targetingLabel), GuiStyle.HINT_COLOR);
        targetHint.widthRel(1.0F);
        targetHint.setEnabledIf(widget -> targeting != TARGET_NONE);
        targetHint.marginTop(GuiStyle.ROW_GAP);
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
            .child(GuiStyle.label(IKey.str(name), GuiStyle.HINT_COLOR))
            .child(GuiStyle.label(IKey.dynamic(() -> entryValue(name, value)), color));
    }

    private String entryValue(String name, Supplier<String> value) {
        return GuiText.trim(value.get(), innerWidth() - GuiText.width(name) - GuiStyle.ROW_TEXT_PADDING);
    }

    private String buildersValue() {
        int builders = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.getJob() == CitizenJob.BUILDER) {
                builders++;
            }
        }
        return builders == 0 ? "none" : String.valueOf(builders);
    }

    private boolean hasSelection() {
        return !selection.isEmpty();
    }

    private boolean hasCitizens() {
        return !colony.getCitizens()
            .isEmpty();
    }

    private ButtonWidget<?> modeButton(String label, int width, int mode) {
        return GuiStyle.toggleButton(() -> label, width, () -> targeting == mode, () -> setTargeting(mode));
    }

    private String targetingLabel() {
        if (targeting == TARGET_BLUEPRINT) {
            return "drag the footprint, RMB cancels";
        }
        if (targeting == TARGET_CHOP || targeting == TARGET_FARM) {
            return "drag a region, RMB cancels";
        }
        if (targeting == TARGET_BUILD) {
            return "click the ground, again to clear";
        }
        if (targeting == TARGET_MINE) {
            return "click a chunk, RMB cancels";
        }
        if (targeting == TARGET_DROP_OFF || targeting == TARGET_PICK_UP || targeting == TARGET_MATERIALS) {
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

    private String materialsValue() {
        if (!colony.hasMaterials()) {
            return "not set";
        }
        return colony.getMaterialsX() + "/" + colony.getMaterialsY() + "/" + colony.getMaterialsZ();
    }

    private String blueprintValue() {
        ColonySnapshot.BlueprintEntry entry = colony.getBlueprint(colony.getActiveBlueprint());
        if (entry == null) {
            return "none";
        }
        return entry.getLabel(colony.getActiveBlueprint());
    }

    private String buildSiteValue() {
        if (!colony.hasBuildSite()) {
            return "none";
        }
        return colony.getBuildX() + "/" + colony.getBuildY() + "/" + colony.getBuildZ();
    }

    private String buildValue() {
        if (!colony.hasBuildSite()) {
            ColonySnapshot.BlueprintEntry entry = colony.getBlueprint(colony.getActiveBlueprint());
            return entry == null ? "" : entry.getBlocks() + " blocks";
        }
        return colony.getBuildTotal() - colony.getBuildRemaining() + "/" + colony.getBuildTotal();
    }

    private TextFieldWidget groupField() {
        groupField = GuiStyle.field(PacketCitizenGroup.MAX_GROUP_LENGTH);
        groupField.expanded();
        return groupField;
    }

    private ButtonWidget<?> buildGroupRow(int index) {
        ButtonWidget<?> row = new ButtonWidget<>();
        row.widthRel(1.0F);
        row.height(GuiStyle.ROW_HEIGHT);
        row.marginBottom(1);
        row.background(groupRowSkin(index, GuiStyle.ROW_BACKGROUND, GuiStyle.ROW_SELECTED));
        row.hoverBackground(groupRowSkin(index, GuiStyle.BUTTON_HOVER, GuiStyle.ROW_SELECTED_HOVER));
        row.child(
            Flow.row()
                .widthRel(1.0F)
                .heightRel(1.0F)
                .paddingLeft(GuiStyle.SWATCH_WIDTH + 4)
                .paddingRight(4)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .child(
                    IKey.dynamic(() -> groupRowLabel(index))
                        .asWidget()
                        .color(GuiStyle.TEXT_COLOR)
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
            GuiDraw.drawRect(x, y, GuiStyle.SWATCH_WIDTH, height, groupRowColor(index));
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
        return GuiText.trim(
            group,
            innerWidth() - GuiStyle.SWATCH_WIDTH - GuiStyle.ROW_TEXT_PADDING * 3 - GuiText.width(groupRowCount(index)));
    }

    private int innerWidth() {
        return (int) sideWidth() - GuiStyle.PADDING * 2 - GuiStyle.SCROLL_THICKNESS;
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
        return groupRowCounted(index, true) > 0 ? GuiStyle.ACTIVE_COLOR : GuiStyle.HINT_COLOR;
    }

    private int groupRowColor(int index) {
        String group = groupAt(index);
        if (group == null) {
            return GuiStyle.SECTION_LINE;
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
