package com.enn3developer.gregcolonies.gametest;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;

/**
 * Every TestPos handed to and returned by this class is test-local, matching GameTestHelper. The
 * colony classes work in world coordinates, so anything crossing that boundary goes through
 * helper.absolute first.
 */
final class ColonyFixture {

    static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000c01d");

    private ColonyFixture() {}

    static TestPos at(int x, int y, int z) {
        return TestPos.at(x, y, z);
    }

    static ColonyRegistry registry(GameTestHelper helper) {
        return ColonyManager.registry(helper.getWorld());
    }

    static Colony colonyAt(GameTestHelper helper, TestPos local) {
        TestPos world = helper.absolute(local);
        return registry(helper).createColony(
            "GameTest",
            OWNER,
            "GameTest",
            helper.getWorld().provider.dimensionId,
            world.x(),
            world.y(),
            world.z());
    }

    /**
     * Tests run in parallel on an eight block grid, so every position a test touches has to stay
     * inside its own cell. Nothing here reaches past SIDE.
     */
    static final int SIDE = 6;

    /** A flat platform with headroom so citizens have somewhere to stand and pathfind. */
    static void floor(GameTestHelper helper) {
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                helper.setBlock(x, 0, z, Blocks.stone);
                for (int clear = 1; clear <= 4; clear++) {
                    helper.setBlock(x, clear, z, Blocks.air);
                }
            }
        }
    }

    /**
     * A WorldServer with no players and no force-loaded chunks stops ticking entities entirely after
     * 1200 ticks, which the void test world hits long before these tests run. Holding the counter
     * down is what a nearby player would do.
     *
     * The other hazard, Horizon-QA's headless setCanSpawnNPCs(false) deleting every INpc each tick,
     * is handled by MixinWorldServer in the mod itself. These tests deliberately run with that flag
     * off so every one of them exercises the fix.
     */
    static void keepEntitiesTicking(GameTestHelper helper) {
        helper.getWorld()
            .resetUpdateEntityTick();
        helper.onEachTick(
            "keep the world ticking entities",
            () -> helper.getWorld()
                .resetUpdateEntityTick());
    }

    /** Everything a colony test needs: a floor, a ticking world and a colony at CENTRE. */
    static Colony arena(GameTestHelper helper) {
        floor(helper);
        keepEntitiesTicking(helper);
        return colonyAt(helper, CENTRE);
    }

    static final TestPos CENTRE = at(0, 1, 0);

    static final TestPos CHEST = at(4, 1, 0);

    static final TestPos TARGET = at(2, 1, 4);

    static final TestPos SPAWN = at(2, 1, 2);

    static void chestWith(GameTestHelper helper, TestPos local, ItemStack stack) {
        helper.setBlock(local, Blocks.chest);
        helper.insertItem(local, stack);
    }

    static int countIn(GameTestHelper helper, TestPos local, Block block) {
        TestPos world = helper.absolute(local);
        IInventory inventory = Inventories.at(helper.getWorld(), world.x(), world.y(), world.z());
        if (inventory == null) {
            return 0;
        }
        return Inventories.count(inventory, stack -> Block.getBlockFromItem(stack.getItem()) == block);
    }

    static Blueprint singleBlock(Block block, int meta) {
        Blueprint blueprint = Blueprint.empty("gametest", 1, 1, 1);
        blueprint.setCell(
            0,
            0,
            0,
            blueprint.getPalette()
                .cellFor(block, meta));
        return blueprint;
    }

    static void materials(GameTestHelper helper, Colony colony, TestPos local) {
        TestPos world = helper.absolute(local);
        colony.site(ColonySiteKind.MATERIALS)
            .set(world.x(), world.y(), world.z());
    }

    /** Point the colony at a one-block build anchored on the given local position. */
    static void buildAt(GameTestHelper helper, Colony colony, Blueprint blueprint, TestPos local) {
        TestPos world = helper.absolute(local);
        colony.addBlueprint(blueprint);
        colony.setBuildSite(new BuildSite(world.x(), world.y(), world.z(), blueprint, 0, false));
    }

    /** Cobblestone needs a real tool, so a builder that has to clear a site carries one. */
    static void givePickaxe(EntityCitizen citizen) {
        citizen.getInventory()
            .getTool()
            .setStackInSlot(0, new ItemStack(Items.iron_pickaxe));
    }

    static int carried(EntityCitizen citizen, Block block) {
        return citizen.getInventory()
            .countMain(stack -> Block.getBlockFromItem(stack.getItem()) == block);
    }

    static EntityCitizen citizen(GameTestHelper helper, Colony colony, TestPos local) {
        EntityCitizen citizen = new EntityCitizen(helper.getWorld());
        citizen.setColonyId(colony.getId());
        return helper.spawnEntity(citizen, local.x() + 0.5D, local.y(), local.z() + 0.5D);
    }

    static boolean isRegistered(Colony colony, EntityCitizen citizen) {
        return colony.getCitizen(citizen.getUniqueID()) != null;
    }

    /**
     * The live entity is the source of truth for its job and writes through to the roster. A roster
     * edit only reaches an entity at its first registration, so promotion goes on the entity.
     */
    static void promoteToBuilder(GameTestHelper helper, Colony colony, EntityCitizen citizen) {
        citizen.setJob(CitizenJob.BUILDER);
    }
}
