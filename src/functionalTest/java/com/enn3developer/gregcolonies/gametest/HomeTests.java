package com.enn3developer.gregcolonies.gametest;

import net.minecraft.init.Blocks;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyHome;
import com.enn3developer.gregcolonies.colony.Homes;
import com.enn3developer.gregcolonies.colony.WorkArea;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class HomeTests {

    private static final TestPos HOME_MIN = ColonyFixture.at(3, 1, 1);

    private static final TestPos HOME_MAX = ColonyFixture.at(6, 3, 5);

    private static final TestPos BED_FOOT = ColonyFixture.at(5, 1, 2);

    private static final TestPos BED_HEAD = ColonyFixture.at(5, 1, 3);

    private static final TestPos SECOND_FOOT = ColonyFixture.at(3, 1, 2);

    private static final TestPos SECOND_HEAD = ColonyFixture.at(3, 1, 3);

    private static final TestPos OUTSIDE_FOOT = ColonyFixture.at(0, 1, 5);

    private static final TestPos OUTSIDE_HEAD = ColonyFixture.at(0, 1, 6);

    private static final TestPos FIRST_SPAWN = ColonyFixture.at(1, 1, 1);

    private static final TestPos SECOND_SPAWN = ColonyFixture.at(1, 1, 3);

    private static final int BED_HEAD_META = 8;

    private static final int NIGHT = 14000;

    private HomeTests() {}

    /** Keeps a citizen parked so idle wandering does not decide these tests. */
    private static final class RestfulOrder extends CitizenCommand {

        @Override
        public String getId() {
            return "gregcolonies_tests:restful_home";
        }

        @Override
        public boolean fearsEnemies() {
            return false;
        }

        @Override
        public CitizenCommandResult update(EntityCitizen citizen) {
            return CitizenCommandResult.RUNNING;
        }
    }

    private static void bed(GameTestHelper helper, TestPos foot, TestPos head) {
        helper.setBlock(foot, Blocks.bed, 0);
        helper.setBlock(head, Blocks.bed, BED_HEAD_META);
    }

    private static WorkArea homeArea(GameTestHelper helper) {
        TestPos min = helper.absolute(HOME_MIN);
        TestPos max = helper.absolute(HOME_MAX);
        return new WorkArea(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    private static ColonyHome house(GameTestHelper helper, Colony colony) {
        WorkArea area = homeArea(helper);
        return ColonyFixture.registry(helper)
            .addHome(colony.getId(), area, Homes.countBeds(helper.getWorld(), area));
    }

    private static EntityCitizen resident(GameTestHelper helper, Colony colony, TestPos spawn) {
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, spawn);
        citizen.getCommands()
            .enqueue(new RestfulOrder());
        return citizen;
    }

    private static ColonyCitizen roster(Colony colony, EntityCitizen citizen) {
        return colony.getCitizen(citizen.getUniqueID());
    }

    @GameTest(timeoutTicks = 300, batch = "night")
    public static void aHomeCountsTheBedsStandingInsideIt(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        bed(helper, BED_FOOT, BED_HEAD);
        bed(helper, SECOND_FOOT, SECOND_HEAD);
        bed(helper, OUTSIDE_FOOT, OUTSIDE_HEAD);

        ColonyHome home = house(helper, colony);
        helper.assertTrue(home != null, "the home was not created");
        helper.assertEquals(2, home.getBeds(), "only the beds inside the region count");
        helper.succeed();
    }

    @GameTest(timeoutTicks = 1600, batch = "night")
    public static void oneBedHousesExactlyOneCitizen(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.fixWorldTime(NIGHT);
        bed(helper, BED_FOOT, BED_HEAD);

        ColonyHome home = house(helper, colony);
        helper.assertEquals(1, home.getBeds(), "the house was built with one bed");

        EntityCitizen first = resident(helper, colony, FIRST_SPAWN);
        EntityCitizen second = resident(helper, colony, SECOND_SPAWN);
        TestPos bed = helper.absolute(BED_HEAD);

        helper.startSequence()
            .thenWaitUntil(
                "both citizens register",
                200,
                () -> helper.assertTrue(
                    ColonyFixture.isRegistered(colony, first) && ColonyFixture.isRegistered(colony, second),
                    "not registered yet"))
            .thenWaitUntil(
                "one of them moves in",
                400,
                () -> helper.assertEquals(1, colony.homeOccupants(home.getId()), "nobody claimed the house"))
            .thenExecuteFor(
                200,
                () -> helper
                    .assertEquals(1, colony.homeOccupants(home.getId()), "a second citizen squeezed into one bed"))
            .thenExecute("and only the resident holds a bed", () -> {
                ColonyCitizen a = roster(colony, first);
                ColonyCitizen b = roster(colony, second);
                helper.assertTrue(a.hasHome() != b.hasHome(), "exactly one citizen should live here");
                ColonyCitizen homeless = a.hasHome() ? b : a;
                helper.assertFalse(
                    homeless.isBedAt(bed.x(), bed.y(), bed.z()),
                    "a citizen without a home took the bed inside the house");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1600, batch = "night")
    public static void aResidentSleepsInItsOwnHouse(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.fixWorldTime(NIGHT);
        bed(helper, BED_FOOT, BED_HEAD);

        ColonyHome home = house(helper, colony);
        EntityCitizen citizen = resident(helper, colony, FIRST_SPAWN);
        TestPos bed = helper.absolute(BED_HEAD);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenWaitUntil(
                "it moves in and lies down",
                900,
                () -> helper.assertTrue(citizen.isAsleep(), "the citizen never lay down"))
            .thenExecute("in the bed of its own house", () -> {
                ColonyCitizen entry = roster(colony, citizen);
                helper.assertEquals(home.getId(), entry.getHomeId(), "the citizen sleeps somewhere it does not live");
                helper.assertTrue(entry.isBedAt(bed.x(), bed.y(), bed.z()), "the wrong bed was claimed");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1600, batch = "night")
    public static void clearingAHomeEvictsItsResident(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.fixWorldTime(NIGHT);
        bed(helper, BED_FOOT, BED_HEAD);

        ColonyHome home = house(helper, colony);
        EntityCitizen citizen = resident(helper, colony, FIRST_SPAWN);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen moves in",
                600,
                () -> helper.assertEquals(1, colony.homeOccupants(home.getId()), "nobody claimed the house"))
            .thenExecute(
                "the player clears the home",
                () -> helper.assertTrue(
                    ColonyFixture.registry(helper)
                        .removeHome(colony.getId(), home.getId()),
                    "the home could not be cleared"))
            .thenExecute("and the citizen is out on the street", () -> {
                ColonyCitizen entry = roster(colony, citizen);
                helper.assertFalse(entry.hasHome(), "the citizen still lives in a home that is gone");
                helper.assertFalse(entry.hasBed(), "the citizen kept a bed it no longer owns");
            })
            .thenSucceed();
    }
}
