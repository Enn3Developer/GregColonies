package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class ColonyTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private Colony colony;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @BeforeEach
    void freshColony() {
        colony = new Colony(7, "Home", OWNER, "Enn3", 0, 100, 64, 200);
    }

    @Test
    void carriesItsIdentity() {
        assertEquals(7, colony.getId());
        assertEquals("Home", colony.getName());
        assertEquals(OWNER, colony.getOwner());
        assertEquals("Enn3", colony.getOwnerName());
        assertEquals(0, colony.getDimension());
        assertEquals(100, colony.getX());
        assertEquals(64, colony.getY());
        assertEquals(200, colony.getZ());
    }

    @Test
    void everySiteKindStartsAbsent() {
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            assertNotNull(colony.site(kind));
            assertFalse(
                colony.site(kind)
                    .isPresent());
        }
    }

    @Test
    void isOwnerChecksTheUuid() {
        assertTrue(colony.isOwner(OWNER));
        assertFalse(colony.isOwner(UUID.randomUUID()));
    }

    @Test
    void isCenteredAtNeedsEveryCoordinateAndTheDimension() {
        assertTrue(colony.isCenteredAt(0, 100, 64, 200));
        assertFalse(colony.isCenteredAt(1, 100, 64, 200));
        assertFalse(colony.isCenteredAt(0, 101, 64, 200));
    }

    @Test
    void isInsideUsesTheConfiguredRadius() {
        assertTrue(colony.isInside(0, 100.5D, 200.5D));
        assertTrue(colony.isInside(0, 100.5D + Config.colonyRadius, 200.5D));
        assertFalse(colony.isInside(0, 100.5D + Config.colonyRadius + 1, 200.5D));
        assertFalse(colony.isInside(1, 100.5D, 200.5D));
    }

    @Test
    void distanceIsInfiniteAcrossDimensions() {
        assertEquals(Double.MAX_VALUE, colony.distanceSqTo(1, 100, 200));
        assertEquals(0.0D, colony.distanceSqTo(0, 100, 200));
        assertEquals(25.0D, colony.distanceSqTo(0, 103, 204));
    }

    @Test
    void thereIsNoActiveBlueprintToBeginWith() {
        assertNull(colony.getActiveBlueprint());
        assertEquals(-1, colony.getActiveBlueprintIndex());
        assertNull(colony.getBlueprint(0));
        assertNull(colony.getBlueprint(-1));
    }

    @Test
    void addingABlueprintMakesItActive() {
        assertEquals(0, colony.addBlueprint(Fixtures.single("a")));
        assertEquals(1, colony.addBlueprint(Fixtures.single("b")));
        assertEquals(1, colony.getActiveBlueprintIndex());
        assertEquals(
            "b",
            colony.getActiveBlueprint()
                .getName());
    }

    @Test
    void nullBlueprintsAreRejected() {
        assertEquals(-1, colony.addBlueprint(null));
        assertTrue(
            colony.getBlueprints()
                .isEmpty());
    }

    @Test
    void theLibraryHasAHardLimit() {
        for (int i = 0; i < Colony.MAX_BLUEPRINTS; i++) {
            assertTrue(colony.addBlueprint(Fixtures.single("b" + i)) >= 0);
        }
        assertEquals(-1, colony.addBlueprint(Fixtures.single("one too many")));
        assertEquals(
            Colony.MAX_BLUEPRINTS,
            colony.getBlueprints()
                .size());
    }

    @Test
    void replacingABlueprintMakesItActive() {
        colony.addBlueprint(Fixtures.single("a"));
        colony.addBlueprint(Fixtures.single("b"));

        assertTrue(colony.replaceBlueprint(0, Fixtures.single("c")));
        assertEquals(0, colony.getActiveBlueprintIndex());
        assertEquals(
            "c",
            colony.getActiveBlueprint()
                .getName());
    }

    @Test
    void replacingOutOfRangeOrWithNullFails() {
        colony.addBlueprint(Fixtures.single("a"));
        assertFalse(colony.replaceBlueprint(5, Fixtures.single("c")));
        assertFalse(colony.replaceBlueprint(-1, Fixtures.single("c")));
        assertFalse(colony.replaceBlueprint(0, null));
    }

    @Test
    void removingBelowTheActiveIndexShiftsIt() {
        colony.addBlueprint(Fixtures.single("a"));
        colony.addBlueprint(Fixtures.single("b"));
        colony.addBlueprint(Fixtures.single("c"));
        assertEquals(2, colony.getActiveBlueprintIndex());

        assertTrue(colony.removeBlueprint(0));
        assertEquals(1, colony.getActiveBlueprintIndex());
        assertEquals(
            "c",
            colony.getActiveBlueprint()
                .getName());
    }

    @Test
    void removingAboveTheActiveIndexLeavesItAlone() {
        colony.addBlueprint(Fixtures.single("a"));
        colony.addBlueprint(Fixtures.single("b"));
        colony.setActiveBlueprint(0);

        assertTrue(colony.removeBlueprint(1));
        assertEquals(0, colony.getActiveBlueprintIndex());
    }

    @Test
    void removingTheActiveOneFallsBackToTheNeighbour() {
        colony.addBlueprint(Fixtures.single("a"));
        colony.addBlueprint(Fixtures.single("b"));
        colony.setActiveBlueprint(1);

        assertTrue(colony.removeBlueprint(1));
        assertEquals(0, colony.getActiveBlueprintIndex());
        assertEquals(
            "a",
            colony.getActiveBlueprint()
                .getName());
    }

    @Test
    void removingTheLastBlueprintLeavesNothingActive() {
        colony.addBlueprint(Fixtures.single("a"));
        assertTrue(colony.removeBlueprint(0));
        assertEquals(-1, colony.getActiveBlueprintIndex());
        assertNull(colony.getActiveBlueprint());
    }

    @Test
    void removingOutOfRangeFails() {
        assertFalse(colony.removeBlueprint(0));
        assertFalse(colony.removeBlueprint(-1));
    }

    @Test
    void renamingGoesThroughTheBlueprint() {
        colony.addBlueprint(Fixtures.single("a"));
        assertTrue(colony.renameBlueprint(0, "  keep  "));
        assertEquals(
            "keep",
            colony.getBlueprint(0)
                .getName());
        assertFalse(colony.renameBlueprint(9, "nope"));
    }

    @Test
    void settingTheActiveBlueprintIsBoundsChecked() {
        colony.addBlueprint(Fixtures.single("a"));
        assertTrue(colony.setActiveBlueprint(0));
        assertFalse(colony.setActiveBlueprint(1));
        assertFalse(colony.setActiveBlueprint(-1));
    }

    @Test
    void placementRotationWraps() {
        colony.setPlaceRotation(5);
        assertEquals(1, colony.getPlaceRotation());
        colony.setPlaceRotation(-1);
        assertEquals(3, colony.getPlaceRotation());
        colony.setPlaceRotation(Blueprint.ROTATIONS);
        assertEquals(0, colony.getPlaceRotation());
    }

    @Test
    void placementMirrorIsRemembered() {
        assertFalse(colony.isPlaceMirror());
        colony.setPlaceMirror(true);
        assertTrue(colony.isPlaceMirror());
    }

    @Test
    void settingABuildSiteDropsTheClaim() {
        UUID builder = UUID.randomUUID();
        colony.setBuildSite(new BuildSite(0, 64, 0, Fixtures.single("hut"), 0, false));
        assertTrue(colony.claimBuildSite(builder, 0L));

        colony.setBuildSite(new BuildSite(1, 64, 1, Fixtures.single("hut"), 0, false));
        assertTrue(colony.claimBuildSite(UUID.randomUUID(), 0L));
    }

    @Test
    void aClaimLocksOtherBuildersOut() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(colony.claimBuildSite(first, 0L));
        assertFalse(colony.claimBuildSite(second, 50L));
        assertTrue(colony.claimBuildSite(first, 50L), "the holder may renew its own claim");
    }

    @Test
    void aStaleClaimExpires() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(colony.claimBuildSite(first, 0L));
        assertTrue(colony.claimBuildSite(second, 1000L));
    }

    @Test
    void releasingAClaimOnlyWorksForTheHolder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        colony.claimBuildSite(first, 0L);

        colony.releaseBuildSite(second);
        assertFalse(colony.claimBuildSite(second, 1L), "a stranger must not be able to release the claim");

        colony.releaseBuildSite(first);
        assertTrue(colony.claimBuildSite(second, 2L));
    }

    @Test
    void ordersQueueInOrder() {
        colony.enqueueOrder(new TestCommand(1));
        colony.enqueueOrder(new TestCommand(2));
        assertEquals(2, colony.getOrderCount());

        assertEquals(1, ((TestCommand) colony.pollOrder()).getPayload());
        assertEquals(2, ((TestCommand) colony.pollOrder()).getPayload());
        assertNull(colony.pollOrder());
    }

    @Test
    void ordersAreCountedPerGroup() {
        CitizenCommand alpha = new TestCommand();
        alpha.setTargetGroup("alpha");
        CitizenCommand loose = new TestCommand();

        colony.enqueueOrder(alpha);
        colony.enqueueOrder(loose);

        assertEquals(2, colony.getOrderCount());
        assertEquals(1, colony.getOrderCount("alpha"));
        assertEquals(1, colony.getOrderCount(""));
        assertEquals(1, colony.getOrderCount(null));
        assertEquals(0, colony.getOrderCount("beta"));
    }

    @Test
    void clearingOrdersByGroupLeavesTheRest() {
        CitizenCommand alpha = new TestCommand();
        alpha.setTargetGroup("alpha");
        colony.enqueueOrder(alpha);
        colony.enqueueOrder(new TestCommand());

        assertEquals(1, colony.clearOrders("alpha"));
        assertEquals(1, colony.getOrderCount());
        assertEquals(0, colony.clearOrders("alpha"));
    }

    @Test
    void clearingEverythingReturnsTheCount() {
        colony.enqueueOrder(new TestCommand());
        colony.enqueueOrder(new TestCommand());
        assertEquals(2, colony.clearOrders());
        assertEquals(0, colony.getOrderCount());
    }

    @Test
    void aNullTargetGroupBecomesTheEmptyString() {
        CitizenCommand command = new TestCommand();
        command.setTargetGroup(null);
        assertEquals("", command.getTargetGroup());
    }

    @Test
    void citizenBookkeeping() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Colony populated = Fixtures.colonyWith(
            colony,
            Fixtures.citizen(first, "Aeliana", "alpha", 0, 1, 64, 1),
            Fixtures.citizen(second, "Marcus", "", 0, 2, 64, 2));

        assertEquals(2, populated.getCitizenCount());
        assertNotNull(populated.getCitizen(first));
        assertNull(populated.getCitizen(UUID.randomUUID()));
        assertTrue(populated.hasCitizenNamed("aeliana"));
        assertFalse(populated.hasCitizenNamed("nobody"));

        assertTrue(populated.removeCitizen(first));
        assertFalse(populated.removeCitizen(first));
        assertEquals(1, populated.getCitizenCount());
    }

    @Test
    void bedsAreClaimedOncePerColony() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Colony populated = Fixtures.colonyWith(
            colony,
            Fixtures.citizen(first, "Aeliana", "", 0, 1, 64, 1),
            Fixtures.citizen(second, "Marcus", "", 0, 2, 64, 2));

        assertTrue(populated.isBedFree(first, 5, 64, 5));
        assertTrue(populated.claimBed(first, 5, 64, 5));
        assertFalse(populated.isBedFree(second, 5, 64, 5));
        assertFalse(populated.claimBed(second, 5, 64, 5));
        assertTrue(populated.claimBed(second, 6, 64, 5));
    }

    @Test
    void claimingABedForAStrangerFails() {
        assertFalse(colony.claimBed(UUID.randomUUID(), 0, 0, 0));
    }

    @Test
    void releasingABedFreesIt() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Colony populated = Fixtures.colonyWith(
            colony,
            Fixtures.citizen(first, "Aeliana", "", 0, 1, 64, 1),
            Fixtures.citizen(second, "Marcus", "", 0, 2, 64, 2));

        populated.claimBed(first, 5, 64, 5);
        populated.releaseBed(first);
        assertTrue(populated.claimBed(second, 5, 64, 5));
    }

    @Test
    void releasingAnUnknownBedIsHarmless() {
        colony.releaseBed(UUID.randomUUID());
    }

    @Test
    void nbtRoundTrips() {
        colony.site(ColonySiteKind.MATERIALS)
            .set(1, 2, 3);
        colony.addBlueprint(Fixtures.single("hut"));
        colony.addBlueprint(Fixtures.cube("tower", 2));
        colony.setActiveBlueprint(0);
        colony.setPlaceRotation(2);
        colony.setPlaceMirror(true);
        colony.setBuildSite(new BuildSite(9, 64, 9, Fixtures.single("hut"), 1, false));
        CitizenCommand order = new TestCommand(42);
        order.setTargetGroup("alpha");
        colony.enqueueOrder(order);

        UUID citizenId = UUID.randomUUID();
        Colony populated = Fixtures.colonyWith(colony, Fixtures.citizen(citizenId, "Aeliana", "alpha", 0, 1, 64, 1));

        Colony read = Colony.readFromNBT(populated.writeToNBT());

        assertEquals(7, read.getId());
        assertEquals("Home", read.getName());
        assertEquals(OWNER, read.getOwner());
        assertEquals("Enn3", read.getOwnerName());
        assertTrue(
            read.site(ColonySiteKind.MATERIALS)
                .isAt(1, 2, 3));
        assertEquals(
            2,
            read.getBlueprints()
                .size());
        assertEquals(0, read.getActiveBlueprintIndex());
        assertEquals(2, read.getPlaceRotation());
        assertTrue(read.isPlaceMirror());
        assertNotNull(read.getBuildSite());
        assertTrue(
            read.getBuildSite()
                .isAt(9, 64, 9));
        assertEquals(1, read.getOrderCount("alpha"));
        assertEquals(1, read.getCitizenCount());
        assertEquals(
            "Aeliana",
            read.getCitizen(citizenId)
                .getName());
        assertEquals(42, ((TestCommand) read.pollOrder()).getPayload());
    }

    @Test
    void aColonyWithoutABuildSiteRoundTrips() {
        Colony read = Colony.readFromNBT(colony.writeToNBT());
        assertNull(read.getBuildSite());
        assertEquals(-1, read.getActiveBlueprintIndex());
    }

    @Test
    void aLegacySingleBlueprintIsPromotedToTheLibrary() {
        NBTTagCompound tag = colony.writeToNBT();
        tag.setTag(
            "blueprint",
            Fixtures.single("old")
                .writeToNBT());
        tag.removeTag("activeBlueprint");

        Colony read = Colony.readFromNBT(tag);
        assertEquals(
            1,
            read.getBlueprints()
                .size());
        assertEquals(
            "blueprint",
            read.getBlueprint(0)
                .getName());
        assertEquals(0, read.getActiveBlueprintIndex());
    }

    @Test
    void unreadableBlueprintsAreDroppedNotFatal() {
        colony.addBlueprint(Fixtures.single("good"));
        NBTTagCompound tag = colony.writeToNBT();
        tag.getTagList("blueprints", 10)
            .getCompoundTagAt(0)
            .setInteger("sx", 0);

        Colony read = Colony.readFromNBT(tag);
        assertTrue(
            read.getBlueprints()
                .isEmpty());
    }

    @Test
    void toStringMentionsTheIdentity() {
        assertTrue(
            colony.toString()
                .contains("Colony#7"));
        assertTrue(
            colony.toString()
                .contains("Home"));
    }
}
