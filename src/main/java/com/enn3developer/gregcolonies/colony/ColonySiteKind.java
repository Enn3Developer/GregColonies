package com.enn3developer.gregcolonies.colony;

public enum ColonySiteKind {

    DROP_OFF("dropOff", "drop-off", "drop-off"),
    PICK_UP("pickUp", "pick-up", "pick-up"),
    MATERIALS("materials", "materials pick-up", "materials");

    private static final ColonySiteKind[] VALUES = values();

    private final String key;

    private final String label;

    private final String shortLabel;

    private final String flagKey;

    ColonySiteKind(String key, String label, String shortLabel) {
        this.key = key;
        this.label = label;
        this.shortLabel = shortLabel;
        this.flagKey = "has" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public String getAxisKey(char axis) {
        return key + axis;
    }

    public static ColonySiteKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }
}
