package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.Fixtures;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;
import com.enn3developer.gregcolonies.testing.TestCommand;

class ColonyManagerTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
        TestCommand.ensureRegistered();
    }

    @Test
    void aFreshManagerHasAnEmptyRegistry() {
        ColonyManager manager = new ColonyManager();
        assertNotNull(manager.getRegistry());
        assertEquals(
            0,
            manager.getRegistry()
                .getColonyCount());
    }

    @Test
    void creatingAColonyMarksTheSaveDataDirty() {
        ColonyManager manager = new ColonyManager();
        assertFalse(manager.isDirty());

        manager.getRegistry()
            .createColony("Home", OWNER, "Enn3", 0, 0, 64, 0);
        assertTrue(manager.isDirty(), "the registry must mark the world save data dirty");
    }

    @Test
    void nbtRoundTripsThroughTheRegistry() {
        ColonyManager manager = new ColonyManager();
        Colony first = manager.getRegistry()
            .createColony("Home", OWNER, "Enn3", 0, 100, 64, 200);
        manager.getRegistry()
            .createColony("Outpost", OWNER, "Enn3", 1, -50, 70, 30);
        first.addBlueprint(Fixtures.single("hut"));
        first.site(ColonySiteKind.MATERIALS)
            .set(1, 2, 3);

        NBTTagCompound tag = new NBTTagCompound();
        manager.writeToNBT(tag);

        ColonyManager read = new ColonyManager();
        read.readFromNBT(tag);

        assertEquals(
            2,
            read.getRegistry()
                .getColonyCount());
        assertEquals(
            3,
            read.getRegistry()
                .getNextId());

        Colony restored = read.getRegistry()
            .getColony(first.getId());
        assertNotNull(restored);
        assertEquals("Home", restored.getName());
        assertTrue(
            restored.site(ColonySiteKind.MATERIALS)
                .isAt(1, 2, 3));
        assertEquals(
            1,
            restored.getBlueprints()
                .size());
    }

    @Test
    void readingReplacesWhateverWasThere() {
        ColonyManager manager = new ColonyManager();
        manager.getRegistry()
            .createColony("Stale", OWNER, "Enn3", 0, 0, 64, 0);

        manager.readFromNBT(new NBTTagCompound());
        assertEquals(
            0,
            manager.getRegistry()
                .getColonyCount());
        assertEquals(
            1,
            manager.getRegistry()
                .getNextId());
    }

    @Test
    void newIdsCarryOnFromWhereTheSaveLeftOff() {
        ColonyManager manager = new ColonyManager();
        manager.getRegistry()
            .createColony("a", OWNER, "Enn3", 0, 0, 64, 0);
        manager.getRegistry()
            .createColony("b", OWNER, "Enn3", 0, 0, 64, 0);

        NBTTagCompound tag = new NBTTagCompound();
        manager.writeToNBT(tag);

        ColonyManager read = new ColonyManager();
        read.readFromNBT(tag);
        assertEquals(
            3,
            read.getRegistry()
                .createColony("c", OWNER, "Enn3", 0, 0, 64, 0)
                .getId());
    }

    @Test
    void theSaveNameIsStable() {
        assertEquals("gregcolonies_colonies", ColonyManager.DATA_NAME);
    }

    @Test
    void mapStorageCanBuildItReflectively() {
        assertDoesNotThrow(() -> ColonyManager.class.getConstructor(String.class));
        assertEquals(
            0,
            new ColonyManager(ColonyManager.DATA_NAME).getRegistry()
                .getColonyCount());
    }
}
