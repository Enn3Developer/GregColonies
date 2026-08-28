package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class ColonyActionsTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private int dirty;

    private ColonyRegistry registry;

    private Colony colony;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @BeforeEach
    void freshColony() {
        dirty = 0;
        registry = new ColonyRegistry(() -> dirty++);
        colony = registry.createColony("Home", OWNER, "Enn3", 0, 100, 64, 200);
    }

    private static class FakeControl implements CitizenControl {

        private final List<String> assigned = new ArrayList<>();

        private double distance;

        private int stopped;

        private String stoppedGroup;

        FakeControl(int stopped, double distance) {
            this.stopped = stopped;
            this.distance = distance;
        }

        @Override
        public int stopWork(String group) {
            stoppedGroup = group;
            return stopped;
        }

        @Override
        public double distanceSq(ColonyCitizen entry, int dimension, double x, double z) {
            return distance;
        }

        @Override
        public void assign(ColonyCitizen entry, String group) {
            assigned.add(entry.getName() + "->" + group);
            entry.setGroup(group);
        }
    }

    private void readyForBuilding() {
        colony.addBlueprint(Fixtures.single("hut"));
        colony.site(ColonySiteKind.MATERIALS)
            .set(1, 64, 1);
    }

    @Test
    void clearingTheBuildSiteAlwaysSucceeds() {
        Outcome outcome = ColonyActions.clearBuildSite(registry, colony);
        assertTrue(outcome.isOk());
        assertEquals("Build site cleared", outcome.getMessage());
        assertNull(colony.getBuildSite());
    }

    @Test
    void aBuildSiteMustBeInTheColonyDimension() {
        readyForBuilding();
        Outcome outcome = ColonyActions.setBuildSite(registry, colony, 1, 10, 64, 10, site -> 0);
        assertFalse(outcome.isOk());
        assertEquals("The build site must be in the colony dimension", outcome.getMessage());
        assertNull(colony.getBuildSite());
    }

    @Test
    void aBuildSiteNeedsABlueprint() {
        colony.site(ColonySiteKind.MATERIALS)
            .set(1, 64, 1);
        Outcome outcome = ColonyActions.setBuildSite(registry, colony, 0, 10, 64, 10, site -> 0);
        assertFalse(outcome.isOk());
        assertEquals("Capture a blueprint before starting a build", outcome.getMessage());
    }

    @Test
    void aBuildSiteNeedsAMaterialsChest() {
        colony.addBlueprint(Fixtures.single("hut"));
        Outcome outcome = ColonyActions.setBuildSite(registry, colony, 0, 10, 64, 10, site -> 0);
        assertFalse(outcome.isOk());
        assertEquals("Set a materials chest before starting a build", outcome.getMessage());
    }

    @Test
    void theBuildSiteSitsOneBlockAboveTheClickedBlock() {
        readyForBuilding();
        Outcome outcome = ColonyActions.setBuildSite(registry, colony, 0, 10, 64, 20, site -> 3);

        assertTrue(outcome.isOk());
        assertEquals("Build site set at 10/65/20, 3 blocks to place", outcome.getMessage());
        assertNotNull(colony.getBuildSite());
        assertTrue(
            colony.getBuildSite()
                .isAt(10, 65, 20));
    }

    @Test
    void theBuildSiteTakesTheColonyPlacement() {
        readyForBuilding();
        colony.setPlaceRotation(1);
        colony.setPlaceMirror(true);
        ColonyActions.setBuildSite(registry, colony, 0, 10, 64, 20, site -> 0);

        assertEquals(
            1,
            colony.getBuildSite()
                .getRotation());
        assertTrue(
            colony.getBuildSite()
                .isMirrored());
    }

    @Test
    void theSurveyIsAskedForTheRemainingCount() {
        readyForBuilding();
        int[] calls = { 0 };
        ColonyActions.setBuildSite(registry, colony, 0, 0, 64, 0, site -> {
            calls[0]++;
            return 7;
        });
        assertEquals(1, calls[0]);
    }

    @Test
    void clearingASiteAlwaysSucceeds() {
        Outcome outcome = ColonyActions.clearSite(registry, colony, ColonySiteKind.PICK_UP);
        assertTrue(outcome.isOk());
        assertEquals("Colony pick-up cleared", outcome.getMessage());
    }

    @Test
    void aSiteMustBeInTheColonyDimension() {
        Outcome outcome = ColonyActions.setSite(registry, colony, ColonySiteKind.DROP_OFF, 1, true, 1, 2, 3);
        assertFalse(outcome.isOk());
        assertEquals("The drop-off must be in the colony dimension", outcome.getMessage());
    }

    @Test
    void aSiteMustHaveAnInventory() {
        Outcome outcome = ColonyActions.setSite(registry, colony, ColonySiteKind.DROP_OFF, 0, false, 1, 2, 3);
        assertFalse(outcome.isOk());
        assertEquals("That block has no inventory", outcome.getMessage());
        assertFalse(
            colony.site(ColonySiteKind.DROP_OFF)
                .isPresent());
    }

    @Test
    void settingASiteReportsThePosition() {
        Outcome outcome = ColonyActions.setSite(registry, colony, ColonySiteKind.MATERIALS, 0, true, 1, 2, 3);
        assertTrue(outcome.isOk());
        assertEquals("Colony materials pick-up set to 1/2/3", outcome.getMessage());
        assertTrue(
            colony.site(ColonySiteKind.MATERIALS)
                .isAt(1, 2, 3));
    }

    @Test
    void aBlueprintWithNothingInItIsRejected() {
        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, Blueprint.empty("empty", 2, 2, 2), -1);
        assertFalse(outcome.isOk());
        assertEquals(
            "That design holds no buildable blocks, or uses blocks this server does not have",
            outcome.getMessage());
    }

    @Test
    void aNullBlueprintIsRejected() {
        assertFalse(
            ColonyActions.storeBlueprint(registry, colony, null, -1)
                .isOk());
    }

    @Test
    void aBlueprintWithUnknownBlocksIsRejected() {
        Blueprint broken = Blueprint.empty("broken", 1, 1, 1);
        broken.getPalette()
            .add("nosuchmod:nosuchblock");
        broken.setCell(0, 0, 0, Blueprint.cell(1, 0));

        assertFalse(
            ColonyActions.storeBlueprint(registry, colony, broken, -1)
                .isOk());
    }

    @Test
    void storingANewBlueprintReportsItsSlotAndSize() {
        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, Fixtures.cube("tower", 2), -1);
        assertTrue(outcome.isOk());
        assertEquals(0, outcome.getValue());
        assertEquals("Blueprint saved: 2x2x2, 8 blocks", outcome.getMessage());
    }

    @Test
    void storingTrimsBeforeSaving() {
        Blueprint loose = Blueprint.empty("loose", 4, 4, 4);
        loose.setCell(
            1,
            1,
            1,
            loose.getPalette()
                .cellFor(net.minecraft.init.Blocks.stone, 0));

        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, loose, -1);
        assertTrue(outcome.isOk());
        assertEquals("Blueprint saved: 1x1x1, 1 blocks", outcome.getMessage());
    }

    @Test
    void storingOverAnExistingSlotReplacesIt() {
        colony.addBlueprint(Fixtures.single("old"));
        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, Fixtures.cube("new", 2), 0);

        assertTrue(outcome.isOk());
        assertEquals(0, outcome.getValue());
        assertEquals(
            1,
            colony.getBlueprints()
                .size());
        assertEquals(
            8,
            colony.getBlueprint(0)
                .blockCount());
    }

    @Test
    void storingOverAMissingSlotIsSilent() {
        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, Fixtures.single("new"), 5);
        assertFalse(outcome.isOk());
        assertFalse(outcome.hasMessage());
    }

    @Test
    void aFullLibraryIsReportedNotSilentlyDropped() {
        for (int i = 0; i < Colony.MAX_BLUEPRINTS; i++) {
            colony.addBlueprint(Fixtures.single("b" + i));
        }
        Outcome outcome = ColonyActions.storeBlueprint(registry, colony, Fixtures.single("extra"), -1);

        assertFalse(outcome.isOk());
        assertEquals(
            "The blueprint library is full (" + Colony.MAX_BLUEPRINTS + "), delete one first",
            outcome.getMessage());
    }

    @Test
    void queueingAnUngroupedOrderCountsEveryPendingOrder() {
        TestCommand alpha = new TestCommand();
        alpha.setTargetGroup("alpha");
        colony.enqueueOrder(alpha);

        Outcome outcome = ColonyActions.enqueueOrder(registry, colony, new TestCommand(), "");
        assertTrue(outcome.isOk());
        assertEquals(
            "Queued gregcolonies:test for colony #1 (2 order(s) pending)",
            outcome.getMessage(),
            "an ungrouped order reports the whole queue, as the command always has");
    }

    @Test
    void queueingAGroupedOrderCountsOnlyThatGroup() {
        colony.enqueueOrder(new TestCommand());

        Outcome outcome = ColonyActions.enqueueOrder(registry, colony, new TestCommand(), "alpha");
        assertTrue(outcome.isOk());
        assertEquals(
            "Queued gregcolonies:test for group alpha of colony #1 (1 order(s) pending)",
            outcome.getMessage());
    }

    @Test
    void queueingStampsTheGroupOnTheCommand() {
        TestCommand command = new TestCommand();
        ColonyActions.enqueueOrder(registry, colony, command, "alpha");
        assertEquals("alpha", command.getTargetGroup());
        assertEquals(1, colony.getOrderCount("alpha"));
    }

    @Test
    void aNullGroupMeansUngrouped() {
        TestCommand command = new TestCommand();
        ColonyActions.enqueueOrder(registry, colony, command, null);
        assertEquals("", command.getTargetGroup());
    }

    @Test
    void cancellingEverythingReportsBothCounts() {
        colony.enqueueOrder(new TestCommand());
        colony.enqueueOrder(new TestCommand());

        FakeControl control = new FakeControl(3, 0.0D);
        Outcome outcome = ColonyActions.cancelOrders(registry, colony, "", control);

        assertTrue(outcome.isOk());
        assertEquals("Dropped 2 pending order(s), stopped 3 citizen(s)", outcome.getMessage());
        assertEquals("", control.stoppedGroup);
    }

    @Test
    void cancellingAGroupNamesIt() {
        TestCommand alpha = new TestCommand();
        alpha.setTargetGroup("alpha");
        colony.enqueueOrder(alpha);
        colony.enqueueOrder(new TestCommand());

        FakeControl control = new FakeControl(2, 0.0D);
        Outcome outcome = ColonyActions.cancelOrders(registry, colony, "alpha", control);

        assertEquals("Dropped 1 order(s) for group alpha, stopped 2 citizen(s)", outcome.getMessage());
        assertEquals("alpha", control.stoppedGroup);
        assertEquals(1, colony.getOrderCount(), "orders for other groups must survive");
    }

    @Test
    void cancellingStopsWorkEvenWithNothingQueued() {
        FakeControl control = new FakeControl(1, 0.0D);
        Outcome outcome = ColonyActions.cancelOrders(registry, colony, null, control);
        assertEquals("Dropped 0 pending order(s), stopped 1 citizen(s)", outcome.getMessage());
    }

    @Test
    void assigningPutsEveryCitizenInRangeIntoTheGroup() {
        Colony populated = Fixtures.colonyWith(
            colony,
            Fixtures.citizen(UUID.randomUUID(), "Aeliana", "", 0, 1, 64, 1),
            Fixtures.citizen(UUID.randomUUID(), "Marcus", "", 0, 2, 64, 2));
        registry.put(populated);

        FakeControl control = new FakeControl(0, 4.0D);
        Outcome outcome = ColonyActions.assignGroup(registry, populated, "alpha", 0, 0.0D, 0.0D, 8, control);

        assertTrue(outcome.isOk());
        assertEquals("2 citizen(s) put into group alpha", outcome.getMessage());
        assertEquals(2, control.assigned.size());
    }

    @Test
    void assigningSkipsCitizensOutOfRange() {
        Colony populated = Fixtures.colonyWith(colony, Fixtures.citizen(UUID.randomUUID(), "Aeliana", "", 0, 1, 64, 1));
        registry.put(populated);

        FakeControl control = new FakeControl(0, 1000.0D);
        Outcome outcome = ColonyActions.assignGroup(registry, populated, "alpha", 0, 0.0D, 0.0D, 8, control);

        assertEquals("0 citizen(s) put into group alpha", outcome.getMessage());
        assertTrue(control.assigned.isEmpty());
    }

    @Test
    void theRangeCheckUsesTheSquaredRadius() {
        Colony populated = Fixtures.colonyWith(colony, Fixtures.citizen(UUID.randomUUID(), "Aeliana", "", 0, 1, 64, 1));
        registry.put(populated);

        assertEquals(
            "1 citizen(s) put into group alpha",
            ColonyActions.assignGroup(registry, populated, "alpha", 0, 0, 0, 8, new FakeControl(0, 64.0D))
                .getMessage());
        assertEquals(
            "0 citizen(s) put into group alpha",
            ColonyActions.assignGroup(registry, populated, "alpha", 0, 0, 0, 8, new FakeControl(0, 64.1D))
                .getMessage());
    }

    @Test
    void assigningAnEmptyGroupUngroups() {
        Colony populated = Fixtures
            .colonyWith(colony, Fixtures.citizen(UUID.randomUUID(), "Aeliana", "alpha", 0, 1, 64, 1));
        registry.put(populated);

        Outcome outcome = ColonyActions
            .assignGroup(registry, populated, null, 0, 0.0D, 0.0D, 8, new FakeControl(0, 0.0D));

        assertEquals("1 citizen(s) ungrouped", outcome.getMessage());
    }

    @Test
    void assigningMarksTheStoreDirty() {
        dirty = 0;
        ColonyActions.assignGroup(registry, colony, "alpha", 0, 0.0D, 0.0D, 8, new FakeControl(0, 0.0D));
        assertEquals(1, dirty);
    }
}
