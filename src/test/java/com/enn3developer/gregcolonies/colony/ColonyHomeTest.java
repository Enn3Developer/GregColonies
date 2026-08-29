package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class ColonyHomeTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID ANNA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static final UUID BEN = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private static final UUID CARA = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private Colony colony;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @BeforeEach
    void freshColony() {
        colony = Fixtures.colonyWith(
            new Colony(7, "Home", OWNER, "Enn3", 0, 100, 64, 200),
            Fixtures.citizen(ANNA, "Anna", "", 0, 100, 64, 200),
            Fixtures.citizen(BEN, "Ben", "", 0, 100, 64, 200),
            Fixtures.citizen(CARA, "Cara", "", 0, 100, 64, 200));
    }

    private ColonyHome house(int beds) {
        return colony.addHome(new WorkArea(100, 64, 200, 105, 68, 205), beds);
    }

    @Test
    void aHomeGetsAnIdAndKeepsItsRegion() {
        ColonyHome home = house(2);
        assertNotNull(home);
        assertEquals(1, home.getId());
        assertEquals(2, home.getBeds());
        assertTrue(home.contains(102, 66, 202));
        assertFalse(home.contains(106, 66, 202));
        assertSame(home, colony.getHome(home.getId()));
        assertSame(home, colony.getHomeAt(102, 66, 202));
    }

    @Test
    void overlappingRegionsAreRefused() {
        assertNotNull(house(1));
        assertNull(colony.addHome(new WorkArea(105, 68, 205, 110, 70, 210), 1));
        assertNull(colony.addHome(new WorkArea(101, 65, 201, 102, 66, 202), 1));
        assertEquals(
            1,
            colony.getHomes()
                .size());
    }

    @Test
    void aTouchingButSeparateRegionIsAccepted() {
        assertNotNull(house(1));
        assertNotNull(colony.addHome(new WorkArea(106, 64, 200, 110, 68, 205), 1));
        assertEquals(
            2,
            colony.getHomes()
                .size());
    }

    @Test
    void onlyAsManyCitizensAsBedsClaimTheHome() {
        ColonyHome home = house(2);
        assertTrue(colony.claimHome(ANNA, home.getId()));
        assertTrue(colony.claimHome(BEN, home.getId()));
        assertFalse(colony.claimHome(CARA, home.getId()));
        assertEquals(2, colony.homeOccupants(home.getId()));
        assertFalse(
            colony.getCitizen(CARA)
                .hasHome());
    }

    @Test
    void claimingTheSameHomeTwiceCostsNoRoom() {
        ColonyHome home = house(1);
        assertTrue(colony.claimHome(ANNA, home.getId()));
        assertTrue(colony.claimHome(ANNA, home.getId()));
        assertEquals(1, colony.homeOccupants(home.getId()));
        assertFalse(colony.claimHome(BEN, home.getId()));
    }

    @Test
    void aBedInsideAHomeBelongsToItsResidents() {
        ColonyHome home = house(1);
        assertTrue(colony.claimHome(ANNA, home.getId()));
        assertFalse(colony.claimBed(BEN, 102, 65, 202));
        assertTrue(colony.claimBed(ANNA, 102, 65, 202));
        assertTrue(
            colony.getCitizen(ANNA)
                .isBedAt(102, 65, 202));
    }

    @Test
    void aBedOutsideEveryHomeStaysOpenToTheHomeless() {
        ColonyHome home = house(1);
        assertTrue(colony.claimHome(ANNA, home.getId()));
        assertTrue(colony.claimBed(BEN, 120, 64, 220));
        assertFalse(colony.claimBed(ANNA, 121, 64, 220));
    }

    @Test
    void clearingAHomeEvictsItsResidents() {
        ColonyHome home = house(1);
        colony.claimHome(ANNA, home.getId());
        colony.claimBed(ANNA, 102, 65, 202);

        assertTrue(colony.removeHome(home.getId()));
        assertFalse(colony.removeHome(home.getId()));
        assertFalse(
            colony.getCitizen(ANNA)
                .hasHome());
        assertFalse(
            colony.getCitizen(ANNA)
                .hasBed());
    }

    @Test
    void anIdIsNeverHandedOutTwice() {
        ColonyHome first = house(1);
        colony.removeHome(first.getId());
        ColonyHome second = house(1);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void homesAndResidentsSurviveASaveAndLoad() {
        ColonyHome home = house(2);
        colony.claimHome(ANNA, home.getId());

        Colony loaded = Colony.readFromNBT(colony.writeToNBT());
        ColonyHome restored = loaded.getHome(home.getId());
        assertNotNull(restored);
        assertEquals(2, restored.getBeds());
        assertEquals(
            100,
            restored.getArea()
                .getMinX());
        assertEquals(
            205,
            restored.getArea()
                .getMaxZ());
        assertEquals(
            home.getId(),
            loaded.getCitizen(ANNA)
                .getHomeId());
        assertEquals(1, loaded.homeOccupants(home.getId()));
        assertNull(loaded.addHome(new WorkArea(101, 65, 201, 102, 66, 202), 1));
    }

    @Test
    void aReloadedColonyKeepsHandingOutFreshIds() {
        ColonyHome home = house(1);
        Colony loaded = Colony.readFromNBT(colony.writeToNBT());
        ColonyHome next = loaded.addHome(new WorkArea(120, 64, 220, 124, 68, 224), 1);
        assertNotNull(next);
        assertNotEquals(home.getId(), next.getId());
    }

    @Test
    void aBedCountRoundTripsThroughNbt() {
        NBTTagCompound tag = new ColonyHome(3, new WorkArea(1, 2, 3, 4, 5, 6), 9).writeToNBT();
        ColonyHome home = ColonyHome.readFromNBT(tag);
        assertEquals(3, home.getId());
        assertEquals(9, home.getBeds());
        assertEquals("1/2/3 to 4/5/6", home.describe());
    }
}
