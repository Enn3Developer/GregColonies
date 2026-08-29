package com.enn3developer.gregcolonies.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.init.Blocks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyHome;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

import gregtech.common.GTMockWorld;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class ColonySnapshotTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    private static Colony colony() {
        return new Colony(3, "Home", OWNER, "Enn3", 0, 100, 64, 200);
    }

    @Test
    void aBareColonySnapshots() {
        ColonySnapshot snapshot = ColonySnapshot.of(colony(), new GTMockWorld());

        assertEquals(3, snapshot.getId());
        assertEquals("Home", snapshot.getName());
        assertEquals("Enn3", snapshot.getOwnerName());
        assertEquals(0, snapshot.getDimension());
        assertEquals(100, snapshot.getX());
        assertEquals(64, snapshot.getY());
        assertEquals(200, snapshot.getZ());
        assertFalse(snapshot.hasBuildSite());
        assertFalse(snapshot.hasBlueprint());
        assertTrue(
            snapshot.getCitizens()
                .isEmpty());
    }

    @Test
    void sitesAndBlueprintsAreCarriedOver() {
        Colony colony = colony();
        colony.site(ColonySiteKind.MATERIALS)
            .set(1, 2, 3);
        colony.addBlueprint(Fixtures.cube("tower", 2));
        colony.setPlaceRotation(2);
        colony.setPlaceMirror(true);

        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());

        assertTrue(
            snapshot.site(ColonySiteKind.MATERIALS)
                .isAt(1, 2, 3));
        assertTrue(snapshot.hasBlueprint());
        assertEquals(0, snapshot.getActiveBlueprint());
        assertEquals(2, snapshot.getPlaceRotation());
        assertTrue(snapshot.isPlaceMirror());

        ColonySnapshot.BlueprintEntry entry = snapshot.getBlueprint(0);
        assertEquals("tower", entry.getName());
        assertEquals(2, entry.getSizeX());
        assertEquals(8, entry.getBlocks());
        assertEquals("2x2x2", entry.getSizeLabel());
    }

    @Test
    void buildProgressIsMeasuredAgainstTheWorld() {
        GTMockWorld world = new GTMockWorld();
        Colony colony = colony();
        colony.setBuildSite(new BuildSite(10, 64, 10, Fixtures.cube("hut", 2), 0, false));

        ColonySnapshot before = ColonySnapshot.of(colony, world);
        assertTrue(before.hasBuildSite());
        assertEquals(10, before.getBuildX());
        assertEquals(64, before.getBuildY());
        assertEquals(10, before.getBuildZ());
        assertEquals(8, before.getBuildTotal());
        assertEquals(8, before.getBuildRemaining());

        BuildSite site = colony.getBuildSite();
        world.setBlock(site.getX(), site.getY(), site.getZ(), Blocks.stone, 0, 2);
        assertEquals(
            7,
            ColonySnapshot.of(colony, world)
                .getBuildRemaining());
    }

    @Test
    void citizensAreCarriedOverWhenNoneAreLoaded() {
        UUID id = UUID.randomUUID();
        Colony colony = Fixtures.colonyWith(colony(), Fixtures.citizen(id, "Aeliana", "alpha", 0, 5, 64, 6));

        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());
        assertEquals(
            1,
            snapshot.getCitizens()
                .size());

        CitizenSnapshot citizen = snapshot.getCitizens()
            .get(0);
        assertEquals(id, citizen.getId());
        assertEquals("Aeliana", citizen.getName());
        assertEquals("alpha", citizen.getGroup());
        assertFalse(citizen.isLoaded());
        assertEquals(5.5D, citizen.getX());
        assertEquals(6.5D, citizen.getZ());
    }

    @Test
    void theWireFormatRoundTrips() {
        Colony colony = colony();
        colony.site(ColonySiteKind.DROP_OFF)
            .set(7, 8, 9);
        colony.addBlueprint(Fixtures.cube("tower", 2));
        colony.setBuildSite(new BuildSite(10, 64, 10, Fixtures.single("hut"), 1, true));
        Colony populated = Fixtures
            .colonyWith(colony, Fixtures.citizen(UUID.randomUUID(), "Aeliana", "alpha", 0, 5, 64, 6));

        ColonySnapshot snapshot = ColonySnapshot.of(populated, new GTMockWorld());
        ByteBuf buf = Unpooled.buffer();
        snapshot.write(buf);
        ColonySnapshot read = ColonySnapshot.read(buf);

        assertEquals(0, buf.readableBytes());
        assertEquals(snapshot.getId(), read.getId());
        assertEquals(snapshot.getName(), read.getName());
        assertEquals(snapshot.getOwnerName(), read.getOwnerName());
        assertEquals(snapshot.getRadius(), read.getRadius());
        assertTrue(
            read.site(ColonySiteKind.DROP_OFF)
                .isAt(7, 8, 9));
        assertEquals(
            snapshot.getBlueprints()
                .size(),
            read.getBlueprints()
                .size());
        assertTrue(read.hasBuildSite());
        assertEquals(snapshot.getBuildX(), read.getBuildX());
        assertEquals(snapshot.getBuildY(), read.getBuildY());
        assertEquals(snapshot.getBuildZ(), read.getBuildZ());
        assertEquals(snapshot.getBuildName(), read.getBuildName());
        assertEquals(snapshot.getBuildTotal(), read.getBuildTotal());
        assertEquals(snapshot.getBuildRemaining(), read.getBuildRemaining());
        assertEquals(
            1,
            read.getCitizens()
                .size());
        assertEquals(
            "Aeliana",
            read.getCitizens()
                .get(0)
                .getName());
    }

    @Test
    void theSiteIsMatchedAgainstTheClickedBlockNotTheAnchor() {
        Colony colony = colony();
        colony.setBuildSite(new BuildSite(10, 64, 10, Fixtures.single("hut"), 0, false));
        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());

        assertTrue(snapshot.isBuildSiteAt(10, 63, 10), "the anchor sits one above the block the player clicked");
        assertFalse(snapshot.isBuildSiteAt(10, 64, 10));
        assertFalse(snapshot.isBuildSiteAt(11, 63, 10));
    }

    @Test
    void orderCountsSurviveTheWire() {
        Colony colony = colony();
        colony.enqueueOrder(new com.enn3developer.gregcolonies.testing.TestCommand());
        colony.enqueueOrder(new com.enn3developer.gregcolonies.testing.TestCommand());

        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());
        assertEquals(2, snapshot.getOrderCount());

        ByteBuf buf = Unpooled.buffer();
        snapshot.write(buf);
        assertEquals(
            2,
            ColonySnapshot.read(buf)
                .getOrderCount());
    }

    @Test
    void aBlueprintLabelCarriesItsIndex() {
        Colony colony = colony();
        colony.addBlueprint(Fixtures.cube("tower", 2));

        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());
        assertTrue(
            snapshot.getBlueprint(0)
                .getLabel(0)
                .contains("tower"));
        assertNull(snapshot.getBlueprint(9));
    }

    @Test
    void homesReachTheClientWithTheirBedsAndResidents() {
        UUID id = UUID.randomUUID();
        Colony colony = Fixtures.colonyWith(colony(), Fixtures.citizen(id, "Aeliana", "alpha", 0, 5, 64, 6));
        ColonyHome home = colony.addHome(new WorkArea(0, 64, 0, 4, 68, 4), 2);
        assertTrue(colony.claimHome(id, home.getId()));

        ColonySnapshot snapshot = ColonySnapshot.of(colony, new GTMockWorld());
        ByteBuf buf = Unpooled.buffer();
        snapshot.write(buf);
        ColonySnapshot read = ColonySnapshot.read(buf);

        assertEquals(0, buf.readableBytes());
        assertEquals(
            1,
            read.getHomes()
                .size());

        ColonySnapshot.HomeEntry entry = read.getHome(0);
        assertEquals(home.getId(), entry.getId());
        assertEquals(2, entry.getBeds());
        assertEquals(1, entry.getOccupants());
        assertEquals("1/2 beds", entry.getValue());
        assertEquals(
            "0/64/0 to 4/68/4",
            entry.getArea()
                .describe());
        assertSame(entry, read.homeAt(2, 66, 2));
        assertNull(read.homeAt(5, 66, 2));
    }
}
