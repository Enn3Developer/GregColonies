package com.enn3developer.gregcolonies.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.network.GCNetwork;
import com.enn3developer.gregcolonies.network.PacketBlueprintAction;
import com.enn3developer.gregcolonies.network.PacketBlueprintSave;
import com.enn3developer.gregcolonies.network.PacketColonyPalette;

public class BlueprintEditor {

    public static final int TOOL_PAINT = 0;

    public static final int TOOL_ERASE = 1;

    public static final int TOOL_BOX = 2;

    public static final int TOOL_PICK = 3;

    public static final int TOOL_ANCHOR = 4;

    public static final int TOOL_COUNT = 5;

    public static final int DEFAULT_X = 16;

    public static final int DEFAULT_Y = 8;

    public static final int DEFAULT_Z = 16;

    private static final int UNDO_DEPTH = 24;

    private static final double EPSILON = 1.0E-6D;

    private final int colonyId;

    private final ColonySnapshot colony;

    private int index;

    private Blueprint model;

    private int anchorX;

    private int anchorY;

    private int anchorZ;

    private final List<BlueprintBrush> palette = new ArrayList<>();

    private int brush;

    private int tool = TOOL_PAINT;

    private int layer;

    private boolean sliced;

    private final Deque<Blueprint> undo = new ArrayDeque<>();

    private final Deque<int[]> undoAnchor = new ArrayDeque<>();

    private final Deque<Blueprint> redo = new ArrayDeque<>();

    private final Deque<int[]> redoAnchor = new ArrayDeque<>();

    private int[] boxAnchor;

    private boolean dirty;

    private int revision = 1;

    private int[] layerRevision;

    public BlueprintEditor(ColonySnapshot colony, int index, Blueprint source) {
        this.colony = colony;
        this.colonyId = colony.getId();
        this.index = index;
        this.model = source == null ? Blueprint.empty("", DEFAULT_X, DEFAULT_Y, DEFAULT_Z) : source.copy();
        int baseX = colony.hasBuildSite() ? colony.getBuildX() : colony.getX();
        int baseY = colony.hasBuildSite() ? colony.getBuildY() : colony.getY();
        int baseZ = colony.hasBuildSite() ? colony.getBuildZ() : colony.getZ();
        this.anchorX = baseX - model.getOriginX();
        this.anchorY = baseY - model.getOriginY();
        this.anchorZ = baseZ - model.getOriginZ();
    }

    public ColonySnapshot getColony() {
        return colony;
    }

    public int getColonyId() {
        return colonyId;
    }

    public int getIndex() {
        return index;
    }

