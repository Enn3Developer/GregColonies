package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.entity.CitizenGender;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class ColonyCitizenTest {

    private static final UUID ID = UUID.fromString("d8a80763-8175-47f0-9ec5-a997c82108f4");

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void nbtRoundTrips() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "alpha", 3, 10, 64, -20);
        citizen.setJob(CitizenJob.BUILDER);
        citizen.setBed(1, 2, 3);

        ColonyCitizen read = ColonyCitizen.readFromNBT(citizen.writeToNBT());

        assertEquals(ID, read.getId());
        assertEquals("Aeliana", read.getName());
        assertEquals("alpha", read.getGroup());
        assertEquals(CitizenJob.BUILDER, read.getJob());
        assertEquals(CitizenGender.FEMALE, read.getGender());
        assertFalse(read.isChild());
        assertEquals(3, read.getDimension());
        assertEquals(10, read.getX());
        assertEquals(64, read.getY());
        assertEquals(-20, read.getZ());
        assertTrue(read.isBedAt(1, 2, 3));
    }

    @Test
    void aCitizenWithoutABedRoundTrips() {
        ColonyCitizen read = ColonyCitizen.readFromNBT(
            Fixtures.citizen(ID, "Marcus", "", 0, 0, 64, 0)
                .writeToNBT());
        assertFalse(read.hasBed());
        assertEquals(0, read.getBedX());
    }

    @Test
    void nullNamesAndGroupsBecomeEmptyStrings() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "alpha", 0, 0, 64, 0);
        citizen.setName(null);
        citizen.setGroup(null);
        assertEquals("", citizen.getName());
        assertEquals("", citizen.getGroup());
    }

    @Test
    void aNullJobFallsBackToNone() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "", 0, 0, 64, 0);
        citizen.setJob(null);
        assertEquals(CitizenJob.NONE, citizen.getJob());
    }

    @Test
    void bedsAreClaimedAndCleared() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "", 0, 0, 64, 0);
        assertFalse(citizen.hasBed());
        assertFalse(citizen.isBedAt(0, 0, 0));

        citizen.setBed(5, 64, 5);
        assertTrue(citizen.hasBed());
        assertTrue(citizen.isBedAt(5, 64, 5));
        assertFalse(citizen.isBedAt(5, 64, 6));

        citizen.clearBed();
        assertFalse(citizen.hasBed());
        assertFalse(citizen.isBedAt(5, 64, 5));
        assertEquals(0, citizen.getBedY());
    }

    @Test
    void distanceIsInfiniteAcrossDimensions() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "", 0, 10, 64, 10);
        assertEquals(Double.MAX_VALUE, citizen.distanceSqTo(1, 10.5D, 10.5D));
    }

    @Test
    void distanceIsMeasuredFromTheBlockCentre() {
        ColonyCitizen citizen = Fixtures.citizen(ID, "Aeliana", "", 0, 10, 64, 10);
        assertEquals(0.0D, citizen.distanceSqTo(0, 10.5D, 10.5D));
        assertEquals(2.0D, citizen.distanceSqTo(0, 11.5D, 11.5D));
    }

    @Test
    void distanceIgnoresHeight() {
        ColonyCitizen high = Fixtures.citizen(ID, "Aeliana", "", 0, 0, 200, 0);
        ColonyCitizen low = Fixtures.citizen(ID, "Marcus", "", 0, 0, 5, 0);
        assertEquals(high.distanceSqTo(0, 0.5D, 0.5D), low.distanceSqTo(0, 0.5D, 0.5D));
    }

    @Test
    void anUnknownJobByteDegradesToNone() {
        NBTTagCompound tag = Fixtures.citizen(ID, "Aeliana", "", 0, 0, 64, 0)
            .writeToNBT();
        tag.setByte("job", (byte) 99);
        assertEquals(
            CitizenJob.NONE,
            ColonyCitizen.readFromNBT(tag)
                .getJob());
    }

    @Test
    void anUnknownGenderByteDegradesToNull() {
        NBTTagCompound tag = Fixtures.citizen(ID, "Aeliana", "", 0, 0, 64, 0)
            .writeToNBT();
        tag.setByte("gender", (byte) 0);
        assertNull(
            ColonyCitizen.readFromNBT(tag)
                .getGender());
    }
}
