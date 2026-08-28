package com.enn3developer.gregcolonies.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.network.ColonySnapshot;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

import gregtech.common.GTMockWorld;

class ColonySelectionTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID ALPHA_ONE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID ALPHA_TWO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final UUID LOOSE = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private ColonySelection selection;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    @BeforeEach
    void freshSelection() {
        selection = new ColonySelection();
        selection.setColony(snapshot(citizens()));
    }

    private static List<ColonyCitizen> citizens() {
        List<ColonyCitizen> citizens = new ArrayList<>();
        citizens.add(Fixtures.citizen(ALPHA_ONE, "Aeliana", "alpha", 0, 1, 64, 1));
        citizens.add(Fixtures.citizen(ALPHA_TWO, "Marcia", "alpha", 0, 2, 64, 2));
        citizens.add(Fixtures.citizen(LOOSE, "Marcus", "", 0, 3, 64, 3));
        return citizens;
    }

    private static ColonySnapshot snapshot(List<ColonyCitizen> citizens) {
        Colony colony = Fixtures
            .colonyWith(new Colony(1, "Home", OWNER, "Enn3", 0, 0, 64, 0), citizens.toArray(new ColonyCitizen[0]));
        return ColonySnapshot.of(colony, new GTMockWorld());
    }

    @Test
    void groupsAreCollectedAndSorted() {
        assertEquals(
            2,
            selection.getGroups()
                .size());
        assertEquals("alpha", selection.groupAt(0));
        assertEquals(ColonySelection.UNGROUPED, selection.groupAt(1));
        assertNull(selection.groupAt(2));
        assertNull(selection.groupAt(-1));
    }

    @Test
    void nothingIsSelectedToBeginWith() {
        assertTrue(selection.isEmpty());
        assertEquals(0, selection.size());
        assertNull(selection.single());
    }

    @Test
    void togglingAddsThenRemoves() {
        selection.toggle(ALPHA_ONE);
        assertTrue(selection.isSelected(ALPHA_ONE));
        assertEquals(1, selection.size());

        selection.toggle(ALPHA_ONE);
        assertFalse(selection.isSelected(ALPHA_ONE));
        assertTrue(selection.isEmpty());
    }

    @Test
    void selectAllTakesEveryCitizen() {
        selection.selectAll();
        assertEquals(3, selection.size());
        assertTrue(selection.isSelected(LOOSE));
    }

    @Test
    void selectAllReplacesWhatWasThere() {
        selection.toggle(UUID.randomUUID());
        selection.selectAll();
        assertEquals(3, selection.size());
    }

    @Test
    void selectingAGroupReplacesTheSelection() {
        selection.toggle(LOOSE);
        selection.selectGroup("alpha", false);

        assertEquals(2, selection.size());
        assertTrue(selection.isSelected(ALPHA_ONE));
        assertFalse(selection.isSelected(LOOSE));
    }

    @Test
    void selectingAGroupCanAddToTheSelection() {
        selection.toggle(LOOSE);
        selection.selectGroup("alpha", true);

        assertEquals(3, selection.size());
        assertTrue(selection.isSelected(LOOSE));
    }

    @Test
    void theUngroupedBucketIsSelectableByItsLabel() {
        selection.selectGroup(ColonySelection.UNGROUPED, false);
        assertEquals(1, selection.size());
        assertTrue(selection.isSelected(LOOSE));
    }

    @Test
    void selectingAnUnknownGroupSelectsNothing() {
        selection.selectGroup("nope", false);
        assertTrue(selection.isEmpty());
    }

    @Test
    void singleOnlyAnswersForExactlyOne() {
        assertNull(selection.single());

        selection.toggle(ALPHA_ONE);
        assertNotNull(selection.single());
        assertEquals(
            "Aeliana",
            selection.single()
                .getName());

        selection.toggle(ALPHA_TWO);
        assertNull(selection.single());
    }

    @Test
    void clearEmptiesTheSelection() {
        selection.selectAll();
        selection.clear();
        assertTrue(selection.isEmpty());
    }

    @Test
    void refreshingDropsCitizensThatAreGone() {
        selection.selectAll();
        assertEquals(3, selection.size());

        List<ColonyCitizen> fewer = new ArrayList<>();
        fewer.add(Fixtures.citizen(ALPHA_ONE, "Aeliana", "alpha", 0, 1, 64, 1));
        selection.setColony(snapshot(fewer));

        assertEquals(1, selection.size());
        assertTrue(selection.isSelected(ALPHA_ONE));
        assertFalse(selection.isSelected(LOOSE));
    }

    @Test
    void refreshingRebuildsTheGroupList() {
        List<ColonyCitizen> regrouped = new ArrayList<>();
        regrouped.add(Fixtures.citizen(ALPHA_ONE, "Aeliana", "beta", 0, 1, 64, 1));
        selection.setColony(snapshot(regrouped));

        assertEquals(
            1,
            selection.getGroups()
                .size());
        assertEquals("beta", selection.groupAt(0));
    }

    @Test
    void nobodyIsLoadedInAHeadlessWorld() {
        selection.selectAll();
        assertEquals(0, selection.countLoaded());
    }

    @Test
    void targetingTogglesOffWhenPickedTwice() {
        assertEquals(TargetMode.NONE, selection.getTargeting());

        selection.setTargeting(TargetMode.CHOP);
        assertEquals(TargetMode.CHOP, selection.getTargeting());

        selection.setTargeting(TargetMode.CHOP);
        assertEquals(TargetMode.NONE, selection.getTargeting());
    }

    @Test
    void switchingTargetModeReplacesTheOldOne() {
        selection.setTargeting(TargetMode.CHOP);
        selection.setTargeting(TargetMode.MINE);
        assertEquals(TargetMode.MINE, selection.getTargeting());
    }

    @Test
    void pendingRegionsNeedATargetMode() {
        selection.setPending(1, 2, 3, 4, 5, 6);
        assertFalse(selection.hasPending(), "a pending box means nothing without a target mode");

        selection.setTargeting(TargetMode.CHOP);
        selection.setPending(1, 2, 3, 4, 5, 6);
        assertTrue(selection.hasPending());
    }

    @Test
    void thePendingBoxKeepsItsCorners() {
        selection.setTargeting(TargetMode.FARM);
        selection.setPending(1, 2, 3, 4, 5, 6);

        int[] pending = selection.getPending();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, pending);
    }

    @Test
    void changingTargetModeDropsThePendingBox() {
        selection.setTargeting(TargetMode.CHOP);
        selection.setPending(1, 2, 3, 4, 5, 6);
        selection.setTargeting(TargetMode.MINE);
        assertFalse(selection.hasPending());
    }

    @Test
    void clearPendingDropsTheBox() {
        selection.setTargeting(TargetMode.CHOP);
        selection.setPending(1, 2, 3, 4, 5, 6);
        selection.clearPending();
        assertFalse(selection.hasPending());
    }

    @Test
    void theGroupLabelFallsBackToUngrouped() {
        ColonySnapshot colony = selection.getColony();
        for (com.enn3developer.gregcolonies.network.CitizenSnapshot citizen : colony.getCitizens()) {
            if (citizen.getId()
                .equals(LOOSE)) {
                assertEquals(ColonySelection.UNGROUPED, ColonySelection.groupLabel(citizen));
            } else {
                assertEquals("alpha", ColonySelection.groupLabel(citizen));
            }
        }
    }
}
