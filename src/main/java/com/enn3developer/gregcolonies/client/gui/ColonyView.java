package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
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

public class ColonyView {

    public static final String UNGROUPED = "ungrouped";

    public static final int TARGET_NONE = 0;

    public static final int TARGET_CHOP = 1;

    public static final int TARGET_MINE = 2;

    public static final int TARGET_DROP_OFF = 3;

    public static final int TARGET_PICK_UP = 4;

    private static final int MAX_GROUP_ROWS = 10;

    private static final int SIDE_WIDTH = 148;

    private static final int ROW_HEIGHT = 13;

    private static final int BUTTON_HEIGHT = 15;

    private static final int TITLE_COLOR = 0xFFFFD060;

    private static final int TEXT_COLOR = 0xFFB4BCC8;

    private static final int HINT_COLOR = 0xFF7C8494;

    private static final int PANEL_BACKGROUND = 0xB0080A0F;

    private static final int BUTTON_BACKGROUND = 0xFF232833;

    private static final int ACTIVE_COLOR = 0xFF7CE07C;

    private static final String SELECT_HINT = "LMB select   LMB drag box   shift add   RMB move";

    private static final String CAMERA_HINT = "RMB drag pan   MMB drag turn   scroll zoom   WASD pan   Q/E turn";

    private final ColonyViewWidget map = new ColonyViewWidget(this);

    private final Set<UUID> selection = new LinkedHashSet<>();

    private final List<String> groups = new ArrayList<>();

    private ColonySnapshot colony;

    private TextFieldWidget groupField;

    private final int[] pending = new int[6];

    private int targeting = TARGET_NONE;

    private boolean hasPending;

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

    public void sendGroup(String group) {
        if (selection.isEmpty()) {
            return;
        }
        GCNetwork.CHANNEL.sendToServer(new PacketCitizenGroup(colony.getId(), group, selection));
    }

    public ModularPanel buildPanel() {
        ModularPanel panel = new ModularPanel("colony_view").fullScreenInvisible();
        panel.child(map.full());
        panel.child(
            Flow.column()
                .coverChildren()
                .childPadding(2)
                .pos(8, 8)
                .child(
                    IKey.dynamic(this::title)
                        .asWidget()
                        .color(TITLE_COLOR)
                        .shadow(true))
                .child(
                    IKey.dynamic(this::status)
                        .asWidget()
                        .color(TEXT_COLOR)
                        .shadow(true))
                .child(
                    IKey.dynamic(this::census)
                        .asWidget()
                        .color(TEXT_COLOR)
                        .shadow(true)));
        panel.child(buildSidePanel());
        panel.child(
            Flow.column()
                .coverChildren()
                .childPadding(2)
                .left(8)
                .bottom(6)
                .child(
                    IKey.str(SELECT_HINT)
                        .asWidget()
                        .color(HINT_COLOR)
                        .shadow(true))
                .child(
                    IKey.str(CAMERA_HINT)
                        .asWidget()
                        .color(HINT_COLOR)
                        .shadow(true))
                .child(
                    IKey.dynamic(this::keyHint)
                        .asWidget()
                        .color(HINT_COLOR)
                        .shadow(true)));
        return panel;
    }

