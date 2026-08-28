package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class BlockPaletteTest {

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    private static BlockPalette palette() {
        BlockPalette palette = new BlockPalette();
        palette.add("");
        return palette;
    }

    @Test
    void airAndNullAreNotBuildable() {
        assertFalse(BlockPalette.isBuildable(null, 0));
        assertFalse(BlockPalette.isBuildable(Blocks.air, 0));
    }

    @Test
    void liquidsAreNotBuildable() {
        assertFalse(BlockPalette.isBuildable(Blocks.water, 0));
        assertFalse(BlockPalette.isBuildable(Blocks.flowing_water, 0));
        assertFalse(BlockPalette.isBuildable(Blocks.lava, 0));
    }

    @Test
    void blocksWithTileEntitiesAreNotBuildable() {
        assertFalse(BlockPalette.isBuildable(Blocks.chest, 0));
        assertFalse(BlockPalette.isBuildable(Blocks.furnace, 0));
    }

    @Test
    void blocksWithoutAnItemAreNotBuildable() {
        assertNull(Item.getItemFromBlock(Blocks.piston_head));
        assertFalse(BlockPalette.isBuildable(Blocks.piston_head, 0));
    }

    @Test
    void plainBlocksAreBuildable() {
        assertTrue(BlockPalette.isBuildable(Blocks.stone, 0));
        assertTrue(BlockPalette.isBuildable(Blocks.planks, 3));
        assertTrue(BlockPalette.isBuildable(Blocks.oak_stairs, 0));
    }

    @Test
    void airCellHasNoBlock() {
        assertNull(palette().blockOf(Blueprint.AIR));
    }

    @Test
    void slotZeroIsReservedAndHasNoBlock() {
        BlockPalette palette = palette();
        assertNull(palette.blockOf(Blueprint.cell(0, 5)));
    }

    @Test
    void slotsPastTheEndHaveNoBlock() {
        assertNull(palette().blockOf(Blueprint.cell(9, 0)));
    }

    @Test
    void unknownRegistryNamesFallBackToAir() {
        BlockPalette palette = palette();
        palette.add("nosuchmod:nosuchblock");
        int cell = Blueprint.cell(1, 0);
        assertEquals(Blocks.air, palette.blockOf(cell));
        assertFalse(palette.isPlaceable(cell));
        assertNull(palette.stackOf(cell));
        assertFalse(palette.matches(cell, new ItemStack(Blocks.stone)));
    }

    @Test
    void cellForRegistersAndReusesASlot() {
        BlockPalette palette = palette();
        int first = palette.cellFor(Blocks.stone, 0);
        int again = palette.cellFor(Blocks.stone, 0);
        assertEquals(first, again);
        assertEquals(2, palette.size());
        assertEquals(1, Blueprint.slotOf(first));
    }

    @Test
    void cellForKeepsTheMeta() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.planks, 3);
        assertEquals(3, Blueprint.metaOf(cell));
        assertEquals(Blocks.planks, palette.blockOf(cell));
    }

    @Test
    void cellForGivesDistinctSlotsToDistinctBlocks() {
        BlockPalette palette = palette();
        int stone = palette.cellFor(Blocks.stone, 0);
        int planks = palette.cellFor(Blocks.planks, 0);
        assertNotEquals(Blueprint.slotOf(stone), Blueprint.slotOf(planks));
        assertEquals(3, palette.size());
    }

    @Test
    void cellForRejectsUnbuildableBlocks() {
        BlockPalette palette = palette();
        assertEquals(Blueprint.AIR, palette.cellFor(Blocks.air, 0));
        assertEquals(Blueprint.AIR, palette.cellFor(Blocks.water, 0));
        assertEquals(Blueprint.AIR, palette.cellFor(Blocks.chest, 0));
        assertEquals(Blueprint.AIR, palette.cellFor((net.minecraft.block.Block) null, 0));
        assertEquals(1, palette.size());
    }

    @Test
    void cellForAStackUsesItsBlockAndDamage() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(new ItemStack(Blocks.planks, 1, 2));
        assertEquals(Blocks.planks, palette.blockOf(cell));
        assertEquals(2, Blueprint.metaOf(cell));
    }

    @Test
    void cellForANullOrNonBlockStackIsAir() {
        BlockPalette palette = palette();
        assertEquals(Blueprint.AIR, palette.cellFor((ItemStack) null));
        assertEquals(Blueprint.AIR, palette.cellFor(new ItemStack(Items.apple)));
    }

    @Test
    void stackOfBuildsThePlaceableItem() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.planks, 2);
        ItemStack stack = palette.stackOf(cell);
        assertNotNull(stack);
        assertEquals(1, stack.stackSize);
        assertEquals(Blocks.planks, net.minecraft.block.Block.getBlockFromItem(stack.getItem()));
        assertEquals(2, stack.getItemDamage());
    }

    @Test
    void stackOfAirIsNull() {
        assertNull(palette().stackOf(Blueprint.AIR));
    }

    @Test
    void stackOfUsesTheDroppedDamageNotTheBlockMeta() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.log, 4);
        assertEquals(4, Blueprint.metaOf(cell));
        assertEquals(
            0,
            palette.stackOf(cell)
                .getItemDamage());
    }

    @Test
    void matchesAcceptsTheRightStack() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.planks, 1);
        assertTrue(palette.matches(cell, new ItemStack(Blocks.planks, 1, 1)));
        assertTrue(palette.matches(cell, new ItemStack(Blocks.planks, 64, 1)));
    }

    @Test
    void matchesRejectsTheWrongBlockMetaOrNull() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.planks, 1);
        assertFalse(palette.matches(cell, new ItemStack(Blocks.planks, 1, 2)));
        assertFalse(palette.matches(cell, new ItemStack(Blocks.stone, 1, 0)));
        assertFalse(palette.matches(cell, new ItemStack(Items.apple)));
        assertFalse(palette.matches(cell, null));
        assertFalse(palette.matches(Blueprint.AIR, new ItemStack(Blocks.stone)));
    }

    @Test
    void matchesUsesTheDroppedDamageForRotatedBlocks() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.log, 8);
        assertTrue(palette.matches(cell, new ItemStack(Blocks.log, 1, 0)));
        assertFalse(palette.matches(cell, new ItemStack(Blocks.log, 1, 8)));
    }

    @Test
    void itemCellFoldsMetaVariantsThatDropTheSameItem() {
        BlockPalette palette = palette();
        int upright = palette.cellFor(Blocks.log, 0);
        int sideways = palette.cellFor(Blocks.log, 4);
        assertNotEquals(upright, sideways);
        assertEquals(palette.itemCell(upright), palette.itemCell(sideways));
    }

    @Test
    void itemCellLeavesUnknownCellsAlone() {
        BlockPalette palette = palette();
        assertEquals(Blueprint.AIR, palette.itemCell(Blueprint.AIR));
    }

    @Test
    void isPlaceableFollowsIsBuildable() {
        BlockPalette palette = palette();
        assertTrue(palette.isPlaceable(palette.cellFor(Blocks.stone, 0)));
        palette.add("nosuchmod:nosuchblock");
        assertFalse(palette.isPlaceable(Blueprint.cell(palette.size() - 1, 0)));
        assertFalse(palette.isPlaceable(Blueprint.AIR));
    }

    @Test
    void rotateLeavesPlainBlocksAlone() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.stone, 0);
        assertEquals(cell, palette.rotate(cell, 1, false));
    }

    @Test
    void rotateTurnsDirectionalBlocks() {
        BlockPalette palette = palette();
        int cell = palette.cellFor(Blocks.ladder, 2);
        int turned = palette.rotate(cell, 1, false);
        assertEquals(Blueprint.slotOf(cell), Blueprint.slotOf(turned));
        assertEquals(5, Blueprint.metaOf(turned));
    }

    @Test
    void rotateLeavesUnknownCellsAlone() {
        assertEquals(Blueprint.AIR, palette().rotate(Blueprint.AIR, 1, false));
    }

    @Test
    void adoptMovesACellBetweenPalettes() {
        BlockPalette source = palette();
        BlockPalette target = palette();
        target.cellFor(Blocks.stone, 0);

        int cell = source.cellFor(Blocks.planks, 3);
        int adopted = target.adopt(source, cell);

        assertEquals(Blocks.planks, target.blockOf(adopted));
        assertEquals(3, Blueprint.metaOf(adopted));
        assertEquals(2, Blueprint.slotOf(adopted));
    }

    @Test
    void adoptingAnUnknownCellGivesAir() {
        BlockPalette source = palette();
        source.add("nosuchmod:nosuchblock");
        assertEquals(Blueprint.AIR, palette().adopt(source, Blueprint.cell(1, 0)));
    }

    @Test
    void copyFromAppendsEveryName() {
        BlockPalette source = palette();
        source.cellFor(Blocks.stone, 0);
        source.cellFor(Blocks.planks, 0);

        BlockPalette target = new BlockPalette();
        target.copyFrom(source);
        assertEquals(source.size(), target.size());
        assertEquals(source.names(), target.names());
    }

    @Test
    void namesAreNotModifiableFromOutside() {
        BlockPalette palette = palette();
        assertThrows(
            UnsupportedOperationException.class,
            () -> palette.names()
                .add("sneaky"));
    }
}
