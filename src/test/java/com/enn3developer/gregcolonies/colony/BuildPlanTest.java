package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.init.Blocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class BuildPlanTest {

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    private static class FakeProbe implements BuildPlan.Probe {

        private final Set<Long> dirty = new HashSet<>();

        private final Set<Long> occupied = new HashSet<>();

        private final Set<Integer> slabs = new HashSet<>();

        @Override
        public boolean needsClearing(int x, int y, int z) {
            return dirty.contains(BlockKey.pack(x, y, z));
        }

        @Override
        public boolean canPlace(int x, int y, int z) {
            return !occupied.contains(BlockKey.pack(x, y, z));
        }

        @Override
        public boolean isFullCube(int cell) {
            return !slabs.contains(cell);
        }

        FakeProbe dirty(int x, int y, int z) {
            dirty.add(BlockKey.pack(x, y, z));
            return this;
        }

        FakeProbe occupied(int x, int y, int z) {
            occupied.add(BlockKey.pack(x, y, z));
            return this;
        }

        FakeProbe slab(int cell) {
            slabs.add(cell);
            return this;
        }

        void cleaned(int x, int y, int z) {
            dirty.remove(BlockKey.pack(x, y, z));
        }
    }

    private static Colony colony() {
        return new Colony(1, "test", UUID.randomUUID(), "owner", 0, 100, 64, 100);
    }

    private static Blueprint filled(int sizeX, int sizeY, int sizeZ) {
        Blueprint blueprint = Blueprint.empty("box", sizeX, sizeY, sizeZ);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    blueprint.setCell(x, y, z, cell);
                }
            }
        }
        return blueprint;
    }

    private static BuildSite siteAt(int x, int y, int z, Blueprint blueprint) {
        return new BuildSite(x, y, z, blueprint, 0, false);
    }

    private static long target(BuildPlan plan) {
        return BlockKey.pack(plan.getTargetX(), plan.getTargetY(), plan.getTargetZ());
    }

    @Test
    void startsInTheClearPhase() {
        BuildPlan plan = new BuildPlan();
        plan.start(siteAt(0, 64, 0, filled(2, 2, 2)));
        assertEquals(BuildPlan.Phase.CLEAR, plan.getPhase());
        assertFalse(plan.hasTarget());
    }

    @Test
    void aCleanSiteFallsStraightThroughToBuilding() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(2, 2, 2));
        plan.start(site);

        assertFalse(plan.nextTarget(new FakeProbe(), colony(), site));
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase());
    }

    @Test
    void clearingWorksFromTheTopDown() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 3, 1));
        plan.start(site);

        FakeProbe probe = new FakeProbe().dirty(0, 64, 0)
            .dirty(0, 66, 0);

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(66, plan.getTargetY());
    }

    @Test
    void clearingVisitsEveryDirtyCellExactlyOnce() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(2, 2, 2));
        plan.start(site);

        FakeProbe probe = new FakeProbe();
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 2; x++) {
                    probe.dirty(site.getX() + x, site.getY() + y, site.getZ() + z);
                }
            }
        }

        Set<Long> seen = new HashSet<>();
        while (plan.nextTarget(probe, colony(), site)) {
            assertTrue(seen.add(target(plan)), "cleared the same cell twice");
            probe.cleaned(plan.getTargetX(), plan.getTargetY(), plan.getTargetZ());
            plan.markProgress();
            plan.clearTarget();
        }
        assertEquals(8, seen.size());
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase());
    }

    @Test
    void theColonyCentreIsNeverCleared() {
        Colony colony = colony();
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(colony.getX(), colony.getY(), colony.getZ(), filled(1, 1, 1));
        plan.start(site);

        FakeProbe probe = new FakeProbe().dirty(colony.getX(), colony.getY(), colony.getZ());
        assertFalse(plan.nextTarget(probe, colony, site));
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase());
    }

    @Test
    void colonySitesAreNeverCleared() {
        Colony colony = colony();
        colony.site(ColonySiteKind.MATERIALS)
            .set(5, 64, 5);

        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(5, 64, 5, filled(1, 1, 1));
        plan.start(site);

        FakeProbe probe = new FakeProbe().dirty(5, 64, 5);
        assertFalse(plan.nextTarget(probe, colony, site));
    }

    @Test
    void blockingAClearTargetSkipsItForever() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 2, 1));
        plan.start(site);

        FakeProbe probe = new FakeProbe().dirty(0, 64, 0)
            .dirty(0, 65, 0);

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(65, plan.getTargetY());
        plan.blockTarget();

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(64, plan.getTargetY());
    }

    @Test
    void blockingIsForgottenWhenThePlanRestarts() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        FakeProbe probe = new FakeProbe().dirty(0, 64, 0);

        plan.start(site);
        assertTrue(plan.nextTarget(probe, colony(), site));
        plan.blockTarget();
        assertFalse(plan.nextTarget(probe, colony(), site));

        plan.start(site);
        assertTrue(plan.nextTarget(probe, colony(), site));
    }

    @Test
    void buildingWorksFromTheBottomUp() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 3, 1));
        plan.start(site);
        plan.nextTarget(new FakeProbe(), colony(), site);

        assertTrue(plan.nextTarget(new FakeProbe(), colony(), site));
        assertEquals(64, plan.getTargetY());
    }

    @Test
    void buildingVisitsEveryCellExactlyOnce() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(2, 2, 2));
        plan.start(site);
        plan.nextTarget(new FakeProbe(), colony(), site);

        Set<Long> seen = new HashSet<>();
        FakeProbe probe = new FakeProbe();
        while (plan.getPhase() == BuildPlan.Phase.BUILD && seen.size() < 8) {
            if (!plan.nextTarget(probe, colony(), site)) {
                continue;
            }
            assertTrue(seen.add(target(plan)), "built the same cell twice");
            plan.markProgress();
            plan.skipCell();
        }
        assertEquals(8, seen.size());
    }

    @Test
    void everyCellOfTheBoxIsReachable() {
        BuildPlan plan = new BuildPlan();
        Blueprint blueprint = filled(3, 4, 5);
        BuildSite site = siteAt(100, 64, 200, blueprint);
        plan.start(site);
        plan.nextTarget(new FakeProbe(), colony(), site);

        Set<Long> seen = new HashSet<>();
        FakeProbe probe = new FakeProbe();
        for (int guard = 0; guard < 500 && seen.size() < blueprint.volume(); guard++) {
            if (plan.nextTarget(probe, colony(), site)) {
                seen.add(target(plan));
                plan.markProgress();
                plan.skipCell();
            }
        }
        assertEquals(blueprint.volume(), seen.size());
    }

    @Test
    void nonCubesWaitForTheDetailPass() {
        Blueprint blueprint = Blueprint.empty("mixed", 2, 1, 1);
        int cube = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        int slab = blueprint.getPalette()
            .cellFor(Blocks.oak_stairs, 0);
        blueprint.setCell(0, 0, 0, cube);
        blueprint.setCell(1, 0, 0, slab);

        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, blueprint);
        plan.start(site);
        FakeProbe probe = new FakeProbe().slab(slab);
        plan.nextTarget(probe, colony(), site);

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(site.getX(), plan.getTargetX());
        probe.occupied(plan.getTargetX(), plan.getTargetY(), plan.getTargetZ());
        plan.markProgress();
        plan.skipCell();

        assertFalse(plan.nextTarget(probe, colony(), site), "the slab must wait for the detail pass");

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(site.getX() + 1, plan.getTargetX());
    }

    @Test
    void aMissingMaterialSkipsEveryCellThatNeedsIt() {
        Blueprint blueprint = Blueprint.empty("wall", 3, 1, 1);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        for (int x = 0; x < 3; x++) {
            blueprint.setCell(x, 0, 0, cell);
        }

        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, blueprint);
        plan.start(site);
        FakeProbe probe = new FakeProbe();
        plan.nextTarget(probe, colony(), site);

        assertTrue(plan.nextTarget(probe, colony(), site));
        plan.materialMissing(site.cellFor(plan.getTargetX(), plan.getTargetY(), plan.getTargetZ()));
        plan.clearTarget();

        assertFalse(plan.nextTarget(probe, colony(), site), "no other cell uses a different material");
    }

    @Test
    void occupiedCellsAreSkipped() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(2, 1, 1));
        plan.start(site);
        FakeProbe probe = new FakeProbe().occupied(site.getX(), site.getY(), site.getZ());
        plan.nextTarget(probe, colony(), site);

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(site.getX() + 1, plan.getTargetX());
    }

    @Test
    void aPassWithNoProgressEndsInStripping() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        plan.start(site);
        FakeProbe probe = new FakeProbe().occupied(0, 64, 0);

        for (int i = 0; i < 10 && plan.getPhase() != BuildPlan.Phase.STRIP; i++) {
            plan.nextTarget(probe, colony(), site);
        }
        assertEquals(BuildPlan.Phase.STRIP, plan.getPhase());
    }

    @Test
    void progressBuysAnotherRound() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        plan.start(site);
        FakeProbe probe = new FakeProbe();
        plan.nextTarget(probe, colony(), site);

        assertTrue(plan.nextTarget(probe, colony(), site));
        plan.markProgress();
        plan.skipCell();

        plan.nextTarget(probe, colony(), site);
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase());
        plan.nextTarget(probe, colony(), site);
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase(), "progress must not end the build");
    }

    @Test
    void strippingProducesNoMoreTargets() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        plan.start(site);
        FakeProbe probe = new FakeProbe().occupied(0, 64, 0);
        for (int i = 0; i < 10; i++) {
            plan.nextTarget(probe, colony(), site);
        }

        assertEquals(BuildPlan.Phase.STRIP, plan.getPhase());
        assertFalse(plan.nextTarget(new FakeProbe(), colony(), site));
        assertFalse(plan.hasTarget());
    }

    @Test
    void clearTargetDropsTheTargetWithoutAdvancing() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        plan.start(site);
        FakeProbe probe = new FakeProbe().dirty(0, 64, 0);

        assertTrue(plan.nextTarget(probe, colony(), site));
        plan.clearTarget();
        assertFalse(plan.hasTarget());

        assertTrue(plan.nextTarget(probe, colony(), site));
        assertEquals(64, plan.getTargetY());
    }

    @Test
    void startResetsAnInFlightPlan() {
        BuildPlan plan = new BuildPlan();
        BuildSite site = siteAt(0, 64, 0, filled(1, 1, 1));
        plan.start(site);
        plan.nextTarget(new FakeProbe(), colony(), site);
        assertEquals(BuildPlan.Phase.BUILD, plan.getPhase());

        plan.start(site);
        assertEquals(BuildPlan.Phase.CLEAR, plan.getPhase());
        assertFalse(plan.hasTarget());
    }
}
