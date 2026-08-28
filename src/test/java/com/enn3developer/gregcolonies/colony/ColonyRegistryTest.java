package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class ColonyRegistryTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private int dirty;

    private ColonyRegistry registry;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @BeforeEach
    void freshRegistry() {
        dirty = 0;
        registry = new ColonyRegistry(() -> dirty++);
    }

    private Colony create(String name, UUID owner, int dimension, int x, int z) {
        return registry.createColony(name, owner, "owner", dimension, x, 64, z);
    }

    @Test
    void startsEmpty() {
        assertEquals(0, registry.getColonyCount());
        assertNull(registry.getColony(1));
        assertTrue(
            registry.getColonies()
                .isEmpty());
    }

    @Test
    void idsAreHandedOutInSequence() {
        assertEquals(1, create("a", OWNER, 0, 0, 0).getId());
        assertEquals(2, create("b", OWNER, 0, 0, 0).getId());
        assertEquals(3, registry.getNextId());
    }

    @Test
    void creatingMarksTheStoreDirty() {
        create("a", OWNER, 0, 0, 0);
        assertEquals(1, dirty);
    }

    @Test
    void coloniesCanBeLookedUpByIdAndPosition() {
        Colony colony = create("a", OWNER, 0, 10, 20);
        assertSame(colony, registry.getColony(colony.getId()));
        assertSame(colony, registry.getColonyAt(0, 10, 64, 20));
        assertNull(registry.getColonyAt(0, 10, 65, 20));
        assertNull(registry.getColonyAt(1, 10, 64, 20));
    }

    @Test
    void theNearestColonyWins() {
        Colony near = create("near", OWNER, 0, 10, 0);
        create("far", OWNER, 0, 500, 0);
        assertSame(near, registry.getNearestColony(0, 0, 0));
    }

    @Test
    void theNearestColonyIgnoresOtherDimensions() {
        create("elsewhere", OWNER, 1, 0, 0);
        assertNull(registry.getNearestColony(0, 0, 0));
    }

    @Test
    void theNearestOwnedColonySkipsOtherOwners() {
        create("theirs", OTHER, 0, 1, 0);
        Colony mine = create("mine", OWNER, 0, 400, 0);
        assertSame(mine, registry.getNearestColonyOf(OWNER, 0, 0, 0));
        assertNull(registry.getNearestColonyOf(UUID.randomUUID(), 0, 0, 0));
    }

    @Test
    void nearestOnAnEmptyRegistryIsNull() {
        assertNull(registry.getNearestColony(0, 0, 0));
        assertNull(registry.getNearestColonyOf(OWNER, 0, 0, 0));
    }

    @Test
    void mutatingAnUnknownColonyChangesNothing() {
        assertFalse(registry.setSite(99, ColonySiteKind.MATERIALS, 1, 2, 3));
        assertFalse(registry.clearSite(99, ColonySiteKind.MATERIALS));
        assertFalse(registry.removeColony(99));
        assertFalse(registry.setPlacement(99, 1, true));
        assertFalse(registry.setBuildSite(99, null));
        assertEquals(-1, registry.addBlueprint(99, Fixtures.single("a")));
        assertEquals(0, registry.clearOrders(99));
        assertEquals(0, dirty);
    }

    @Test
    void settingASiteMarksDirty() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        dirty = 0;

        assertTrue(registry.setSite(colony.getId(), ColonySiteKind.MATERIALS, 1, 2, 3));
        assertTrue(
            colony.site(ColonySiteKind.MATERIALS)
                .isAt(1, 2, 3));
        assertEquals(1, dirty);
    }

    @Test
    void clearingAnAbsentSiteIsNotAChange() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        dirty = 0;

        assertFalse(registry.clearSite(colony.getId(), ColonySiteKind.MATERIALS));
        assertEquals(0, dirty);

        registry.setSite(colony.getId(), ColonySiteKind.MATERIALS, 1, 2, 3);
        dirty = 0;
        assertTrue(registry.clearSite(colony.getId(), ColonySiteKind.MATERIALS));
        assertEquals(1, dirty);
    }

    @Test
    void blueprintsGoThroughTheRegistry() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        dirty = 0;

        assertEquals(0, registry.addBlueprint(colony.getId(), Fixtures.single("a")));
        assertEquals(1, dirty);
        assertTrue(registry.renameBlueprint(colony.getId(), 0, "hut"));
        assertTrue(registry.replaceBlueprint(colony.getId(), 0, Fixtures.single("b")));
        assertTrue(registry.setActiveBlueprint(colony.getId(), 0));
        assertTrue(registry.removeBlueprint(colony.getId(), 0));
        assertFalse(registry.removeBlueprint(colony.getId(), 0));
    }

    @Test
    void aFullLibraryReportsNoSlot() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        for (int i = 0; i < Colony.MAX_BLUEPRINTS; i++) {
            registry.addBlueprint(colony.getId(), Fixtures.single("b" + i));
        }
        dirty = 0;
        assertEquals(-1, registry.addBlueprint(colony.getId(), Fixtures.single("full")));
        assertEquals(0, dirty);
    }

    @Test
    void placementIsStoredOnTheColony() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        assertTrue(registry.setPlacement(colony.getId(), 5, true));
        assertEquals(1, colony.getPlaceRotation());
        assertTrue(colony.isPlaceMirror());
    }

    @Test
    void buildSitesAreSetAndClaimed() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        UUID builder = UUID.randomUUID();

        assertTrue(registry.setBuildSite(colony.getId(), new BuildSite(0, 64, 0, Fixtures.single("hut"), 0, false)));
        assertNotNull(colony.getBuildSite());
        assertTrue(registry.claimBuildSite(colony.getId(), builder, 0L));
        assertFalse(registry.claimBuildSite(colony.getId(), UUID.randomUUID(), 1L));

        registry.releaseBuildSite(colony.getId(), builder);
        assertTrue(registry.claimBuildSite(colony.getId(), UUID.randomUUID(), 2L));
    }

    @Test
    void claimingOnAnUnknownColonyIsFalseAndHarmless() {
        assertFalse(registry.claimBuildSite(99, UUID.randomUUID(), 0L));
        registry.releaseBuildSite(99, UUID.randomUUID());
        registry.releaseBed(99, UUID.randomUUID());
    }

    @Test
    void citizenGroupsAndJobsGoThroughTheRegistry() {
        UUID citizenId = UUID.randomUUID();
        Colony colony = Fixtures
            .colonyWith(create("a", OWNER, 0, 0, 0), Fixtures.citizen(citizenId, "Aeliana", "", 0, 0, 64, 0));
        registry.put(colony);
        dirty = 0;

        assertTrue(registry.setCitizenGroup(colony.getId(), citizenId, "alpha"));
        assertEquals(
            "alpha",
            colony.getCitizen(citizenId)
                .getGroup());
        assertTrue(registry.setCitizenJob(colony.getId(), citizenId, CitizenJob.BUILDER));
        assertEquals(
            CitizenJob.BUILDER,
            colony.getCitizen(citizenId)
                .getJob());
        assertEquals(2, dirty);

        assertFalse(registry.setCitizenGroup(colony.getId(), UUID.randomUUID(), "alpha"));
        assertFalse(registry.setCitizenJob(colony.getId(), UUID.randomUUID(), CitizenJob.BUILDER));
    }

    @Test
    void bedsGoThroughTheRegistry() {
        UUID citizenId = UUID.randomUUID();
        Colony colony = Fixtures
            .colonyWith(create("a", OWNER, 0, 0, 0), Fixtures.citizen(citizenId, "Aeliana", "", 0, 0, 64, 0));
        registry.put(colony);

        assertTrue(registry.claimBed(colony.getId(), citizenId, 5, 64, 5));
        assertTrue(
            colony.getCitizen(citizenId)
                .isBedAt(5, 64, 5));

        registry.releaseBed(colony.getId(), citizenId);
        assertFalse(
            colony.getCitizen(citizenId)
                .hasBed());
    }

    @Test
    void removingACitizenGoesThroughTheRegistry() {
        UUID citizenId = UUID.randomUUID();
        Colony colony = Fixtures
            .colonyWith(create("a", OWNER, 0, 0, 0), Fixtures.citizen(citizenId, "Aeliana", "", 0, 0, 64, 0));
        registry.put(colony);

        assertTrue(registry.removeCitizen(colony.getId(), citizenId));
        assertFalse(registry.removeCitizen(colony.getId(), citizenId));
    }

    @Test
    void ordersAreQueuedAndCleared() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        TestCommand alpha = new TestCommand();
        alpha.setTargetGroup("alpha");

        assertTrue(registry.enqueueOrder(colony.getId(), alpha));
        assertTrue(registry.enqueueOrder(colony.getId(), new TestCommand()));
        assertEquals(2, colony.getOrderCount());

        assertEquals(1, registry.clearOrders(colony.getId(), "alpha"));
        assertEquals(1, registry.clearOrders(colony.getId()));
        assertEquals(0, colony.getOrderCount());
    }

    @Test
    void clearingNothingDoesNotMarkDirty() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        dirty = 0;
        assertEquals(0, registry.clearOrders(colony.getId()));
        assertEquals(0, registry.clearOrders(colony.getId(), "alpha"));
        assertEquals(0, dirty);
    }

    @Test
    void removingAColonyMarksDirty() {
        Colony colony = create("a", OWNER, 0, 0, 0);
        dirty = 0;

        assertTrue(registry.removeColony(colony.getId()));
        assertEquals(1, dirty);
        assertEquals(0, registry.getColonyCount());
        assertFalse(registry.removeColony(colony.getId()));
        assertEquals(1, dirty);
    }

    @Test
    void theColonyViewIsReadOnly() {
        create("a", OWNER, 0, 0, 0);
        assertThrows(
            UnsupportedOperationException.class,
            () -> registry.getColonies()
                .clear());
    }

    @Test
    void clearResetsTheStore() {
        create("a", OWNER, 0, 0, 0);
        create("b", OWNER, 0, 0, 0);
        registry.clear();

        assertEquals(0, registry.getColonyCount());
        assertEquals(1, registry.getNextId());
    }

    @Test
    void nextIdIsNeverBelowOne() {
        registry.setNextId(0);
        assertEquals(1, registry.getNextId());
        registry.setNextId(-5);
        assertEquals(1, registry.getNextId());
        registry.setNextId(9);
        assertEquals(9, registry.getNextId());
    }

    @Test
    void putRestoresAColonyWithoutMarkingDirty() {
        Colony colony = new Colony(42, "loaded", OWNER, "owner", 0, 0, 64, 0);
        registry.put(colony);

        assertEquals(0, dirty);
        assertSame(colony, registry.getColony(42));
        assertEquals(1, registry.getColonyCount());
    }

    @Test
    void markDirtyIsForwarded() {
        registry.markDirty();
        assertEquals(1, dirty);
    }
}
