package com.enn3developer.gregcolonies.client.gui;

import java.util.Set;
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
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.enn3developer.gregcolonies.client.ControllingCompat;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySite;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;
import com.enn3developer.gregcolonies.network.PacketCitizenGroup;
import com.enn3developer.gregcolonies.network.PacketCitizenJob;
import com.enn3developer.gregcolonies.network.PacketColonyBuild;
import com.enn3developer.gregcolonies.network.PacketColonyHome;
import com.enn3developer.gregcolonies.network.PacketColonySite;
import com.enn3developer.gregcolonies.network.PacketOpenCitizen;

public class ColonyView extends OverlayView {

    public static final String UNGROUPED = ColonySelection.UNGROUPED;

    private static final int BUILD_LABEL_COLOR = TargetMode.BUILD.color(TargetMode.LABEL_ALPHA);

    private static final int MAX_GROUP_ROWS = 32;

    private static final int MAX_HOME_ROWS = Colony.MAX_HOMES;

    private static final int HOME_LABEL_COLOR = TargetMode.HOME.color(TargetMode.LABEL_ALPHA);

    private static final int SIDE_WIDTH = 168;

    private static final int SIDE_MIN_WIDTH = 124;

    private static final float SIDE_MAX_FRACTION = 0.38F;

    private static final int ASSIGN_WIDTH = 46;

    private static final String SELECT_HINT = "LMB select   LMB drag box   shift add   LMB twice opens";

    private static final String MOVE_HINT = "RMB move   RMB drag pan   MMB drag turn";

    private static final String CAMERA_HINT = "scroll zoom   WASD pan   Q/E turn";

    private static final String GROUP_HINT = "click a group to select it   shift adds";

    private static final int MARKER_MARGIN = 7;

    private final ColonyViewWidget map = new ColonyViewWidget(this);

    private final ColonySelection selection = new ColonySelection();

    private TextFieldWidget groupField;

    private ModularPanel panel;

    private ListWidget<IWidget, ?> sidePanel;

    public ColonyView(ColonySnapshot colony) {
        super(false);
        setColony(colony);
    }

    public ColonySnapshot getColony() {
        return selection.getColony();
    }

    public void setColony(ColonySnapshot colony) {
        selection.setColony(colony);
    }

    public boolean isSelected(UUID id) {
        return selection.isSelected(id);
    }

    public Set<UUID> getSelection() {
        return selection.get();
    }

    public void clearSelection() {
        selection.clear();
    }

    public void toggle(UUID id) {
        selection.toggle(id);
    }

    public void selectAll() {
        selection.selectAll();
    }

    public void selectGroup(String group, boolean add) {
        selection.selectGroup(group, add);
    }

    public int getSelectedLoaded() {
        return selection.countLoaded();
    }

    public TargetMode getTargeting() {
        return selection.getTargeting();
    }

    public void setTargeting(TargetMode mode) {
        selection.setTargeting(mode);
    }

    public boolean hasPending() {
        return selection.hasPending();
    }

    public int[] getPending() {
        return selection.getPending();
    }

    public void setPending(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        selection.setPending(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void clearPending() {
        selection.clearPending();
    }

    public boolean isEditing() {
        return groupField != null && groupField.isFocused();
    }

    public void sendCommand(byte action, boolean append, int x, int y, int z) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL
            .sendToServer(new PacketCitizenCommand(getColony().getId(), action, append, x, y, z, selection.get()));
    }

    public void sendArea(byte action, boolean append, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(
            new PacketCitizenCommand(getColony().getId(), action, append, x1, y1, z1, x2, y2, z2, selection.get()));
    }

    public void sendSite(ColonySiteKind kind, int x, int y, int z) {
        boolean clear = getColony().site(kind)
            .isAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonySite(getColony().getId(), kind, x, y, z, clear));
    }

    public void sendHome(int x1, int y1, int z1, int x2, int y2, int z2) {
        ColonySnapshot.HomeEntry home = getColony().homeAt(x1, y1, z1);
        if (home != null) {
            GCNetwork.CHANNEL.sendToServer(PacketColonyHome.clear(getColony().getId(), home.getId()));
            return;
        }
        GCNetwork.CHANNEL.sendToServer(PacketColonyHome.set(getColony().getId(), x1, y1, z1, x2, y2, z2));
    }

    public void openBlueprints() {
        BlueprintScreen.open(this);
    }

    public void sendBuild(int x, int y, int z) {
        boolean clear = getColony().isBuildSiteAt(x, y, z);
        GCNetwork.CHANNEL.sendToServer(new PacketColonyBuild(getColony().getId(), x, y, z, clear));
    }

    public CitizenSnapshot getSingleSelected() {
        CitizenSnapshot citizen = selection.single();
        return canOpen(citizen) ? citizen : null;
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
        GCNetwork.CHANNEL.sendToServer(new PacketOpenCitizen(getColony().getId(), citizen.getId()));
    }

