package com.enn3developer.gregcolonies.gametest;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder(GregColoniesTestMod.MODID)
public class BuilderTests {

    private static final TestPos CHEST = ColonyFixture.CHEST;

    private static final TestPos TARGET = ColonyFixture.TARGET;

    private static final TestPos SPAWN = ColonyFixture.SPAWN;

    private BuilderTests() {}

    private static EntityCitizen builderFor(GameTestHelper helper, Colony colony) {
        return ColonyFixture.citizen(helper, colony, SPAWN);
    }

    @GameTest(timeoutTicks = 2000)
    public static void aBuilderPlacesTheBlueprintAndSpendsTheMaterials(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.dirt, 0), TARGET);

        EntityCitizen builder = builderFor(helper, colony);

        helper.startSequence()
            .thenExecute(
                "the site starts unbuilt",
                () -> helper.assertBlockPresent(Blocks.air, TARGET.x(), TARGET.y(), TARGET.z()))
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenWaitUntil(
                "the builder places the blueprint block",
                1600,
                () -> helper.assertBlockPresent(Blocks.dirt, TARGET.x(), TARGET.y(), TARGET.z()))
            .thenExecute("exactly one dirt was spent, and the rest is accounted for", () -> {
                int inChest = ColonyFixture.countIn(helper, CHEST, Blocks.dirt);
                int carried = ColonyFixture.carried(builder, Blocks.dirt);
                helper.assertTrue(inChest < 16, "the builder never drew from the materials chest");
                helper.assertEquals(
                    15,
                    inChest + carried,
                    "one dirt should have been spent placing the block, the rest still chest or carried");
                BuildSite site = colony.getBuildSite();
                helper.assertNotNull(site, "the build site vanished");
                helper.assertEquals(0, site.remaining(helper.getWorld()), "the build should be finished");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1400)
    public static void aBuilderWithoutTheRightMaterialPlacesNothing(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.cobblestone, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.gold_block, 0), TARGET);

        EntityCitizen builder = builderFor(helper, colony);

        helper.onEachTick(
            "nothing is ever placed without the right material",
            () -> helper.assertBlockAbsent(Blocks.gold_block, TARGET.x(), TARGET.y(), TARGET.z()));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenIdle(900)
            .thenExecute("the chest was not raided either", () -> {
                helper.assertEquals(
                    16,
                    ColonyFixture.countIn(helper, CHEST, Blocks.cobblestone),
                    "the wrong material must stay in the chest");
                helper.assertEquals(
                    1,
                    colony.getBuildSite()
                        .remaining(helper.getWorld()),
                    "the build must still be outstanding");
            })
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 2000)
    public static void theBuilderClearsTheSiteBeforeBuilding(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        helper.setBlock(TARGET, Blocks.cobblestone);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.dirt, 0), TARGET);

        EntityCitizen builder = builderFor(helper, colony);
        ColonyFixture.givePickaxe(builder);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenWaitUntil(
                "the cobblestone in the way is removed",
                1200,
                () -> helper.assertBlockAbsent(Blocks.cobblestone, TARGET.x(), TARGET.y(), TARGET.z()))
            .thenWaitUntil(
                "the blueprint block goes up in its place",
                600,
                () -> helper.assertBlockPresent(Blocks.dirt, TARGET.x(), TARGET.y(), TARGET.z()))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1400)
    public static void theColonyCentreIsNeverClearedByABuild(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        TestPos centre = ColonyFixture.CENTRE;
        helper.setBlock(centre, Blocks.gold_block);

        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.dirt, 0), centre);

        EntityCitizen builder = builderFor(helper, colony);

        helper.onEachTick(
            "the colony centre survives",
            () -> helper.assertBlockPresent(Blocks.gold_block, centre.x(), centre.y(), centre.z()));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenIdle(900)
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1400)
    public static void theMaterialsChestIsNeverClearedByABuild(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.dirt, 0), CHEST);

        EntityCitizen builder = builderFor(helper, colony);

        helper.onEachTick(
            "the materials chest survives",
            () -> helper.assertBlockPresent(Blocks.chest, CHEST.x(), CHEST.y(), CHEST.z()));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenIdle(900)
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 2000)
    public static void aBuildSurvivesAnNbtRoundTrip(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);

        Blueprint blueprint = ColonyFixture.singleBlock(Blocks.dirt, 0);
        ColonyFixture.buildAt(helper, colony, blueprint, TARGET);

        BuildSite reloaded = BuildSite.readFromNBT(
            colony.getBuildSite()
                .writeToNBT());
        helper.assertNotNull(reloaded, "the build site did not survive an NBT round trip");
        colony.setBuildSite(reloaded);

        EntityCitizen builder = builderFor(helper, colony);

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, builder), "not registered yet"))
            .thenExecute("it is promoted to builder", () -> ColonyFixture.promoteToBuilder(helper, colony, builder))
            .thenWaitUntil(
                "the reloaded site still builds",
                1600,
                () -> helper.assertBlockPresent(Blocks.dirt, TARGET.x(), TARGET.y(), TARGET.z()))
            .thenSucceed();
    }

    @GameTest(timeoutTicks = 1400)
    public static void aCitizenWithoutTheBuilderJobDoesNotBuild(GameTestHelper helper) {
        Colony colony = ColonyFixture.arena(helper);
        ColonyFixture.chestWith(helper, CHEST, new ItemStack(Blocks.dirt, 16));
        ColonyFixture.materials(helper, colony, CHEST);
        ColonyFixture.buildAt(helper, colony, ColonyFixture.singleBlock(Blocks.dirt, 0), TARGET);

        EntityCitizen idler = builderFor(helper, colony);

        helper.onEachTick(
            "no block is placed by a citizen with no job",
            () -> helper.assertBlockAbsent(Blocks.dirt, TARGET.x(), TARGET.y(), TARGET.z()));

        helper.startSequence()
            .thenWaitUntil(
                "the citizen registers",
                200,
                () -> helper.assertTrue(ColonyFixture.isRegistered(colony, idler), "not registered yet"))
            .thenIdle(900)
            .thenExecute("the materials are untouched", () -> {
                helper.assertEquals(
                    16,
                    ColonyFixture.countIn(helper, CHEST, Blocks.dirt),
                    "only a builder may spend colony materials");
            })
            .thenSucceed();
    }
}
