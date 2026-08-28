package com.enn3developer.gregcolonies.gametest;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandChop;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class WorkCommandTests {

    private static final TestPos SPAWN = ColonyFixture.SPAWN;

    private static final TestPos CHEST = ColonyFixture.CHEST;

    private static final TestPos TREE = ColonyFixture.at(5, 1, 5);

    private static final TestPos WOOD_MIN = ColonyFixture.at(3, 0, 3);

    private static final TestPos WOOD_MAX = ColonyFixture.at(6, 5, 6);

    private WorkCommandTests() {}

    private static CitizenCommandChop chopOrder(GameTestHelper helper) {
        TestPos min = helper.absolute(WOOD_MIN);
        TestPos max = helper.absolute(WOOD_MAX);
        return new CitizenCommandChop(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    private static EntityCitizen lumberjack(GameTestHelper helper, Colony colony) {
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        ColonyFixture.giveTool(citizen, Items.iron_axe);
        return citizen;
    }

    @GameTest(timeoutTicks = 600)
    public static void aMoveToOrderWalksTheCitizenToTheSpot(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        TestPos target = helper.absolute(ColonyFixture.at(6, 1, 6));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("it starts away from the spot", () -> {
                helper.assertTrue(
                    citizen.getDistanceSq(target.x() + 0.5D, target.y(), target.z() + 0.5D) > 4.0D,
                    "the citizen is already standing on the target");
                ColonyFixture.order(helper, colony, new CitizenCommandMoveTo(target.x(), target.y(), target.z()));
            })
            .thenWaitUntil(
                "the citizen walks over",
                300,
                () -> helper.assertTrue(
                    citizen.getDistanceSq(target.x() + 0.5D, target.y(), target.z() + 0.5D) <= 4.0D,
                    "the citizen never arrived"))
            .thenWaitUntil(
                "and the order is done, not just claimed",
                60,
                () -> helper.assertFalse(
                    citizen.getCommands()
                        .hasWork(),
                    "the move order is still in flight"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 2400)
    public static void aChopOrderFellsTheTreeAndBanksTheLogs(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.tree(helper, TREE);
        ColonyFixture.chest(helper, CHEST);
        ColonyFixture.site(helper, colony, ColonySiteKind.DROP_OFF, CHEST);

        EntityCitizen citizen = lumberjack(helper, colony);

        helper.startSequence()
            .thenExecute(
                "the tree starts whole",
                () -> helper.assertEquals(
                    ColonyFixture.TREE_LOGS,
                    ColonyFixture.standingLogs(helper, TREE),
                    "the tree should start standing"))
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a chop order is queued", () -> ColonyFixture.order(helper, colony, chopOrder(helper)))
            .thenWaitUntil(
                "every log comes down",
                1400,
                () -> helper.assertEquals(0, ColonyFixture.standingLogs(helper, TREE), "logs are still standing"))
            .thenWaitUntil(
                "the logs reach the drop-off chest",
                600,
                () -> helper.assertEquals(
                    ColonyFixture.TREE_LOGS,
                    ColonyFixture.countIn(helper, CHEST, Blocks.log),
                    "the logs never reached the drop-off"))
            .thenExecute(
                "and the citizen kept none of them",
                () -> helper
                    .assertEquals(0, ColonyFixture.carried(citizen, Blocks.log), "the logs should all be banked"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 2400)
    public static void aFelledTreeStaysOnTheCitizenWithoutADropOff(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.tree(helper, TREE);

        EntityCitizen citizen = lumberjack(helper, colony);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a chop order is queued", () -> ColonyFixture.order(helper, colony, chopOrder(helper)))
            .thenWaitUntil(
                "every log comes down",
                1400,
                () -> helper.assertEquals(0, ColonyFixture.standingLogs(helper, TREE), "logs are still standing"))
            .thenExecute(
                "the citizen is carrying them",
                () -> helper.assertEquals(
                    ColonyFixture.TREE_LOGS,
                    ColonyFixture.carried(citizen, Blocks.log),
                    "with nowhere to bank them the logs stay on the citizen"))
            .thenWaitUntil(
                "and the order still finishes",
                600,
                () -> helper.assertFalse(
                    citizen.getCommands()
                        .hasWork(),
                    "the chop order never ended"))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1200)
    public static void aChopOrderWithoutAnAxeLeavesTheTreeStanding(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.tree(helper, TREE);
        ColonyFixture.chest(helper, CHEST);
        ColonyFixture.site(helper, colony, ColonySiteKind.DROP_OFF, CHEST);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);

        helper.onEachTick(
            "no log is ever broken by bare hands",
            () -> helper.assertEquals(
                ColonyFixture.TREE_LOGS,
                ColonyFixture.standingLogs(helper, TREE),
                "a citizen with no axe must not touch the tree"));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("a chop order is queued", () -> ColonyFixture.order(helper, colony, chopOrder(helper)))
            .thenIdle(600)
            .thenExecute(
                "and nothing was harvested",
                () -> helper.assertEquals(
                    0,
                    ColonyFixture.carried(citizen, Blocks.log),
                    "the citizen harvested without a tool"))
            .thenSucceed();
    }
}