    public Blueprint getModel() {
        return model;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public int getAnchorZ() {
        return anchorZ;
    }

    public double centerX() {
        return anchorX + model.getSizeX() / 2.0D;
    }

    public double centerY() {
        return anchorY + model.getSizeY() / 2.0D;
    }

    public double centerZ() {
        return anchorZ + model.getSizeZ() / 2.0D;
    }

    public List<BlueprintBrush> getPalette() {
        return palette;
    }

    public BlueprintBrush getBrush() {
        return brush >= 0 && brush < palette.size() ? palette.get(brush) : null;
    }

    public int getBrushIndex() {
        return brush;
    }

    public void setBrush(int index) {
        if (index >= 0 && index < palette.size()) {
            brush = index;
        }
    }

    public int getTool() {
        return tool;
    }

    public void setTool(int tool) {
        this.tool = tool;
        boxAnchor = null;
    }

    public int getLayer() {
        return layer;
    }

    public void stepLayer(int by) {
        layer = clamp(layer + by, 0, model.getSizeY() - 1);
    }

    public boolean isSliced() {
        return sliced;
    }

    public void toggleSlice() {
        sliced = !sliced;
    }

    public int ceiling() {
        return sliced ? Math.min(layer, model.getSizeY() - 1) : model.getSizeY() - 1;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getRevision() {
        return revision;
    }

    public int layerRevision(int y) {
        return layerRevision != null && y >= 0 && y < layerRevision.length ? layerRevision[y] : revision;
    }

    private void touch(int y) {
        revision++;
        if (layerRevision == null || layerRevision.length != model.getSizeY()) {
            touchAll();
            return;
        }
        for (int at = Math.max(0, y - 1); at <= Math.min(layerRevision.length - 1, y + 1); at++) {
            layerRevision[at] = revision;
        }
    }

    private void touchAll() {
        revision++;
        layerRevision = new int[model.getSizeY()];
        for (int at = 0; at < layerRevision.length; at++) {
            layerRevision[at] = revision;
        }
    }

    public boolean hasUndo() {
        return !undo.isEmpty();
    }

    public boolean hasRedo() {
        return !redo.isEmpty();
    }

    public int[] getBoxAnchor() {
        return boxAnchor;
    }

    public void acceptPalette(PacketColonyPalette message) {
        if (message.getColonyId() != colonyId) {
            return;
        }
        BlueprintBrush current = getBrush();
        palette.clear();
        for (PacketColonyPalette.Entry entry : message.getEntries()) {
            BlueprintBrush made = BlueprintBrush.of(entry.getBlock(), entry.getMeta(), entry.getHeld());
            if (made != null) {
                palette.add(made);
            }
        }
        brush = 0;
        if (current == null) {
            return;
        }
        for (int i = 0; i < palette.size(); i++) {
            if (palette.get(i)
                .is(current.getBlock(), current.getMeta())) {
                brush = i;
                return;
            }
        }
        BlueprintBrush kept = BlueprintBrush.of(current.getBlock(), current.getMeta(), 0);
        if (kept != null) {
            palette.add(kept);
            brush = palette.size() - 1;
        }
    }

    public void requestPalette() {
        GCNetwork.CHANNEL.sendToServer(new PacketBlueprintAction(colonyId, PacketBlueprintAction.PALETTE, index));
    }

    public void save() {
        for (PacketBlueprintSave packet : PacketBlueprintSave.split(colonyId, index, model)) {
            GCNetwork.CHANNEL.sendToServer(packet);
        }
        dirty = false;
    }

    public boolean resize(int axis, int by) {
        int sizeX = model.getSizeX() + (axis == 0 ? by : 0);
        int sizeY = model.getSizeY() + (axis == 1 ? by : 0);
        int sizeZ = model.getSizeZ() + (axis == 2 ? by : 0);
        Blueprint resized = model.resized(sizeX, sizeY, sizeZ, 0, 0, 0);
        if (resized == null) {
            return false;
        }
        pushUndo();
        model = resized;
        layer = clamp(layer, 0, model.getSizeY() - 1);
        touchAll();
        return true;
    }

    public void turn() {
        pushUndo();
        pivot(model.transformed(1, false));
        layer = clamp(layer, 0, model.getSizeY() - 1);
        touchAll();
    }

    public void flip() {
        pushUndo();
        pivot(model.transformed(0, true));
        touchAll();
    }

    private void pivot(Blueprint turned) {
        int worldX = anchorX + model.getOriginX();
        int worldY = anchorY + model.getOriginY();
        int worldZ = anchorZ + model.getOriginZ();
        model = turned;
        anchorX = worldX - model.getOriginX();
        anchorY = worldY - model.getOriginY();
        anchorZ = worldZ - model.getOriginZ();
    }

    public void clearLayer() {
        pushUndo();
        for (int z = 0; z < model.getSizeZ(); z++) {
            for (int x = 0; x < model.getSizeX(); x++) {
                model.setCell(x, layer, z, Blueprint.AIR);
            }
        }
        touch(layer);
    }

    public void fillLayer() {
        int cell = brushCell();
        if (cell == Blueprint.AIR) {
            return;
        }
        pushUndo();
        for (int z = 0; z < model.getSizeZ(); z++) {
            for (int x = 0; x < model.getSizeX(); x++) {
                model.setCell(x, layer, z, cell);
            }
        }
        touch(layer);
    }

    public void wipe() {
        pushUndo();
        model = Blueprint.empty(model.getName(), model.getSizeX(), model.getSizeY(), model.getSizeZ());
        touchAll();
    }

    public void pushUndo() {
        undo.addLast(model.copy());
        undoAnchor.addLast(anchor());
        while (undo.size() > UNDO_DEPTH) {
            undo.removeFirst();
            undoAnchor.removeFirst();
        }
        redo.clear();
        redoAnchor.clear();
        dirty = true;
    }

    public void undo() {
        if (undo.isEmpty()) {
            return;
        }
        redo.addLast(model.copy());
        redoAnchor.addLast(anchor());
        model = undo.removeLast();
        restore(undoAnchor.removeLast());
    }

    public void redo() {
        if (redo.isEmpty()) {
            return;
        }
        undo.addLast(model.copy());
        undoAnchor.addLast(anchor());
        model = redo.removeLast();
        restore(redoAnchor.removeLast());
    }

    private int[] anchor() {
        return new int[] { anchorX, anchorY, anchorZ };
    }

    private void restore(int[] anchor) {
        anchorX = anchor[0];
        anchorY = anchor[1];
        anchorZ = anchor[2];
        layer = clamp(layer, 0, model.getSizeY() - 1);
        dirty = true;
        touchAll();
    }

    public int brushCell() {
        BlueprintBrush selected = getBrush();
        return selected == null ? Blueprint.AIR : model.cellFor(selected.getBlock(), selected.getMeta());
    }

    public Map<Integer, Integer> shortfall() {
        Map<Integer, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : model.materials()
            .entrySet()) {
            int cell = entry.getKey();
            Block block = model.blockOf(cell);
            int meta = Blueprint.metaOf(cell);
            int held = 0;
            for (BlueprintBrush entry2 : palette) {
                if (entry2.is(block, meta)) {
                    held = entry2.getHeld();
                    break;
                }
            }
            if (held < entry.getValue()) {
                missing.put(cell, entry.getValue() - held);
            }
        }
        return missing;
    }

    public void apply(BlueprintTrace.Hit hit, boolean erasing) {
        if (hit == null) {
            return;
        }
        if (tool == TOOL_PICK) {
            take(hit);
            return;
        }
        if (tool == TOOL_ANCHOR) {
            int[] spot = hit.solid ? hit.hit() : hit.place();
            pushUndo();
            model.setOrigin(spot[0], spot[1], spot[2]);
            return;
        }
        if (tool == TOOL_BOX) {
            box(hit, erasing);
            return;
        }
        if (erasing || tool == TOOL_ERASE) {
            if (hit.solid) {
                model.setCell(hit.hitX, hit.hitY, hit.hitZ, Blueprint.AIR);
                dirty = true;
                touch(hit.hitY);
            }
            return;
        }
        int cell = brushCell();
        if (cell != Blueprint.AIR && model.contains(hit.placeX, hit.placeY, hit.placeZ)) {
            model.setCell(hit.placeX, hit.placeY, hit.placeZ, cell);
            dirty = true;
            touch(hit.placeY);
        }
    }

    private void take(BlueprintTrace.Hit hit) {
        if (!hit.solid) {
            return;
        }
        int cell = model.cellAt(hit.hitX, hit.hitY, hit.hitZ);
        Block block = model.blockOf(cell);
        if (block == null) {
            return;
        }
        int meta = Blueprint.metaOf(cell);
        for (int i = 0; i < palette.size(); i++) {
            if (palette.get(i)
                .is(block, meta)) {
                brush = i;
                return;
            }
        }
        BlueprintBrush made = BlueprintBrush.of(block, meta, 0);
        if (made != null) {
            palette.add(made);
            brush = palette.size() - 1;
        }
    }

    private void box(BlueprintTrace.Hit hit, boolean erasing) {
        int[] target = erasing || tool == TOOL_ERASE ? hit.hit() : hit.place();
        if (target == null) {
            return;
        }
        if (boxAnchor == null) {
            boxAnchor = target;
            return;
        }
        int cell = erasing ? Blueprint.AIR : brushCell();
        if (cell == Blueprint.AIR && !erasing) {
            boxAnchor = null;
            return;
        }
        pushUndo();
        for (int y = Math.min(boxAnchor[1], target[1]); y <= Math.max(boxAnchor[1], target[1]); y++) {
            for (int z = Math.min(boxAnchor[2], target[2]); z <= Math.max(boxAnchor[2], target[2]); z++) {
                for (int x = Math.min(boxAnchor[0], target[0]); x <= Math.max(boxAnchor[0], target[0]); x++) {
                    model.setCell(x, y, z, cell);
                }
            }
        }
        touchAll();
        boxAnchor = null;
    }

    public BlueprintTrace.Hit trace(double originX, double originY, double originZ, double dirX, double dirY,
        double dirZ) {
        return BlueprintTrace
            .trace(model, ceiling(), layer, originX - anchorX, originY - anchorY, originZ - anchorZ, dirX, dirY, dirZ);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

}