    public void sendGroup(String group) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenGroup(getColony().getId(), group, selection.get()));
    }

    public void sendJob(CitizenJob job) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenJob(getColony().getId(), job, selection.get()));
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
        return covers(getHeader(), x, y) || covers(sidePanel, x, y) || covers(getHints(), x, y);
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

    private ListWidget<IWidget, ?> buildSidePanel() {
        ListWidget<IWidget, ?> list = GuiStyle.panelList();
        list.maxSizeRelOffset(1.0F, -GuiStyle.SCREEN_MARGIN * 2);
        list.width(this::sideWidth, Unit.Measure.PIXEL);
        list.right(GuiStyle.SCREEN_MARGIN);
        list.top(GuiStyle.SCREEN_MARGIN);

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
                .child(modeButton(TargetMode.CHOP))
                .child(modeButton(TargetMode.MINE)));
        list.child(
            GuiStyle.row()
                .child(modeButton(TargetMode.FARM)));
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
                .child(modeButton(TargetMode.DROP_OFF))
                .child(modeButton(TargetMode.PICK_UP)));
        list.child(
            GuiStyle.row()
                .child(modeButton(TargetMode.MATERIALS)));
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            list.child(
                entry(
                    kind.getShortLabel(),
                    () -> siteValue(kind),
                    TargetMode.of(kind)
                        .color(TargetMode.LABEL_ALPHA)));
        }

        list.child(GuiStyle.section("Homes", this::homesValue, GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(modeButton(TargetMode.HOME)));
        for (int index = 0; index < MAX_HOME_ROWS; index++) {
            list.child(buildHomeRow(index));
        }

        list.child(GuiStyle.section("Build", this::buildValue, GuiStyle.SECTION_GAP));
        list.child(
            GuiStyle.row()
                .child(modeButton(TargetMode.BUILD)));
        list.child(
            GuiStyle.row()
                .child(GuiStyle.button("Blueprints", GuiStyle.EXPAND, () -> true, this::openBlueprints)));
        list.child(
            GuiStyle.row()
                .child(
                    GuiStyle.button("Builder", GuiStyle.EXPAND, this::hasSelection, () -> sendJob(CitizenJob.BUILDER)))
                .child(GuiStyle.button("No job", GuiStyle.EXPAND, this::hasSelection, () -> sendJob(CitizenJob.NONE))));
        list.child(entry("blueprint", this::blueprintValue, BUILD_LABEL_COLOR));
        list.child(entry("builders", this::buildersValue, BUILD_LABEL_COLOR));
        list.child(entry("site", this::buildSiteValue, BUILD_LABEL_COLOR));

        TextWidget<?> targetHint = GuiStyle.label(IKey.dynamic(this::targetingLabel), GuiStyle.HINT_COLOR);
        targetHint.widthRel(1.0F);
        targetHint.setEnabledIf(widget -> getTargeting() != TargetMode.NONE);
        targetHint.marginTop(GuiStyle.ROW_GAP);
        list.child(targetHint);

        sidePanel = list;
        return list;
    }

    private IWidget buildHomeRow(int index) {
        Flow row = entry(() -> homeLabel(index), () -> homeValue(index), HOME_LABEL_COLOR);
        row.setEnabledIf(widget -> getColony().getHome(index) != null);
        return row;
    }

    private String homeLabel(int index) {
        ColonySnapshot.HomeEntry home = getColony().getHome(index);
        return home == null ? "" : home.getLabel();
    }

    private String homeValue(int index) {
        ColonySnapshot.HomeEntry home = getColony().getHome(index);
        return home == null ? "" : home.getValue();
    }

    private String homesValue() {
        int beds = 0;
        int occupants = 0;
        for (ColonySnapshot.HomeEntry home : getColony().getHomes()) {
            beds += home.getBeds();
            occupants += home.getOccupants();
        }
        return getColony().getHomes()
            .size() + " homes   " + occupants + "/" + beds + " beds";
    }

    private Flow entry(String name, Supplier<String> value, int color) {
        return entry(() -> name, value, color);
    }

    private Flow entry(Supplier<String> name, Supplier<String> value, int color) {
        return Flow.row()
            .widthRel(1.0F)
            .coverChildrenHeight()
            .marginBottom(1)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(GuiStyle.label(IKey.dynamic(name::get), GuiStyle.HINT_COLOR))
            .child(GuiStyle.label(IKey.dynamic(() -> entryValue(name.get(), value)), color));
    }

    private String entryValue(String name, Supplier<String> value) {
        return GuiText.fit(value.get(), name, innerWidth() - GuiStyle.ROW_TEXT_PADDING);
    }

    private String buildersValue() {
        int builders = 0;
        for (CitizenSnapshot citizen : getColony().getCitizens()) {
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
        return !getColony().getCitizens()
            .isEmpty();
    }

    private ButtonWidget<?> modeButton(TargetMode mode) {
        return GuiStyle
            .toggleButton(mode::getLabel, GuiStyle.EXPAND, () -> getTargeting() == mode, () -> setTargeting(mode));
    }

    private String targetingLabel() {
        return getTargeting().getHint();
    }

    private String siteValue(ColonySiteKind kind) {
        ColonySite site = getColony().site(kind);
        return site.isPresent() ? site.describe() : "not set";
    }

    private String blueprintValue() {
        ColonySnapshot.BlueprintEntry entry = getColony().getBlueprint(getColony().getActiveBlueprint());
        if (entry == null) {
            return "none";
        }
        return entry.getLabel(getColony().getActiveBlueprint());
    }

    private String buildSiteValue() {
        if (!getColony().hasBuildSite()) {
            return "none";
        }
        return getColony().getBuildX() + "/" + getColony().getBuildY() + "/" + getColony().getBuildZ();
    }

    private String buildValue() {
        if (!getColony().hasBuildSite()) {
            ColonySnapshot.BlueprintEntry entry = getColony().getBlueprint(getColony().getActiveBlueprint());
            return entry == null ? "" : entry.getBlocks() + " blocks";
        }
        return getColony().getBuildTotal() - getColony().getBuildRemaining() + "/" + getColony().getBuildTotal();
    }

    private TextFieldWidget groupField() {
        groupField = GuiStyle.field(PacketCitizenGroup.MAX_GROUP_LENGTH);
        groupField.expanded();
        return groupField;
    }

    private IWidget buildGroupRow(int index) {
        return GuiRow.at(
            index,
            () -> selection.getGroups()
                .size())
            .marginBottom(1)
            .padding(GuiStyle.SWATCH_WIDTH + 4, 4)
            .label(() -> groupRowLabel(index))
            .hint(() -> groupRowCount(index), () -> groupRowCountColor(index))
            .skin(
                groupRowSkin(index, GuiStyle.ROW_BACKGROUND, GuiStyle.ROW_SELECTED),
                groupRowSkin(index, GuiStyle.BUTTON_HOVER, GuiStyle.ROW_SELECTED_HOVER))
            .onClick(
                row -> selectGroup(
                    selection.getGroups()
                        .get(row),
                    Interactable.hasShiftDown()))
            .build();
    }

    private IDrawable groupRowSkin(int index, int fill, int selected) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, groupRowSelected(index) ? selected : fill);
            GuiDraw.drawRect(x, y, GuiStyle.SWATCH_WIDTH, height, groupRowColor(index));
        };
    }

    private String groupAt(int index) {
        return selection.groupAt(index);
    }

    private String groupRowLabel(int index) {
        String group = groupAt(index);
        if (group == null) {
            return "";
        }
        return GuiText
            .fit(group, groupRowCount(index), innerWidth() - GuiStyle.SWATCH_WIDTH - GuiStyle.ROW_TEXT_PADDING * 3);
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
        for (CitizenSnapshot citizen : getColony().getCitizens()) {
            if (groupLabel(citizen).equals(group) && (!selectedOnly || selection.isSelected(citizen.getId()))) {
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
        return ColonySelection.groupLabel(citizen);
    }

    @Override
    protected String title() {
        return getColony().getName() + " #" + getColony().getId();
    }

    @Override
    protected String headerHint() {
        return "owner " + getColony().getOwnerName()
            + "   dim "
            + getColony().getDimension()
            + "   "
            + getColony().getX()
            + "/"
            + getColony().getY()
            + "/"
            + getColony().getZ()
            + "   r"
            + getColony().getRadius();
    }

    @Override
    protected String headerLine() {
        int loaded = 0;
        for (CitizenSnapshot citizen : getColony().getCitizens()) {
            if (citizen.isLoaded()) {
                loaded++;
            }
        }
        return getColony().getCitizens()
            .size() + " citizens   " + loaded + " loaded   " + getColony().getOrderCount() + " orders";
    }

    private String groupValue() {
        return selection.getGroups()
            .size() + " groups";
    }

    private String selectionValue() {
        if (selection.isEmpty()) {
            return "none";
        }
        return selection.size() + "/"
            + getColony().getCitizens()
                .size();
    }

    private String loadedValue() {
        return getSelectedLoaded() + " of " + selection.size() + " loaded";
    }

    @Override
    protected String helpHint() {
        return (isHelpOpen() ? "H hide help   " : "H help   ") + ControllingCompat.describe(GCKeyBindings.openColony)
            + " close";
    }

    @Override
    protected IKey[] hintLines() {
        return new IKey[] { text(SELECT_HINT), text(MOVE_HINT), text(CAMERA_HINT), text(GROUP_HINT),
            text("R recenter   ctrl+A all   G guard   C cancel   I inventory") };
    }
}
