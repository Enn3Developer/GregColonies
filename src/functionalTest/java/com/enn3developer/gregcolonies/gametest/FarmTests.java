package com.enn3developer.gregcolonies.gametest;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandFarm;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class FarmTests {

    private static final TestPos SPAWN = ColonyFixture.SPAWN;

    private static final TestPos CHEST = ColonyFixture.CHEST;

    private static final TestPos SOIL = ColonyFixture.at(5, 0, 5);

    private static final TestPos PLOT = ColonyFixture.at(5, 1, 5);

    private static final TestPos FIELD_MIN = ColonyFixture.at(3, 0, 3);

    private static final TestPos FIELD_MAX = ColonyFixture.at(6, 2, 6);

    private static final int RIPE = 7;

    private FarmTests() {}

    private static CitizenCommandFarm farmOrder(GameTestHelper helper) {
        TestPos min = helper.absolute(FIELD_MIN);
        TestPos max = helper.absolute(FIELD_MAX);
        return new CitizenCommandFarm(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    private static Colony field(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.disableRandomTicks();
        helper.setBlock(SOIL, Blocks.farmland, RIPE);
        helper.setBlock(PLOT, Blocks.wheat, RIPE);
        return colony;
    }

    private static EntityCitizen farmer(GameTestHelper helper, Colony colony) {
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        citizen.getInventory()
            .store(new ItemStack(Items.wheat_seeds, 16));
        return citizen;
    }

    private static int carriedSeeds(EntityCitizen citizen) {
        return citizen.getInventory()
            .countMain(stack -> stack.getItem() == Items.wheat_seeds);
    }

    @GameTest(timeoutTicks = 1600)
    public static void aFarmOrderReapsRipeWheatAndSowsThePlotAgain(GameTestHelper helper) {
        Colony colony = field(helper);
        EntityCitizen citizen = farmer(helper, colony);

        helper.startSequence()
            .thenExecute("the plot starts ripe", () -> helper.assertBlockPresent(Blocks.wheat, RIPE, PLOT))
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a farm order is queued", () -> ColonyFixture.order(helper, colony, farmOrder(helper)))
            .thenWaitUntil(
                "the plot is reaped and sown again",
                1000,
                () -> helper.assertBlockPresent(Blocks.wheat, 0, PLOT))
            .thenExecute("and the wheat went into the farmer's pack", () -> {
                helper.assertTrue(
                    citizen.getInventory()
                        .countMain(stack -> stack.getItem() == Items.wheat) > 0,
                    "the harvested wheat is nowhere to be found");
                helper.assertBlockPresent(Blocks.farmland, SOIL);
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 2000)
    public static void aFarmerBanksTheHarvestAndKeepsItsSeeds(GameTestHelper helper) {
        Colony colony = field(helper);
        ColonyFixture.chest(helper, CHEST);
        ColonyFixture.site(helper, colony, ColonySiteKind.DROP_OFF, CHEST);

        EntityCitizen citizen = farmer(helper, colony);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a farm order is queued", () -> ColonyFixture.order(helper, colony, farmOrder(helper)))
            .thenWaitUntil(
                "the wheat reaches the drop-off chest",
                1600,
                () -> helper.assertTrue(
                    helper.countItems(CHEST, new ItemStack(Items.wheat)) > 0,
                    "the harvest never reached the drop-off"))
            .thenExecute("but the seeds stay with the farmer", () -> {
                helper.assertTrue(carriedSeeds(citizen) > 0, "a farmer must keep seeds back to sow with");
                helper.assertEquals(
                    0,
                    helper.countItems(CHEST, new ItemStack(Items.wheat_seeds)),
                    "seeds under the reserve must not be banked");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1600)
    public static void aFarmerWithAHoeTillsTheDirtItFinds(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.disableRandomTicks();
        helper.setBlock(SOIL, Blocks.dirt);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        ColonyFixture.giveTool(citizen, Items.iron_hoe);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a farm order is queued", () -> ColonyFixture.order(helper, colony, farmOrder(helper)))
            .thenWaitUntil(
                "the dirt is turned into farmland",
                1000,
                () -> helper.assertBlockPresent(Blocks.farmland, SOIL))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1200)
    public static void aFarmerWithoutAHoeLeavesTheDirtAlone(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        helper.disableRandomTicks();
        helper.setBlock(SOIL, Blocks.dirt);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);

        helper.onEachTick("the dirt is never tilled", () -> helper.assertBlockAbsent(Blocks.farmland, SOIL));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a farm order is queued", () -> ColonyFixture.order(helper, colony, farmOrder(helper)))
            .thenIdle(600)
            .thenExecute("and the dirt is still dirt", () -> helper.assertBlockPresent(Blocks.dirt, SOIL))
            .thenSucceed();
    }
}
