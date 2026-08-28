package com.enn3developer.gregcolonies.gametest;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.server.MinecraftServer;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/**
 * With spawn-npcs off, WorldServer deletes every INpc entity each tick. EntityCitizen inherits INpc
 * from EntityVillager, so without MixinWorldServer no colony could exist on such a server.
 */
@GameTestHolder(GregColoniesTestMod.MODID)
public class NpcPurgeTests {

    private NpcPurgeTests() {}

    @GameTest(timeoutTicks = 300)
    public static void theHeadlessRunReallyHasNpcSpawningOff(GameTestHelper helper) {
        helper.assertFalse(
            MinecraftServer.getServer()
                .getCanSpawnNPCs(),
            "these tests only prove anything while spawn-npcs is off");
        helper.succeed();
    }

    @GameTest(timeoutTicks = 400)
    public static void aCitizenSurvivesWithNpcSpawningOff(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.onEachTick(
            "the citizen is never purged",
            () -> helper.assertFalse(citizen.isDead, "a colony citizen was deleted by the spawn-npcs purge"));

        helper.startSequence()
            .thenWaitUntil(
                "and it still gets to register",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenIdle(120)
            .thenExecute(
                "still alive after a while",
                () -> helper.assertTrue(citizen.isEntityAlive(), "the citizen died later on"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 300)
    public static void aPlainVillagerIsStillPurged(GameTestHelper helper) {
        ColonyFixture.arena(helper);

        EntityVillager villager = helper.spawnEntity(
            new EntityVillager(helper.getWorld()),
            ColonyFixture.SPAWN.x() + 0.5D,
            ColonyFixture.SPAWN.y(),
            ColonyFixture.SPAWN.z() + 0.5D);

        helper.startSequence()
            .thenWaitUntil(
                "vanilla NPCs are left to the server owner's setting",
                60,
                () -> helper.assertTrue(villager.isDead, "the exemption must not cover vanilla villagers"))
            .thenSucceed();
    }
}
