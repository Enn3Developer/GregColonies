package com.enn3developer.gregcolonies.client.gui;

import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.network.PacketCitizenCommand;

public enum TargetMode {

    NONE("", null, 0x7CE07C, Pick.NONE, "", PacketCitizenCommand.CHOP),
    CHOP("Chop", null, 0x7CE07C, Pick.REGION, "drag a region, RMB cancels", PacketCitizenCommand.CHOP),
    MINE("Mine", null, 0xFFB040, Pick.CHUNK, "click a chunk, RMB cancels", PacketCitizenCommand.MINE),
    FARM("Farm", null, 0xD8E060, Pick.REGION, "drag a region, RMB cancels", PacketCitizenCommand.FARM),
    DROP_OFF("Drop-off", ColonySiteKind.DROP_OFF, 0xFF7CE0, Pick.SPOT, "click a chest, again to clear",
        PacketCitizenCommand.CHOP),
    PICK_UP("Pick-up", ColonySiteKind.PICK_UP, 0x7CE0FF, Pick.SPOT, "click a chest, again to clear",
        PacketCitizenCommand.CHOP),
    MATERIALS("Materials", ColonySiteKind.MATERIALS, 0xFFC46B, Pick.SPOT, "click a chest, again to clear",
        PacketCitizenCommand.CHOP),
    BUILD("Build", null, 0x9CE06B, Pick.SPOT, "click the ground, again to clear", PacketCitizenCommand.CHOP),
    HOME("Home", null, 0xB08CFF, Pick.VOLUME, "drag a room corner to corner, click a home to clear",
        PacketCitizenCommand.CHOP);

    public enum Pick {
        NONE,
        REGION,
        VOLUME,
        CHUNK,
        SPOT
    }

    public static final int LABEL_ALPHA = 0xFF;

    public static final int AREA_ALPHA = 0xB0;

    public static final int MARK_ALPHA = 0x70;

    private final String label;

    private final ColonySiteKind site;

    private final int rgb;

    private final Pick pick;

    private final String hint;

    private final byte command;

    TargetMode(String label, ColonySiteKind site, int rgb, Pick pick, String hint, byte command) {
        this.label = label;
        this.site = site;
        this.rgb = rgb;
        this.pick = pick;
        this.hint = hint;
        this.command = command;
    }

    public String getLabel() {
        return label;
    }

    public ColonySiteKind getSite() {
        return site;
    }

    public Pick getPick() {
        return pick;
    }

    public String getHint() {
        return hint;
    }

    public byte getCommand() {
        return command;
    }

    public int color(int alpha) {
        return alpha << 24 | rgb;
    }

    public boolean isSpot() {
        return pick == Pick.SPOT;
    }

    public static TargetMode of(ColonySiteKind kind) {
        for (TargetMode mode : values()) {
            if (mode.site == kind) {
                return mode;
            }
        }
        return NONE;
    }
}
