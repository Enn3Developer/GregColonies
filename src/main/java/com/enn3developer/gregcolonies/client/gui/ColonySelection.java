package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.enn3developer.gregcolonies.network.CitizenSnapshot;
import com.enn3developer.gregcolonies.network.ColonySnapshot;

public class ColonySelection {

    public static final String UNGROUPED = "ungrouped";

    private static final int BOUNDS = 6;

    private final Set<UUID> selected = new LinkedHashSet<>();

    private final List<String> groups = new ArrayList<>();

    private final int[] pending = new int[BOUNDS];

    private ColonySnapshot colony;

    private TargetMode targeting = TargetMode.NONE;

    private boolean hasPending;

    public ColonySnapshot getColony() {
        return colony;
    }

    public void setColony(ColonySnapshot colony) {
        this.colony = colony;
        Set<UUID> known = new LinkedHashSet<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            known.add(citizen.getId());
            counts.merge(groupLabel(citizen), 1, Integer::sum);
        }
        selected.retainAll(known);
        groups.clear();
        groups.addAll(counts.keySet());
    }

    public static String groupLabel(CitizenSnapshot citizen) {
        return citizen.getGroup()
            .isEmpty() ? UNGROUPED : citizen.getGroup();
    }

    public List<String> getGroups() {
        return groups;
    }

    public String groupAt(int index) {
        return index >= 0 && index < groups.size() ? groups.get(index) : null;
    }

    public Set<UUID> get() {
        return selected;
    }

    public boolean isEmpty() {
        return selected.isEmpty();
    }

    public int size() {
        return selected.size();
    }

    public boolean isSelected(UUID id) {
        return selected.contains(id);
    }

    public void clear() {
        selected.clear();
    }

    public void toggle(UUID id) {
        if (!selected.remove(id)) {
            selected.add(id);
        }
    }

    public void selectAll() {
        selected.clear();
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            selected.add(citizen.getId());
        }
    }

    public void selectGroup(String group, boolean add) {
        if (!add) {
            selected.clear();
        }
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (groupLabel(citizen).equals(group)) {
                selected.add(citizen.getId());
            }
        }
    }

    public int countLoaded() {
        int loaded = 0;
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.isLoaded() && selected.contains(citizen.getId())) {
                loaded++;
            }
        }
        return loaded;
    }

    public CitizenSnapshot single() {
        if (selected.size() != 1) {
            return null;
        }
        for (CitizenSnapshot citizen : colony.getCitizens()) {
            if (selected.contains(citizen.getId())) {
                return citizen;
            }
        }
        return null;
    }

    public TargetMode getTargeting() {
        return targeting;
    }

    public void setTargeting(TargetMode mode) {
        targeting = targeting == mode ? TargetMode.NONE : mode;
        hasPending = false;
    }

    public boolean hasPending() {
        return targeting != TargetMode.NONE && hasPending;
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
}
