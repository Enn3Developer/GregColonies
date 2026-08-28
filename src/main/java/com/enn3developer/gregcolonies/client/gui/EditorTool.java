package com.enn3developer.gregcolonies.client.gui;

public enum EditorTool {

    PAINT("Paint", "paint"),
    ERASE("Erase", "erase"),
    BOX("Box", "box"),
    PICK("Pick", "pick"),
    ANCHOR("Anchor", "anchor");

    private final String label;

    private final String state;

    EditorTool(String label, String state) {
        this.label = label;
        this.state = state;
    }

    public String getLabel() {
        return label;
    }

    public String getState() {
        return state;
    }

    public boolean erases() {
        return this == ERASE || this == PICK;
    }

    public boolean draws() {
        return this == PAINT || this == ERASE;
    }

    public EditorTool next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