    private Flow buildSidePanel() {
        return Flow.column()
            .width(SIDE_WIDTH)
            .coverChildrenHeight()
            .childPadding(3)
            .padding(6)
            .right(8)
            .top(8)
            .background(new Rectangle().color(PANEL_BACKGROUND))
            .child(
                IKey.str("Groups")
                    .asWidget()
                    .color(TITLE_COLOR)
                    .shadow(true))
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
                    .child(button("Assign", 44, () -> sendGroup(groupField.getText()))))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(button("Ungroup", 66, () -> sendGroup("")))
                    .child(button("Select all", 66, this::selectAll)))
            .child(
                IKey.dynamic(this::selectionLabel)
                    .asWidget()
                    .color(TEXT_COLOR)
                    .shadow(true))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(button("Guard", 66, () -> sendCommand(PacketCitizenCommand.GUARD, false, 0, 0, 0)))
                    .child(button("Cancel", 66, () -> sendCommand(PacketCitizenCommand.CANCEL, false, 0, 0, 0))))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(modeButton("Chop", 66, TARGET_CHOP))
                    .child(modeButton("Mine", 66, TARGET_MINE)))
            .child(
                Flow.row()
                    .widthRel(1.0F)
                    .height(BUTTON_HEIGHT)
                    .childPadding(3)
                    .child(modeButton("Drop-off", 66, TARGET_DROP_OFF))
                    .child(modeButton("Pick-up", 66, TARGET_PICK_UP)))
            .child(
                IKey.dynamic(this::dropOffLabel)
                    .asWidget()
                    .color(TEXT_COLOR)
                    .shadow(true))
            .child(
                IKey.dynamic(this::pickUpLabel)
                    .asWidget()
                    .color(TEXT_COLOR)
                    .shadow(true))
            .child(
                IKey.dynamic(this::targetingLabel)
                    .asWidget()
                    .color(HINT_COLOR)
                    .shadow(true));
    }

    private ButtonWidget<?> modeButton(String label, int width, int mode) {
        return new ButtonWidget<>().size(width, BUTTON_HEIGHT)
            .background(new Rectangle().color(BUTTON_BACKGROUND))
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
            return "click a chest, click it again to clear";
        }
        return "";
    }

    private String dropOffLabel() {
        if (!colony.hasDropOff()) {
            return "drop-off none";
        }
        return "drop-off " + colony.getDropOffX() + "/" + colony.getDropOffY() + "/" + colony.getDropOffZ();
    }

    private String pickUpLabel() {
        if (!colony.hasPickUp()) {
            return "pick-up none";
        }
        return "pick-up " + colony.getPickUpX() + "/" + colony.getPickUpY() + "/" + colony.getPickUpZ();
    }

    private TextFieldWidget groupField() {
        groupField = new TextFieldWidget();
        groupField.setMaxLength(PacketCitizenGroup.MAX_GROUP_LENGTH);
        return groupField.width(80)
            .height(BUTTON_HEIGHT);
    }

    private ButtonWidget<?> button(String label, int width, Runnable action) {
        return new ButtonWidget<>().size(width, BUTTON_HEIGHT)
            .background(new Rectangle().color(BUTTON_BACKGROUND))
            .child(
                IKey.str(label)
                    .asWidget()
                    .color(TEXT_COLOR)
                    .shadow(true)
                    .posRel(Alignment.Center))
            .onMousePressed(mouseButton -> {
                action.run();
                return true;
            });
    }

    private ButtonWidget<?> buildGroupRow(int index) {
        ButtonWidget<?> row = new ButtonWidget<>().size(SIDE_WIDTH - 12, ROW_HEIGHT)
            .background(new Rectangle().color(BUTTON_BACKGROUND))
            .child(
                IKey.dynamic(() -> groupRowLabel(index))
                    .asWidget()
                    .color(() -> groupRowColor(index))
                    .shadow(true)
                    .posRel(0.0F, 0.5F)
                    .marginLeft(4))
            .onMousePressed(mouseButton -> {
                String group = groupAt(index);
                if (group != null) {
                    selectGroup(group, com.cleanroommc.modularui.api.widget.Interactable.hasShiftDown());
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
        return group + "   " + selected + "/" + count;
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
            + "   center "
            + colony.getX()
            + "/"
            + colony.getY()
            + "/"
            + colony.getZ()
            + "   radius "
            + colony.getRadius();
    }

    private String census() {
        int loaded = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.isLoaded()) {
                loaded++;
            }
        }
        return "citizens " + colony.getCitizens()
            .size() + " (" + loaded + " loaded)   orders " + colony.getOrderCount();
    }

    private String selectionLabel() {
        return "selected " + selection.size() + " (" + getSelectedLoaded() + " loaded)";
    }

    private String keyHint() {
        return "R recenter   ctrl+A select all   G guard   C cancel   "
            + ControllingCompat.describe(GCKeyBindings.openColony)
            + " close";
    }
}
