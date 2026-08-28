package com.enn3developer.gregcolonies.gametest;

import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Items;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandGuard;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class GuardTests {

    private static final TestPos SPAWN = ColonyFixture.SPAWN;

    private static final TestPos LAIR = ColonyFixture.at(5, 1, 5);

    private static final double FLEE_DISTANCE_SQ = 18.0D * 18.0D;

    private GuardTests() {}

    private static EntityCitizen guard(GameTestHelper helper, Colony colony) {
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        ColonyFixture.giveTool(citizen, Items.iron_sword);
        return citizen;
    }

    private static EntitySpider spider(GameTestHelper helper) {
        return helper.spawnEntity(new EntitySpider(helper.getWorld()), LAIR.x() + 0.5D, LAIR.y(), LAIR.z() + 0.5D);
    }

    @GameTest(timeoutTicks = 1200)
    public static void aGuardStrikesAHostileInsideTheColony(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = guard(helper, colony);
        EntitySpider[] hostile = new EntitySpider[1];

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute(
                "a guard order is queued",
                () -> ColonyFixture.order(helper, colony, new CitizenCommandGuard()))
            .thenWaitUntil(
                "the citizen takes the watch",
                120,
                () -> helper.assertNotNull(
                    citizen.getCommands()
                        .getCurrent(),
                    "the guard order was never claimed"))
            .thenExecute("a hostile wanders in", () -> hostile[0] = spider(helper))
            .thenWaitUntil(
                "the guard draws blood",
                600,
                () -> helper
                    .assertTrue(hostile[0].getHealth() < hostile[0].getMaxHealth(), "the guard never landed a hit"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 800)
    public static void aGuardStandsItsGroundInsteadOfFleeing(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = guard(helper, colony);
        EntitySpider[] hostile = new EntitySpider[1];

        helper.startSequence()
            .thenExecute(
                "an unordered citizen is afraid of mobs",
                () -> helper.assertTrue(citizen.isAfraid(), "a citizen with no orders should fear mobs"))
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute(
                "a guard order is queued",
                () -> ColonyFixture.order(helper, colony, new CitizenCommandGuard()))
            .thenWaitUntil(
                "the citizen takes the watch",
                120,
                () -> helper.assertFalse(citizen.isAfraid(), "a guard on duty must not fear mobs"))
            .thenExecute("a hostile wanders in", () -> hostile[0] = spider(helper))
            .thenIdle(200)
            .thenExecute("and the guard closed in rather than running", () -> {
                helper.assertFalse(citizen.isAfraid(), "the guard lost its nerve");
                helper.assertTrue(
                    citizen.getDistanceSqToEntity(hostile[0]) < FLEE_DISTANCE_SQ,
                    "the guard ran away from what it was posted against");
            })
            .thenSucceed();
    }
}
