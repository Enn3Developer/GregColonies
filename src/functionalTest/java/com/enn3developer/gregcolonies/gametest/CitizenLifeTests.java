package com.enn3developer.gregcolonies.gametest;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandResult;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class CitizenLifeTests {

    private static final TestPos SPAWN = ColonyFixture.SPAWN;

    private static final TestPos CHEST = ColonyFixture.CHEST;

    private static final TestPos BED_FOOT = ColonyFixture.at(5, 1, 2);

    private static final TestPos BED_HEAD = ColonyFixture.at(5, 1, 3);

    private static final int BED_HEAD_META = 8;

    private static final int LARDER = 16;

    private static final int NIGHT = 14000;

    private static final int NIGHT_START = 13000;

    private static final int NIGHT_END = 23000;

    private static final int DAY_LENGTH = 24000;

    private CitizenLifeTests() {}

    private static final class RestfulOrder extends CitizenCommand {

        @Override
        public String getId() {
            return "gregcolonies_tests:restful";
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

    private static boolean isNight(GameTestHelper helper) {
        long time = helper.getWorld()
            .getWorldTime() % DAY_LENGTH;
        return time >= NIGHT_START && time < NIGHT_END;
    }

    @GameTest(timeoutTicks = 900)
    public static void aCitizenWithAnEmptyLarderStocksUpFromThePickUpChest(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Items.bread, LARDER));
        ColonyFixture.site(helper, colony, ColonySiteKind.PICK_UP, CHEST);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        helper.assertEquals(
            0,
            citizen.getInventory()
                .countFood(),
            "a fresh citizen carries no food");

        helper.startSequence()
            .thenWaitUntil(
                "the citizen fetches food from the pick-up chest",
                600,
                () -> helper.assertEquals(
                    LARDER,
                    citizen.getInventory()
                        .countFood(),
                    "the citizen never stocked up"))
            .thenExecute(
                "and the chest paid for it",
                () -> helper.assertEquals(
                    0,
                    helper.countItems(CHEST, new ItemStack(Items.bread)),
                    "the bread was duplicated instead of moved"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 900)
    public static void aHungryCitizenEatsFromItsOwnLarder(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        citizen.getInventory()
            .getFood()
            .setStackInSlot(0, new ItemStack(Items.bread, 4));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute(
                "it starts full",
                () -> helper.assertEquals(
                    20,
                    citizen.getDiet()
                        .getFoodLevel(),
                    "a fresh citizen starts fed"))
            .thenExecuteFor(
                12,
                () -> citizen.getDiet()
                    .addExhaustion(4.5F))
            .thenWaitUntil(
                "the citizen eats one of its loaves",
                300,
                () -> helper.assertEquals(
                    3,
                    citizen.getInventory()
                        .countFood(),
                    "the citizen never ate"))
            .thenExecute(
                "and the meal refilled it",
                () -> helper.assertTrue(
                    citizen.getDiet()
                        .getFoodLevel() > 14,
                    "eating did not raise the food level past the threshold"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1600, batch = "night")
    public static void aCitizenClaimsABedAndSleepsThroughTheNight(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.fixWorldTime(NIGHT);
        helper.setBlock(BED_FOOT, Blocks.bed, 0);
        helper.setBlock(BED_HEAD, Blocks.bed, BED_HEAD_META);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        citizen.getCommands()
            .enqueue(new RestfulOrder());
        TestPos bed = helper.absolute(BED_HEAD);
        helper.assertBlockPresent(Blocks.bed, BED_HEAD);
        helper.assertTrue(isNight(helper), "the sleep tests only say anything at night");
        helper.assertFalse(citizen.isAfraid(), "mobs in a neighbouring test cell must not decide this test");

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute(
                "the colony gives it a bed",
                () -> helper.assertTrue(
                    ColonyFixture.registry(helper)
                        .claimBed(colony.getId(), citizen.getUniqueID(), bed.x(), bed.y(), bed.z()),
                    "the bed could not be claimed"))
            .thenWaitUntil(
                "the citizen goes to bed",
                800,
                () -> helper.assertTrue(citizen.isAsleep(), "the citizen never lay down"))
            .thenExecute("and the colony holds the bed for it", () -> {
                helper.assertTrue(
                    colony.getCitizen(citizen.getUniqueID())
                        .hasBed(),
                    "the bed was never claimed on the roster");
                helper.assertFalse(
                    colony.isBedFree(ColonyFixture.OWNER, bed.x(), bed.y(), bed.z()),
                    "a claimed bed must not be offered to anyone else");
            })
            .thenSucceed();
    }
}
