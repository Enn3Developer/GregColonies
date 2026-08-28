package com.enn3developer.gregcolonies.gametest;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class ColonyRosterTests {

    private ColonyRosterTests() {}

    @GameTest(timeoutTicks = 300)
    public static void aSpawnedCitizenJoinsTheRoster(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers itself with the colony",
                200,
                () -> helper.assertEquals(1, colony.getCitizenCount(), "the roster should hold the spawned citizen"))
            .thenExecute("the roster entry carries the generated name", () -> {
                helper
                    .assertNotNull(colony.getCitizen(citizen.getUniqueID()), "the citizen is missing from the roster");
                helper.assertFalse(
                    colony.getCitizen(citizen.getUniqueID())
                        .getName()
                        .isEmpty(),
                    "a registered citizen must have a name");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 400)
    public static void aCitizenWritesItsGroupAndJobThroughToTheRoster(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("the citizen is given a group and a job", () -> {
                citizen.setGroup("alpha");
                citizen.setJob(CitizenJob.BUILDER);
            })
            .thenExecute("the roster carries both straight away", () -> {
                helper.assertEquals(
                    "alpha",
                    colony.getCitizen(citizen.getUniqueID())
                        .getGroup(),
                    "the group did not reach the roster");
                helper.assertEquals(
                    CitizenJob.BUILDER,
                    colony.getCitizen(citizen.getUniqueID())
                        .getJob(),
                    "the job did not reach the roster");
            })
            .thenIdle(120)
            .thenExecute("and the roster refresh does not undo it", () -> {
                helper.assertEquals(CitizenJob.BUILDER, citizen.getJob(), "the job was lost on a roster refresh");
                helper.assertEquals("alpha", citizen.getGroup(), "the group was lost on a roster refresh");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 400)
    public static void aStoredJobIsAdoptedWhenACitizenFirstRegisters(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen first = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, first), "not registered yet"))
            .thenExecute("it takes a job, and the roster remembers", () -> {
                first.setJob(CitizenJob.BUILDER);
                helper.assertEquals(
                    CitizenJob.BUILDER,
                    colony.getCitizen(first.getUniqueID())
                        .getJob(),
                    "the roster should have the job");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 400)
    public static void aCitizenClaimsAQueuedOrder(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);
        TestPos target = helper.absolute(ColonyFixture.at(5, 1, 5));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute(
                "an order is queued for the colony",
                () -> ColonyFixture.registry(helper)
                    .enqueueOrder(colony.getId(), new CitizenCommandMoveTo(target.x(), target.y(), target.z())))
            .thenWaitUntil(
                "the citizen takes the order off the colony queue",
                150,
                () -> helper.assertEquals(0, colony.getOrderCount(), "the order was never claimed"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 400)
    public static void anOrderForAnotherGroupIsLeftAlone(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);
        TestPos target = helper.absolute(ColonyFixture.at(5, 1, 5));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("an order is queued for a group nobody is in", () -> {
                CitizenCommandMoveTo order = new CitizenCommandMoveTo(target.x(), target.y(), target.z());
                order.setTargetGroup("nobody");
                ColonyFixture.registry(helper)
                    .enqueueOrder(colony.getId(), order);
            })
            .thenIdle(120)
            .thenExecute(
                "the order is still waiting",
                () -> helper.assertEquals(1, colony.getOrderCount(), "an ungrouped citizen took a grouped order"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 300)
    public static void aDeadCitizenLeavesTheRoster(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertEquals(1, colony.getCitizenCount(), "not registered yet"))
            .thenExecute(
                "the citizen dies",
                () -> citizen.attackEntityFrom(net.minecraft.util.DamageSource.outOfWorld, 1000.0F))
            .thenWaitUntil(
                "the roster no longer lists it",
                60,
                () -> helper.assertEquals(0, colony.getCitizenCount(), "a dead citizen must leave the roster"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 300)
    public static void aCitizenIsNamedAndFedWhenItSpawns(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, ColonyFixture.SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("it has a name, a gender and a full belly", () -> {
                helper.assertFalse(
                    citizen.getCitizenName()
                        .isEmpty(),
                    "a citizen must be named");
                helper.assertNotNull(citizen.getGender(), "a citizen must have a gender");
                helper.assertEquals(
                    20,
                    citizen.getDiet()
                        .getFoodLevel(),
                    "a fresh citizen starts fed");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 300)
    public static void twoCitizensBothJoinTheRoster(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.citizen(helper, colony, ColonyFixture.at(2, 1, 2));
        ColonyFixture.citizen(helper, colony, ColonyFixture.at(3, 1, 3));

        helper.startSequence()
            .thenWaitUntil(
                "both citizens register",
                200,
                () -> helper.assertEquals(2, colony.getCitizenCount(), "both citizens should be on the roster"))
            .thenExecute(
                "and they are told apart",
                () -> {
                    helper.assertEquals(
                        2,
                        colony.getCitizens()
                            .size(),
                        "the roster collapsed two citizens into one");
                })
            .thenSucceed();
    }
}
