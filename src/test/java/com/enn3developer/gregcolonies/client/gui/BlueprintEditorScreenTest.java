package com.enn3developer.gregcolonies.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.gui.ClientBootstrap;
import com.enn3developer.gregcolonies.testing.gui.GuiHarness;
import com.enn3developer.gregcolonies.testing.gui.Rendered;

import gregtech.common.GTMockWorld;

class BlueprintEditorScreenTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final class RecordingGateway implements EditorGateway {

        private int paletteRequests;

        private int savedIndex = -1;

        private Blueprint saved;

        @Override
        public void requestPalette(int colonyId, int index) {
            paletteRequests++;
        }

        @Override
        public void save(int colonyId, int index, Blueprint model) {
            savedIndex = index;
            saved = model;
        }
    }

    @BeforeAll
    static void boot() {
        ClientBootstrap.ensure();
    }

    private static ColonySnapshot snapshot() {
        return ColonySnapshot.of(new Colony(1, "Home", OWNER, "Enn3", 0, 0, 64, 0), new GTMockWorld());
    }

    private static final int WIDTH = 640;

    private static final int HEIGHT = 400;

    private static BlueprintEditor editor(RecordingGateway gateway) {
        return new BlueprintEditor(snapshot(), 0, Fixtures.cube("tower", 3), gateway);
    }

    private static GuiHarness open(BlueprintEditor editor) {
        return GuiHarness.open(new BlueprintEditorScreen(new BlueprintEditorView(editor)), WIDTH, HEIGHT);
    }

    @Test
    void theEditorAsksForItsPaletteWhenItOpens() {
        RecordingGateway gateway = new RecordingGateway();
        open(editor(gateway));

        assertTrue(gateway.paletteRequests > 0, "the editor never asked the server for the palette");
    }

    @Test
    void clickingAToolButtonSwitchesTheTool() {
        BlueprintEditor editor = editor(new RecordingGateway());
        GuiHarness harness = open(editor);

        assertEquals(EditorTool.PAINT, editor.getTool());
        assertTrue(
            harness.click(
                harness.render()
                    .button("Box")),
            "the tool button did not take the click");

        assertEquals(EditorTool.BOX, editor.getTool());
    }

    @Test
    void theCanvasButtonsResizeTheBlueprint() {
        BlueprintEditor editor = editor(new RecordingGateway());
        GuiHarness harness = open(editor);

        Rendered.Node label = harness.render()
            .byText("X 3");
        Rendered.Node grow = harness.render()
            .at(label.x() + label.width() + 4, label.centerY());
        assertNotNull(grow, "no button to the right of the X row");

        harness.click(grow);

        assertEquals(
            4,
            editor.getModel()
                .getSizeX());
        assertTrue(
            harness.render()
                .shows("X 4"),
            "the readout did not follow the model");
    }

    @Test
    void escapeOnUnsavedWorkAsksBeforeLeaving() {
        BlueprintEditor editor = editor(new RecordingGateway());
        GuiHarness harness = open(editor);

        assertFalse(
            harness.render()
                .shows("Unsaved changes"),
            "nothing should be asked before an edit");
        harness.click(
            harness.render()
                .button("Wipe"));
        assertTrue(editor.isDirty(), "wiping the canvas is an edit");

        harness.key(Keyboard.KEY_ESCAPE);

        assertTrue(
            harness.render()
                .shows("Unsaved changes"),
            "escape threw the work away without asking:\n" + harness.render());
    }

    @Test
    void savingSendsTheRenamedModelThroughTheGateway() {
        RecordingGateway gateway = new RecordingGateway();
        BlueprintEditor editor = editor(gateway);
        GuiHarness harness = open(editor);

        harness.dropScheduled();
        harness.click(
            harness.render()
                .button("Save"));

        assertSame(editor.getModel(), gateway.saved, "the editor did not hand its model over");
        assertEquals(0, gateway.savedIndex);
        assertEquals(1, harness.scheduled(), "saving should also ask the client to leave the editor");
    }
}
