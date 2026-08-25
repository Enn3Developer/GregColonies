package com.enn3developer.gregcolonies.client.gui;

import java.util.Map;
import java.util.TreeMap;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.enn3developer.gregcolonies.client.ControllingCompat;
import com.enn3developer.gregcolonies.client.GCKeyBindings;
import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;

public class ColonyView {

    private static final String UNGROUPED = "ungrouped";

    private static final int TITLE_COLOR = 0xFFFFD060;

    private static final int TEXT_COLOR = 0xFFB4BCC8;

    private static final int HINT_COLOR = 0xFF7C8494;

    private final ColonyViewWidget map = new ColonyViewWidget(this);

    private ColonySnapshot colony;

    public ColonyView(ColonySnapshot colony) {
        this.colony = colony;
    }

    public ColonySnapshot getColony() {
        return colony;
    }

    public void setColony(ColonySnapshot colony) {
        this.colony = colony;
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
                    IKey.dynamic(this::groups)
                        .asWidget()
                        .color(TEXT_COLOR)
                        .shadow(true)));
        panel.child(
            IKey.dynamic(this::controls)
                .asWidget()
                .color(HINT_COLOR)
                .shadow(true)
                .left(8)
                .bottom(6));
        return panel;
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

    private String groups() {
        Map<String, Integer> counts = new TreeMap<>();
        int loaded = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            String group = citizen.getGroup()
                .isEmpty() ? UNGROUPED : citizen.getGroup();
            counts.merge(group, 1, Integer::sum);
            if (citizen.isLoaded()) {
                loaded++;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("citizens ")
            .append(
                colony.getCitizens()
                    .size())
            .append(" (")
            .append(loaded)
            .append(" loaded)   orders ")
            .append(colony.getOrderCount());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            builder.append("   ")
                .append(entry.getKey())
                .append(' ')
                .append(entry.getValue());
        }
        return builder.toString();
    }

    private String controls() {
        return "drag pan   right drag rotate   scroll zoom   Q/E turn   R recenter   "
            + ControllingCompat.describe(GCKeyBindings.openColony)
            + " close";
    }
}
