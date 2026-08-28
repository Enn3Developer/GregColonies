package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class BlockRotationTest {

    private static final int NORTH = ForgeDirection.NORTH.ordinal();

    private static final int SOUTH = ForgeDirection.SOUTH.ordinal();

    private static final int WEST = ForgeDirection.WEST.ordinal();

    private static final int EAST = ForgeDirection.EAST.ordinal();

    private static final int UP = ForgeDirection.UP.ordinal();

    private static final int DOWN = ForgeDirection.DOWN.ordinal();

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void nullBlocksAreLeftAlone() {
        assertEquals(7, BlockRotation.transform(null, 7, 1, true));
    }

    @Test
    void theIdentityTransformIsLeftAlone() {
        assertEquals(5, BlockRotation.transform(Blocks.ladder, 5, 0, false));
    }

    @Test
    void plainBlocksKeepTheirMeta() {
        assertEquals(3, BlockRotation.transform(Blocks.stone, 3, 1, true));
        assertEquals(2, BlockRotation.transform(Blocks.planks, 2, 3, false));
        assertEquals(7, BlockRotation.transform(Blocks.wool, 7, 2, true));
    }

    @Test
    void ladderTurnsClockwise() {
        assertEquals(EAST, BlockRotation.transform(Blocks.ladder, NORTH, 1, false));
        assertEquals(SOUTH, BlockRotation.transform(Blocks.ladder, EAST, 1, false));
        assertEquals(WEST, BlockRotation.transform(Blocks.ladder, SOUTH, 1, false));
        assertEquals(NORTH, BlockRotation.transform(Blocks.ladder, WEST, 1, false));
    }

    @Test
    void fourQuarterTurnsRestoreTheFacing() {
        int meta = NORTH;
        for (int i = 0; i < 4; i++) {
            meta = BlockRotation.transform(Blocks.ladder, meta, 1, false);
        }
        assertEquals(NORTH, meta);
    }

    @Test
    void mirroringSwapsEastAndWest() {
        assertEquals(WEST, BlockRotation.transform(Blocks.ladder, EAST, 0, true));
        assertEquals(EAST, BlockRotation.transform(Blocks.ladder, WEST, 0, true));
    }

    @Test
    void mirroringLeavesNorthAndSouthAlone() {
        assertEquals(NORTH, BlockRotation.transform(Blocks.ladder, NORTH, 0, true));
        assertEquals(SOUTH, BlockRotation.transform(Blocks.ladder, SOUTH, 0, true));
    }

    @Test
    void verticalFacingsAreNeverTurned() {
        assertEquals(UP, BlockRotation.transform(Blocks.piston, UP, 1, true));
        assertEquals(DOWN, BlockRotation.transform(Blocks.piston, DOWN, 3, true));
    }

    @Test
    void furnaceAndChestTurnLikeLadders() {
        assertEquals(EAST, BlockRotation.transform(Blocks.furnace, NORTH, 1, false));
        assertEquals(EAST, BlockRotation.transform(Blocks.chest, NORTH, 1, false));
        assertEquals(EAST, BlockRotation.transform(Blocks.ender_chest, NORTH, 1, false));
    }

    @Test
    void pistonsKeepTheirExtendedBit() {
        int extended = 8;
        int meta = BlockRotation.transform(Blocks.piston, NORTH | extended, 1, false);
        assertEquals(EAST, meta & 7);
        assertEquals(extended, meta & 8);
    }

    @Test
    void dispensersKeepTheirTriggeredBit() {
        int triggered = 8;
        int meta = BlockRotation.transform(Blocks.dispenser, SOUTH | triggered, 2, false);
        assertEquals(NORTH, meta & 7);
        assertEquals(triggered, meta & 8);
    }

    @Test
    void pillarsSwapAxesOnOddTurns() {
        assertEquals(8, BlockRotation.transform(Blocks.log, 4, 1, false));
        assertEquals(4, BlockRotation.transform(Blocks.log, 8, 1, false));
        assertEquals(12, BlockRotation.transform(Blocks.log, 12, 1, false));
        assertEquals(0, BlockRotation.transform(Blocks.log, 0, 1, false));
    }

    @Test
    void pillarsKeepTheirWoodTypeWhenTurned() {
        assertEquals(8 | 2, BlockRotation.transform(Blocks.log, 4 | 2, 1, false));
    }

    @Test
    void pillarsAreUntouchedOnEvenTurns() {
        assertEquals(4, BlockRotation.transform(Blocks.log, 4, 2, false));
        assertEquals(8, BlockRotation.transform(Blocks.log, 8, 2, true));
    }

    @Test
    void quartzPillarsSwapAxesOnOddTurns() {
        assertEquals(4, BlockRotation.transform(Blocks.quartz_block, 3, 1, false));
        assertEquals(3, BlockRotation.transform(Blocks.quartz_block, 4, 1, false));
        assertEquals(2, BlockRotation.transform(Blocks.quartz_block, 2, 1, false));
        assertEquals(3, BlockRotation.transform(Blocks.quartz_block, 3, 2, false));
    }

    @Test
    void pumpkinsTurnWithinTheirLowTwoBits() {
        assertEquals(1, BlockRotation.transform(Blocks.pumpkin, 0, 1, false));
        assertEquals(0, BlockRotation.transform(Blocks.pumpkin, 3, 1, false));
        assertEquals(2, BlockRotation.transform(Blocks.pumpkin, 0, 2, false));
    }

    @Test
    void pumpkinsMirrorAcrossTheOddFacings() {
        assertEquals(3, BlockRotation.transform(Blocks.pumpkin, 1, 0, true));
        assertEquals(1, BlockRotation.transform(Blocks.pumpkin, 3, 0, true));
        assertEquals(0, BlockRotation.transform(Blocks.pumpkin, 0, 0, true));
    }

    @Test
    void stairsTurnAndKeepTheirHalf() {
        assertEquals(2, BlockRotation.transform(Blocks.oak_stairs, 0, 1, false));
        assertEquals(2 | 4, BlockRotation.transform(Blocks.oak_stairs, 4, 1, false));
    }

    @Test
    void fourQuarterTurnsRestoreStairs() {
        int meta = 0;
        for (int i = 0; i < 4; i++) {
            meta = BlockRotation.transform(Blocks.oak_stairs, meta, 1, false);
        }
        assertEquals(0, meta);
    }

    @Test
    void stairsMirrorAcrossTheEastWestPair() {
        assertEquals(1, BlockRotation.transform(Blocks.oak_stairs, 0, 0, true));
        assertEquals(0, BlockRotation.transform(Blocks.oak_stairs, 1, 0, true));
    }

    @Test
    void everyStairMetaStaysAValidStairMeta() {
        for (int meta = 0; meta < 8; meta++) {
            for (int steps = 0; steps < 4; steps++) {
                int turned = BlockRotation.transform(Blocks.oak_stairs, meta, steps, false);
                assertTrue(turned >= 0 && turned < 8, "stair meta " + meta + " turned into " + turned);
                assertEquals(meta & 4, turned & 4, "the top/bottom half must survive rotation");
            }
        }
    }

    @Test
    void everyLadderMetaStaysAHorizontalFacing() {
        for (int meta = 2; meta <= 5; meta++) {
            for (int steps = 0; steps < 4; steps++) {
                int turned = BlockRotation.transform(Blocks.ladder, meta, steps, true);
                assertTrue(turned >= 2 && turned <= 5, "ladder meta " + meta + " turned into " + turned);
            }
        }
    }
}
