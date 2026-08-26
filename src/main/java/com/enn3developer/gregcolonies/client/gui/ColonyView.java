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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
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

    public static final int TARGET_DROP_OFF = 3;

    public static final int TARGET_PICK_UP = 4;

    private static final int MAX_GROUP_ROWS = 10;

    private static final int SIDE_WIDTH = 164;

    private static final int SIDE_PADDING = 6;

    private static final int INNER_WIDTH = SIDE_WIDTH - SIDE_PADDING * 2;

    private static final int HALF_WIDTH = (INNER_WIDTH - 3) / 2;

    private static final int ASSIGN_WIDTH = 46;

    private static final int ROW_HEIGHT = 13;

    private static final int BUTTON_HEIGHT = 15;

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

    private static final int ROW_BACKGROUND = 0xFF1B2233;

    private static final int ACTIVE_BACKGROUND = 0xFF27543A;

    private static final int ACTIVE_BORDER = 0xFF4FA05F;

    private static final int ACTIVE_COLOR = 0xFFA8F0A8;

    private static final int DROP_OFF_COLOR = 0xFFFF7CE0;

    private static final int PICK_UP_COLOR = 0xFF7CE0FF;

    private static final String SELECT_HINT = "LMB select   LMB drag box   shift add   LMB twice opens";

    private static final String MOVE_HINT = "RMB move   RMB drag pan   MMB drag turn";

    private static final String CAMERA_HINT = "scroll zoom   WASD pan   Q/E turn";

    private static final String GROUP_HINT = "click a group to select it   shift adds";

    private static final int HINT_LINE = 11;

    private static final int MARKER_MARGIN = 7;

    private static final int HELP_HEIGHT = HINT_LINE * 5 + 11;

    private static final int HELP_BOTTOM = 6 + HINT_LINE + 12;

    private final ColonyViewWidget map = new ColonyViewWidget(this);

    private final Set<UUID> selection = new LinkedHashSet<>();

    private final List<String> groups = new ArrayList<>();

    private ColonySnapshot colony;

    private TextFieldWidget groupField;

    private final int[] pending = new int[6];

    private int targeting = TARGET_NONE;

    private boolean hasPending;

    private boolean helpOpen;

    private Flow header;

    private Flow sidePanel;

    private Flow hints;

    private Flow help;

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
        ModularPanel panel = new ModularPanel("colony_view").fullScreenInvisible();
        panel.child(map.full());
        panel.child(buildHeader());
        panel.child(buildSidePanel());
        panel.child(buildHelp());
        panel.child(buildHints());
        return panel;
    }

    public boolean isOverChrome(int x, int y) {
        return covers(header, x, y) || covers(sidePanel, x, y) || covers(hints, x, y) || covers(help, x, y);
    }

    private static boolean covers(Flow widget, int x, int y) {
        if (widget == null || !widget.isEnabled()) {
            return false;
        }
        Area area = widget.getArea();
        return x >= area.x - MARKER_MARGIN && x <= area.x + area.width + MARKER_MARGIN
            && y >= area.y - MARKER_MARGIN
            && y <= area.y + area.height + MARKER_MARGIN;
    }

    private Flow buildHeader() {
        header = Flow.column()
            .coverChildren()
            .childPadding(2)
            .padding(6)
            .pos(6, 6)
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
            .padding(6)
            .left(6)
            .bottom(6)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(skin(PANEL_BACKGROUND, PANEL_BORDER))
            .child(label(IKey.dynamic(this::helpHint), TEXT_COLOR));
        return hints;
    }

    private Flow buildHelp() {
        help = Flow.column()
            .coverChildrenWidth()
            .height(HELP_HEIGHT)
            .childPadding(2)
            .padding(6)
            .left(6)
            .bottom(HELP_BOTTOM)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(skin(PANEL_BACKGROUND, PANEL_BORDER))
            .setEnabledIf(widget -> helpOpen)
            .child(label(IKey.str(SELECT_HINT), HINT_COLOR))
            .child(label(IKey.str(MOVE_HINT), HINT_COLOR))
            .child(label(IKey.str(CAMERA_HINT), HINT_COLOR))
            .child(label(IKey.str(GROUP_HINT), HINT_COLOR))
            .child(label(IKey.dynamic(this::keyHint), HINT_COLOR));
        return help;
    }

    private Flow buildSidePanel() {
        sidePanel = Flow.column()
            .width(SIDE_WIDTH)
            .coverChildrenHeight()
            .childPadding(3)
            .padding(SIDE_PADDING)
            .right(6)
            .top(6)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .background(skin(PANEL_BACKGROUND, PANEL_BORDER))
            .child(section("Groups", this::groupValue))
            .child(
                Flow.column()
                    .coverChildrenHeight()
                    .widthRel(1.0F)
                    .childPadding(1)
                    .collapseDisabledChild()
                    .children(MAX_GROUP_ROWS, this::buildGroupRow))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(groupField())
                    .child(button("Assign", ASSIGN_WIDTH, this::hasSelection, () -> sendGroup(groupField.getText()))))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(button("Ungroup", HALF_WIDTH, this::hasSelection, () -> sendGroup("")))
                    .child(button("Select all", HALF_WIDTH, () -> true, this::selectAll)))
            .child(section("Selection", this::selectionValue))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(
                        button(
                            "Guard",
                            HALF_WIDTH,
                            this::hasSelection,
                            () -> sendCommand(PacketCitizenCommand.GUARD, false, 0, 0, 0)))
                    .child(
                        button(
                            "Cancel",
                            HALF_WIDTH,
                            this::hasSelection,
                            () -> sendCommand(PacketCitizenCommand.CANCEL, false, 0, 0, 0))))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(modeButton("Chop", HALF_WIDTH, TARGET_CHOP))
                    .child(modeButton("Mine", HALF_WIDTH, TARGET_MINE)))
            .child(
                button(
                    "Inventory",
                    INNER_WIDTH,
                    () -> getSingleSelected() != null,
                    () -> openCitizen(getSingleSelected())))
            .child(section("Colony", () -> ""))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(modeButton("Drop-off", HALF_WIDTH, TARGET_DROP_OFF))
                    .child(modeButton("Pick-up", HALF_WIDTH, TARGET_PICK_UP)))
            .child(label(IKey.dynamic(this::dropOffLabel), DROP_OFF_COLOR))
            .child(label(IKey.dynamic(this::pickUpLabel), PICK_UP_COLOR))
            .child(label(IKey.dynamic(this::targetingLabel), HINT_COLOR));
        return sidePanel;
    }

    private Flow section(String title, java.util.function.Supplier<String> value) {
        return Flow.column()
            .widthRel(1.0F)
            .coverChildrenHeight()
            .marginTop(3)
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
                    .background(new com.cleanroommc.modularui.drawable.Rectangle().color(SECTION_LINE)));
    }

    private static TextWidget label(IKey key, int color) {
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

    private static IDrawable dynamicSkin(IntSupplier fill, IntSupplier border) {
        return (context, x, y, width, height, theme) -> {
            skin(fill.getAsInt(), border.getAsInt()).draw(context, x, y, width, height, theme);
        };
    }

    private boolean hasSelection() {
        return !selection.isEmpty();
    }

    private ButtonWidget<?> modeButton(String label, int width, int mode) {
        return new ButtonWidget<>().size(width, BUTTON_HEIGHT)
            .background(
                dynamicSkin(
                    () -> targeting == mode ? ACTIVE_BACKGROUND : BUTTON_BACKGROUND,
                    () -> targeting == mode ? ACTIVE_BORDER : BUTTON_BORDER))
            .hoverBackground(
                dynamicSkin(
                    () -> targeting == mode ? ACTIVE_BACKGROUND : BUTTON_HOVER,
                    () -> targeting == mode ? ACTIVE_BORDER : BUTTON_BORDER))
            .child(
                IKey.str(label)
                    .asWidget()
                    .color(() -> targeting == mode ? ACTIVE_COLOR : TEXT_COLOR)
                    .shadow(true)
                    .posRel(Alignment.Center))
            .onMousePressed(mouseButton -> {
                setTargeting(mode);
                return true;
            });
    }

    private String targetingLabel() {
        if (targeting == TARGET_CHOP) {
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

    private String dropOffLabel() {
        if (!colony.hasDropOff()) {
            return "drop-off   not set";
        }
        return "drop-off   " + colony.getDropOffX() + "/" + colony.getDropOffY() + "/" + colony.getDropOffZ();
    }

    private String pickUpLabel() {
        if (!colony.hasPickUp()) {
            return "pick-up   not set";
        }
        return "pick-up   " + colony.getPickUpX() + "/" + colony.getPickUpY() + "/" + colony.getPickUpZ();
    }

    private TextFieldWidget groupField() {
        groupField = new TextFieldWidget();
        groupField.setMaxLength(PacketCitizenGroup.MAX_GROUP_LENGTH);
        return groupField.background(skin(ROW_BACKGROUND, BUTTON_BORDER))
            .width(INNER_WIDTH - ASSIGN_WIDTH - 3)
            .height(BUTTON_HEIGHT);
    }

    private ButtonWidget<?> button(String label, int width, BooleanSupplier enabled, Runnable action) {
        return new ButtonWidget<>().size(width, BUTTON_HEIGHT)
            .background(skin(BUTTON_BACKGROUND, BUTTON_BORDER))
            .hoverBackground(skin(BUTTON_HOVER, BUTTON_BORDER))
            .child(
                IKey.str(label)
                    .asWidget()
                    .color(() -> enabled.getAsBoolean() ? TEXT_COLOR : DISABLED_COLOR)
                    .shadow(true)
                    .posRel(Alignment.Center))
            .onMousePressed(mouseButton -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> buildGroupRow(int index) {
        ButtonWidget<?> row = new ButtonWidget<>().size(INNER_WIDTH, ROW_HEIGHT)
            .background(skin(ROW_BACKGROUND, ROW_BACKGROUND))
            .hoverBackground(skin(BUTTON_HOVER, BUTTON_BORDER))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .heightRel(1.0F)
                    .padding(5, 0)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        IKey.dynamic(() -> groupRowLabel(index))
                            .asWidget()
                            .color(() -> groupRowColor(index))
                            .shadow(true))
                    .child(
                        IKey.dynamic(() -> groupRowCount(index))
                            .asWidget()
                            .color(TEXT_COLOR)
                            .shadow(true)))
            .onMousePressed(mouseButton -> {
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

    private String groupAt(int index) {
        return index < groups.size() ? groups.get(index) : null;
    }

    private String groupRowLabel(int index) {
        String group = groupAt(index);
        return group == null ? "" : group;
    }

    private String groupRowCount(int index) {
        String group = groupAt(index);
        if (group == null) {
            return "";
        }
        int count = 0;
        int selected = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (!groupLabel(citizen).equals(group)) {
                continue;
            }
            count++;
            if (selection.contains(citizen.getId())) {
                selected++;
            }
        }
        return selected + "/" + count;
    }

    private int groupRowColor(int index) {
        String group = groupAt(index);
        if (group == null) {
            return TEXT_COLOR;
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
                .size()
            + "   "
            + getSelectedLoaded()
            + " loaded";
    }

    private String helpHint() {
        return (helpOpen ? "H hide help   " : "H help   ") + ControllingCompat.describe(GCKeyBindings.openColony)
            + " close";
    }

    private String keyHint() {
        return "R recenter   ctrl+A all   G guard   C cancel   I inventory";
    }
}
