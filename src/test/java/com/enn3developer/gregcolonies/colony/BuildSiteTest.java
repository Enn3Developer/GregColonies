package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

import gregtech.common.GTMockWorld;

class BuildSiteTest {

    private GTMockWorld world;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new GTMockWorld();
    }

    private static Blueprint pillar() {
        Blueprint blueprint = Blueprint.empty("pillar", 1, 3, 1);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        for (int y = 0; y < 3; y++) {
            blueprint.setCell(0, y, 0, cell);
        }
        return blueprint;
    }

    private static Blueprint slab3x3() {
        Blueprint blueprint = Blueprint.empty("slab", 3, 1, 3);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.planks, 0);
        for (int z = 0; z < 3; z++) {
            for (int x = 0; x < 3; x++) {
                blueprint.setCell(x, 0, z, cell);
            }
        }
        return blueprint;
    }

    @Test
    void theAnchorIsRememberedAndTheCornerIsDerivedFromTheOrigin() {
        BuildSite site = new BuildSite(10, 64, 20, slab3x3(), 0, false);
        assertTrue(site.isAt(10, 64, 20));
        assertEquals(10, site.getAnchorX());
        assertEquals(64, site.getAnchorY());
        assertEquals(20, site.getAnchorZ());
        assertEquals(9, site.getX());
        assertEquals(64, site.getY());
        assertEquals(19, site.getZ());
    }

    @Test
    void rotationIsNormalised() {
        assertEquals(3, new BuildSite(0, 0, 0, slab3x3(), -1, false).getRotation());
        assertEquals(1, new BuildSite(0, 0, 0, slab3x3(), 5, false).getRotation());
        assertTrue(new BuildSite(0, 0, 0, slab3x3(), 0, true).isMirrored());
    }

    @Test
    void containsCoversExactlyTheFootprint() {
        BuildSite site = new BuildSite(10, 64, 20, slab3x3(), 0, false);
        assertTrue(site.contains(9, 64, 19));
        assertTrue(site.contains(11, 64, 21));
        assertFalse(site.contains(8, 64, 20));
        assertFalse(site.contains(10, 65, 20));
        assertFalse(site.contains(10, 64, 22));
    }

    @Test
    void cellForMapsWorldCoordinatesBackToTheBlueprint() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        assertNotEquals(Blueprint.AIR, site.cellFor(10, 64, 20));
        assertNotEquals(Blueprint.AIR, site.cellFor(10, 66, 20));
        assertEquals(Blueprint.AIR, site.cellFor(10, 67, 20));
        assertEquals(Blueprint.AIR, site.cellFor(11, 64, 20));
    }

    @Test
    void totalIsTheBlueprintBlockCount() {
        assertEquals(3, new BuildSite(0, 0, 0, pillar(), 0, false).total());
        assertEquals(9, new BuildSite(0, 0, 0, slab3x3(), 0, false).total());
    }

    @Test
    void nothingIsPlacedInAnEmptyWorld() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        assertEquals(3, site.remaining(world));
        assertFalse(site.isPlaced(world, 10, 64, 20));
    }

    @Test
    void cellsOutsideTheBlueprintCountAsAlreadyPlaced() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        assertTrue(site.isPlaced(world, 10, 67, 20));
        assertTrue(site.isPlaced(world, 99, 99, 99));
    }

    @Test
    void airCellsInsideTheBlueprintCountAsAlreadyPlaced() {
        Blueprint blueprint = Blueprint.empty("gap", 1, 3, 1);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        blueprint.setCell(0, 0, 0, cell);
        blueprint.setCell(0, 2, 0, cell);

        BuildSite site = new BuildSite(10, 64, 20, blueprint, 0, false);
        assertEquals(Blueprint.AIR, site.cellFor(10, 65, 20));
        assertTrue(site.isPlaced(world, 10, 65, 20));
        assertEquals(2, site.remaining(world));
    }

    @Test
    void placingTheRightBlockCountsAsProgress() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        world.setBlock(10, 64, 20, Blocks.stone, 0, 2);
        assertTrue(site.isPlaced(world, 10, 64, 20));
        assertEquals(2, site.remaining(world));
    }

    @Test
    void theWrongBlockDoesNotCount() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        world.setBlock(10, 64, 20, Blocks.dirt, 0, 2);
        assertFalse(site.isPlaced(world, 10, 64, 20));
        assertEquals(3, site.remaining(world));
    }

    @Test
    void theWrongMetaDoesNotCount() {
        Blueprint blueprint = Blueprint.empty("wood", 1, 1, 1);
        blueprint.setCell(
            0,
            0,
            0,
            blueprint.getPalette()
                .cellFor(Blocks.planks, 2));
        BuildSite site = new BuildSite(0, 64, 0, blueprint, 0, false);

        world.setBlock(0, 64, 0, Blocks.planks, 0, 2);
        assertFalse(site.isPlaced(world, 0, 64, 0));
        world.setBlock(0, 64, 0, Blocks.planks, 2, 2);
        assertTrue(site.isPlaced(world, 0, 64, 0));
    }

    @Test
    void remainingFallsToZeroWhenTheBuildIsDone() {
        BuildSite site = new BuildSite(10, 64, 20, pillar(), 0, false);
        for (int y = 64; y < 67; y++) {
            world.setBlock(10, y, 20, Blocks.stone, 0, 2);
        }
        assertEquals(0, site.remaining(world));
    }

    @Test
    void airAndReplaceableBlocksAreFree() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        assertTrue(site.isFree(world, 0, 64, 0));

        world.setBlock(0, 64, 0, Blocks.tallgrass, 1, 2);
        assertTrue(site.isFree(world, 0, 64, 0));
    }

    @Test
    void solidBlocksAreNotFree() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        world.setBlock(0, 64, 0, Blocks.dirt, 0, 2);
        assertFalse(site.isFree(world, 0, 64, 0));
    }

    @Test
    void needsStackWantsAMatchingItemOnAFreeUnbuiltCell() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        assertTrue(site.needsStack(world, new ItemStack(Blocks.stone), 0, 64, 0));
    }

    @Test
    void needsStackRejectsTheWrongItem() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        assertFalse(site.needsStack(world, new ItemStack(Blocks.dirt), 0, 64, 0));
    }

    @Test
    void needsStackRejectsCellsOutsideTheBlueprint() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        assertFalse(site.needsStack(world, new ItemStack(Blocks.stone), 5, 64, 0));
    }

    @Test
    void needsStackRejectsCellsThatAreAlreadyBuilt() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        world.setBlock(0, 64, 0, Blocks.stone, 0, 2);
        assertFalse(site.needsStack(world, new ItemStack(Blocks.stone), 0, 64, 0));
    }

    @Test
    void needsStackRejectsBlockedCells() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        world.setBlock(0, 64, 0, Blocks.dirt, 0, 2);
        assertFalse(site.needsStack(world, new ItemStack(Blocks.stone), 0, 64, 0));
    }

    @Test
    void scaffoldsStartEmpty() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        assertFalse(site.hasScaffolds());
        assertNull(site.topScaffold());
    }

    @Test
    void scaffoldsAreAddedOnceAndRemoved() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        site.addScaffold(1, 64, 1);
        site.addScaffold(1, 64, 1);
        assertTrue(site.hasScaffolds());
        assertTrue(site.isScaffoldAt(1, 64, 1));

        site.removeScaffold(1, 64, 1);
        assertFalse(site.hasScaffolds());
    }

    @Test
    void removingAnUnknownScaffoldIsHarmless() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        site.addScaffold(1, 64, 1);
        site.removeScaffold(9, 9, 9);
        assertTrue(site.isScaffoldAt(1, 64, 1));
    }

    @Test
    void topScaffoldIsTheHighestOne() {
        BuildSite site = new BuildSite(0, 64, 0, pillar(), 0, false);
        site.addScaffold(0, 64, 0);
        site.addScaffold(0, 70, 0);
        site.addScaffold(0, 67, 0);
        assertEquals(70, site.topScaffold()[1]);
    }

    @Test
    void nbtRoundTrips() {
        BuildSite site = new BuildSite(10, 64, 20, slab3x3(), 1, true);
        site.addScaffold(1, 65, 2);
        site.addScaffold(3, 66, 4);

        NBTTagCompound tag = site.writeToNBT();
        BuildSite read = BuildSite.readFromNBT(tag);

        assertNotNull(read);
        assertEquals(site.getX(), read.getX());
        assertEquals(site.getY(), read.getY());
        assertEquals(site.getZ(), read.getZ());
        assertEquals(site.getAnchorX(), read.getAnchorX());
        assertEquals(site.getAnchorY(), read.getAnchorY());
        assertEquals(site.getAnchorZ(), read.getAnchorZ());
        assertEquals(site.getRotation(), read.getRotation());
        assertEquals(site.isMirrored(), read.isMirrored());
        assertEquals(site.total(), read.total());
        assertTrue(read.isScaffoldAt(1, 65, 2));
        assertTrue(read.isScaffoldAt(3, 66, 4));
        assertEquals(66, read.topScaffold()[1]);
    }

    @Test
    void nbtFromBeforeAnchorsFallsBackToTheCorner() {
        BuildSite site = new BuildSite(10, 64, 20, slab3x3(), 0, false);
        NBTTagCompound tag = site.writeToNBT();
        tag.removeTag("ax");
        tag.removeTag("ay");
        tag.removeTag("az");

        BuildSite read = BuildSite.readFromNBT(tag);
        assertEquals(read.getX(), read.getAnchorX());
        assertEquals(read.getZ(), read.getAnchorZ());
    }

    @Test
    void nbtWithABrokenBlueprintIsRejected() {
        BuildSite site = new BuildSite(0, 0, 0, pillar(), 0, false);
        NBTTagCompound tag = site.writeToNBT();
        tag.getCompoundTag("blueprint")
            .setInteger("sy", 99);
        assertNull(BuildSite.readFromNBT(tag));
    }

    @Test
    void rotatingTheSiteRotatesTheFootprint() {
        Blueprint blueprint = Blueprint.empty("bar", 3, 1, 1);
        int cell = blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
        for (int x = 0; x < 3; x++) {
            blueprint.setCell(x, 0, 0, cell);
        }

        BuildSite straight = new BuildSite(0, 64, 0, blueprint, 0, false);
        BuildSite turned = new BuildSite(0, 64, 0, blueprint, 1, false);

        assertEquals(
            3,
            straight.getBlueprint()
                .getSizeX());
        assertEquals(
            1,
            turned.getBlueprint()
                .getSizeX());
        assertEquals(
            3,
            turned.getBlueprint()
                .getSizeZ());
        assertEquals(straight.total(), turned.total());
    }
}
