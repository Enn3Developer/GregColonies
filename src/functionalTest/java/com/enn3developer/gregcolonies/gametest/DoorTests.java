package com.enn3developer.gregcolonies.gametest;

import net.minecraft.block.BlockDoor;
import net.minecraft.init.Blocks;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class DoorTests {

    private static final int WALL_X = 3;

    private static final int GAP_Z = 3;

    private static final int DOOR_TOP_META = 8;

    private static final TestPos SPAWN = ColonyFixture.at(1, 1, GAP_Z);

    private static final TestPos BEYOND = ColonyFixture.at(5, 1, GAP_Z);

    private static final double REACH_SQ = 4.0D;

    private DoorTests() {}

    /** A wall the citizen cannot climb or walk around, with a single closed door in it. */
    private static void wallWithADoor(GameTestHelper helper) {
        for (int z = 0; z <= ColonyFixture.SIDE; z++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(WALL_X, y, z, z == GAP_Z ? Blocks.air : Blocks.stone);
            }
        }
        helper.setBlock(WALL_X, 1, GAP_Z, Blocks.wooden_door, 0);
        helper.setBlock(WALL_X, 2, GAP_Z, Blocks.wooden_door, DOOR_TOP_META);
    }

    @GameTest(timeoutTicks = 900)
    public static void aCitizenOpensAClosedDoorOnItsWay(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        wallWithADoor(helper);

        EntityCitizen citizen = ColonyFixture.citizen(helper, colony, SPAWN);
        TestPos target = helper.absolute(BEYOND);
        TestPos door = helper.absolute(ColonyFixture.at(WALL_X, 1, GAP_Z));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, citizen), "not registered yet"))
            .thenExecute("the door starts shut and the citizen on this side of it", () -> {
                helper.assertFalse(
                    ((BlockDoor) Blocks.wooden_door).func_150015_f(helper.getWorld(), door.x(), door.y(), door.z()),
                    "the door was already open");
                helper.assertTrue(citizen.posX < door.x(), "the citizen spawned on the wrong side of the wall");
                ColonyFixture.order(helper, colony, new CitizenCommandMoveTo(target.x(), target.y(), target.z()));
            })
            .thenWaitUntil(
                "it lets itself through",
                600,
                () -> helper.assertTrue(
                    citizen.getDistanceSq(target.x() + 0.5D, target.y(), target.z() + 0.5D) <= REACH_SQ,
                    "the citizen never got past the door"))
            .thenSucceed();
    }
}
