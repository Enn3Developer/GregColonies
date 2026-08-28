package com.enn3developer.gregcolonies.testing.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widget.Widget;

class GuiHarnessTest {

    private static final int PROBE_WIDTH = 60;

    private static final int PROBE_HEIGHT = 20;

    private static final class Probe extends Widget<Probe> implements Interactable {

        private final List<String> events = new ArrayList<>();

        @Override
        public Result onMousePressed(int mouseButton) {
            events.add("press " + mouseButton);
            return Result.SUCCESS;
        }

        @Override
        public boolean onMouseRelease(int mouseButton) {
            events.add("release " + mouseButton);
            return true;
        }

        @Override
        public void onMouseDrag(int mouseButton, long since) {
            events.add("drag " + mouseButton);
        }

        @Override
        public boolean onMouseScroll(UpOrDown scroll, int amount) {
            events.add("scroll " + scroll);
            return true;
        }

        @Override
        public Result onKeyPressed(char typedChar, int keyCode) {
            events.add("key " + keyCode + " '" + typedChar + "'");
            return Result.SUCCESS;
        }
    }

    private Probe probe;

    @BeforeAll
    static void boot() {
        ClientBootstrap.ensure();
    }

    private GuiHarness open() {
        probe = new Probe();
        probe.width(PROBE_WIDTH)
            .height(PROBE_HEIGHT)
            .left(10)
            .top(10);
        ModularPanel panel = ModularPanel.defaultPanel("harness_probe", 200, 100);
        panel.child(probe);
        return GuiHarness.open(new ModularScreen("gregcolonies_test", panel));
    }

    private Rendered.Node probeNode(GuiHarness harness) {
        return harness.render()
            .ofType(Probe.class)
            .get(0);
    }

    @Test
    void aPanelIsLaidOutInsideTheScreen() {
        GuiHarness harness = open();
        Rendered.Node panel = harness.render()
            .all()
            .get(0);
        Rendered.Node node = probeNode(harness);

        assertEquals(200, panel.width());
        assertEquals(100, panel.height());
        assertEquals(PROBE_WIDTH, node.width());
        assertEquals(PROBE_HEIGHT, node.height());
        assertEquals(panel.x() + 10, node.x());
        assertEquals(panel.y() + 10, node.y());
        assertEquals(
            harness.render()
                .onScreen(harness.width(), harness.height())
                .size(),
            harness.render()
                .visible()
                .size(),
            "everything should fit on screen");
    }

    @Test
    void aClickReachesTheWidgetUnderTheCursor() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);

        assertTrue(harness.click(node), "the widget did not take the click");

        assertEquals(Arrays.asList("press 0", "release 0"), probe.events);
    }

    @Test
    void aClickOutsideTheWidgetIsNotDelivered() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);

        harness.click(node.x() - 5, node.y() - 5);

        assertTrue(probe.events.isEmpty(), "the widget took a click that missed it: " + probe.events);
    }

    @Test
    void aRightClickCarriesItsButton() {
        GuiHarness harness = open();

        harness.click(probeNode(harness), GuiHarness.RIGHT);

        assertEquals(Arrays.asList("press 1", "release 1"), probe.events);
    }

    @Test
    void aDragDeliversEveryStepBetweenPressAndRelease() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);

        harness.drag(node.x() + 2, node.centerY(), node.x() + node.width() - 2, node.centerY());

        assertEquals("press 0", probe.events.get(0));
        assertEquals("release 0", probe.events.get(probe.events.size() - 1));
        assertTrue(
            probe.events.stream()
                .filter("drag 0"::equals)
                .count() > 1,
            "a drag should deliver more than one step: " + probe.events);
    }

    @Test
    void scrollingCarriesItsDirection() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);

        harness.scrollUp(node.centerX(), node.centerY());
        harness.scrollDown(node.centerX(), node.centerY());

        assertEquals(Arrays.asList("scroll UP", "scroll DOWN"), probe.events);
    }

    @Test
    void keysReachTheFocusedWidget() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);
        harness.click(node);
        probe.events.clear();

        harness.key('x', Keyboard.KEY_X);

        assertEquals(Arrays.asList("key " + Keyboard.KEY_X + " 'x'"), probe.events);
    }

    @Test
    void theMouseHoversWhatItMovesOver() {
        GuiHarness harness = open();
        Rendered.Node node = probeNode(harness);

        harness.moveMouse(node.centerX(), node.centerY());
        assertTrue(probe.isHovering(), "the widget should be hovered");

        harness.moveMouse(node.x() - 5, node.y() - 5);
        assertFalse(probe.isHovering(), "the widget should have lost the hover");
    }

    @Test
    void resizingRelaysOutTheScreen() {
        GuiHarness harness = open();
        int before = probeNode(harness).x();

        harness.resize(640, 400);

        assertEquals(640, harness.width());
        assertNotEquals(before, probeNode(harness).x(), "a centred panel should move when the screen grows");
    }
}
