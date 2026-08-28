package com.enn3developer.gregcolonies.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.gui.ClientBootstrap;
import com.enn3developer.gregcolonies.testing.gui.GuiHarness;
import com.enn3developer.gregcolonies.testing.gui.Rendered;

import gregtech.common.GTMockWorld;

class ColonyScreenTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID ANNA = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID BRUNO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeAll
    static void boot() {
        ClientBootstrap.ensure();
    }

    private static ColonySnapshot snapshot() {
        Colony colony = Fixtures.colonyWith(
            new Colony(1, "Home", OWNER, "Enn3", 0, 0, 64, 0),
            Fixtures.citizen(ANNA, "Anna", "", 0, 1, 64, 1),
            Fixtures.citizen(BRUNO, "Bruno", "diggers", 0, 2, 64, 2));
        return ColonySnapshot.of(colony, new GTMockWorld());
    }

    private static GuiHarness open(ColonyView view) {
        return GuiHarness.open(new ColonyScreen(view));
    }

    @Test
    void theColonyScreenLaysOutItsHeader() {
        GuiHarness harness = open(new ColonyView(snapshot()));
        Rendered rendered = harness.render();

        Rendered.Node title = rendered.byText("Home #1");
        assertTrue(title.width() > 0 && title.height() > 0, "the title has no area: " + title);
        assertTrue(title.x() >= 0 && title.x() + title.width() <= harness.width(), "the title is off screen: " + title);
        assertTrue(rendered.shows("owner Enn3   dim 0   0/64/0   r64"), "the header is missing:\n" + rendered);
        assertSame(
            title.widget(),
            rendered.at(title.centerX(), title.centerY())
                .widget());
    }

    @Test
    void theScreenTellsYouHowManyCitizensThereAre() {
        GuiHarness harness = open(new ColonyView(snapshot()));

        assertTrue(
            harness.render()
                .shows("2 citizens   0 loaded   0 orders"),
            "the citizen summary is wrong:\n" + harness.render());
    }

    @Test
    void clickingSelectAllSelectsEveryCitizen() {
        ColonyView view = new ColonyView(snapshot());
        GuiHarness harness = open(view);

        assertTrue(
            view.getSelection()
                .isEmpty(),
            "nothing should be selected before the click");
        assertTrue(
            harness.click(
                harness.render()
                    .byText("Select all")),
            "the button did not take the click");

        assertEquals(
            2,
            view.getSelection()
                .size());
        assertTrue(
            view.getSelection()
                .contains(ANNA));
        assertTrue(
            view.getSelection()
                .contains(BRUNO));
    }

    @Test
    void typingIntoTheGroupFieldReachesTheWidget() {
        GuiHarness harness = open(new ColonyView(snapshot()));
        Rendered.Node field = harness.render()
            .ofType(TextFieldWidget.class)
            .get(0);

        harness.click(field);
        harness.type("diggers");

        assertEquals("diggers", ((TextFieldWidget) field.widget()).getText());
    }

    @Test
    void aNarrowScreenStillFitsTheHeader() {
        GuiHarness harness = open(new ColonyView(snapshot()));
        harness.resize(320, 200);
        Rendered rendered = harness.render();

        Rendered.Node title = rendered.byText("Home #1");
        assertTrue(title.x() + title.width() <= 320, "the title spills off a narrow screen: " + title);
        for (Rendered.Node node : rendered.visible()) {
            assertTrue(node.x() >= 0 && node.y() >= 0, "widget placed off screen: " + node);
        }
    }
}
