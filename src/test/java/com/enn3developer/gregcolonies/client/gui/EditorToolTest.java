package com.enn3developer.gregcolonies.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.colony.ColonySiteKind;

class EditorToolTest {

    @Test
    void everyToolHasALabelAndAStateName() {
        Set<String> labels = new HashSet<>();
        Set<String> states = new HashSet<>();
        for (EditorTool tool : EditorTool.values()) {
            assertFalse(
                tool.getLabel()
                    .isEmpty());
            assertFalse(
                tool.getState()
                    .isEmpty());
            labels.add(tool.getLabel());
            states.add(tool.getState());
        }
        assertEquals(EditorTool.values().length, labels.size());
        assertEquals(EditorTool.values().length, states.size());
    }

    @Test
    void onlyEraseAndPickRemoveBlocks() {
        assertTrue(EditorTool.ERASE.erases());
        assertTrue(EditorTool.PICK.erases());
        assertFalse(EditorTool.PAINT.erases());
        assertFalse(EditorTool.BOX.erases());
        assertFalse(EditorTool.ANCHOR.erases());
    }

    @Test
    void onlyPaintAndEraseDragOverCells() {
        assertTrue(EditorTool.PAINT.draws());
        assertTrue(EditorTool.ERASE.draws());
        assertFalse(EditorTool.BOX.draws());
        assertFalse(EditorTool.PICK.draws());
        assertFalse(EditorTool.ANCHOR.draws());
    }

    @Test
    void cyclingVisitsEveryToolAndComesBack() {
        EditorTool tool = EditorTool.PAINT;
        Set<EditorTool> seen = new HashSet<>();
        for (int i = 0; i < EditorTool.values().length; i++) {
            assertTrue(seen.add(tool), "the cycle repeated early at " + tool);
            tool = tool.next();
        }
        assertEquals(EditorTool.PAINT, tool);
        assertEquals(EditorTool.values().length, seen.size());
    }

    @Test
    void cyclingFollowsTheDeclaredOrder() {
        EditorTool[] tools = EditorTool.values();
        for (int i = 0; i < tools.length; i++) {
            assertEquals(tools[(i + 1) % tools.length], tools[i].next(), tools[i] + " cycles to the wrong tool");
        }
    }

    @Test
    void targetModesCarryTheirSiteKind() {
        assertEquals(ColonySiteKind.DROP_OFF, TargetMode.DROP_OFF.getSite());
        assertEquals(ColonySiteKind.PICK_UP, TargetMode.PICK_UP.getSite());
        assertEquals(ColonySiteKind.MATERIALS, TargetMode.MATERIALS.getSite());
        assertNull(TargetMode.CHOP.getSite());
    }

    @Test
    void everySiteKindHasATargetMode() {
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            assertEquals(
                kind,
                TargetMode.of(kind)
                    .getSite());
        }
        assertEquals(TargetMode.NONE, TargetMode.of(null));
    }

    @Test
    void spotModesAreTheOnesThatClickASingleBlock() {
        assertTrue(TargetMode.DROP_OFF.isSpot());
        assertTrue(TargetMode.BUILD.isSpot());
        assertFalse(TargetMode.CHOP.isSpot());
        assertFalse(TargetMode.MINE.isSpot());
        assertFalse(TargetMode.NONE.isSpot());
    }

    @Test
    void pickKindsMatchTheHints() {
        assertEquals(TargetMode.Pick.REGION, TargetMode.CHOP.getPick());
        assertEquals(TargetMode.Pick.REGION, TargetMode.FARM.getPick());
        assertEquals(TargetMode.Pick.CHUNK, TargetMode.MINE.getPick());
        assertEquals(TargetMode.Pick.NONE, TargetMode.NONE.getPick());
    }

    @Test
    void colourPacksTheAlphaIntoTheTopByte() {
        int colour = TargetMode.CHOP.color(TargetMode.LABEL_ALPHA);
        assertEquals(0xFF, colour >>> 24);
        assertEquals(0x7CE07C, colour & 0xFFFFFF);

        assertEquals(TargetMode.AREA_ALPHA, TargetMode.CHOP.color(TargetMode.AREA_ALPHA) >>> 24);
        assertEquals(0, TargetMode.CHOP.color(0) >>> 24);
    }

    @Test
    void everyActiveModeExplainsItself() {
        for (TargetMode mode : TargetMode.values()) {
            if (mode == TargetMode.NONE) {
                continue;
            }
            assertFalse(
                mode.getLabel()
                    .isEmpty(),
                mode + " needs a label");
            assertFalse(
                mode.getHint()
                    .isEmpty(),
                mode + " needs a hint");
        }
    }
}
