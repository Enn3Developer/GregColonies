package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BlockKeyTest {

    @Test
    void distinctCoordinatesGiveDistinctKeys() {
        Set<Long> keys = new HashSet<>();
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y < 40; y++) {
                for (int z = -4; z <= 4; z++) {
                    keys.add(BlockKey.pack(x, y, z));
                }
            }
        }
        assertEquals(9 * 40 * 9, keys.size());
    }

    @Test
    void axesDoNotBleedIntoEachOther() {
        assertNotEquals(BlockKey.pack(1, 0, 0), BlockKey.pack(0, 1, 0));
        assertNotEquals(BlockKey.pack(1, 0, 0), BlockKey.pack(0, 0, 1));
        assertNotEquals(BlockKey.pack(0, 1, 0), BlockKey.pack(0, 0, 1));
    }

    @Test
    void negativeCoordinatesStayDistinct() {
        assertNotEquals(BlockKey.pack(-1, 64, -1), BlockKey.pack(1, 64, 1));
        assertNotEquals(BlockKey.pack(-1, 64, 5), BlockKey.pack(-1, 64, -5));
    }

    @Test
    void isStable() {
        assertEquals(BlockKey.pack(12, 64, -30), BlockKey.pack(12, 64, -30));
    }

    @Test
    void heightWrapsAtTwelveBits() {
        assertEquals(BlockKey.pack(0, 0, 0), BlockKey.pack(0, 4096, 0));
    }

    @Test
    void everyAxisKeepsItsOwnBits() {
        for (int bit = 0; bit < 26; bit++) {
            int step = 1 << bit;
            assertNotEquals(BlockKey.pack(0, 64, 0), BlockKey.pack(step, 64, 0), "x aliases at a step of " + step);
            assertNotEquals(BlockKey.pack(0, 64, 0), BlockKey.pack(0, 64, step), "z aliases at a step of " + step);
        }
        for (int bit = 0; bit < 12; bit++) {
            int step = 1 << bit;
            assertNotEquals(BlockKey.pack(0, 0, 0), BlockKey.pack(0, step, 0), "y aliases at a step of " + step);
        }
    }

    @Test
    void theWholeWorldBorderPacksWithoutCollisions() {
        int[] edges = { -30_000_000, -1, 0, 1, 30_000_000 };
        Set<Long> keys = new HashSet<>();
        for (int x : edges) {
            for (int z : edges) {
                for (int y = 0; y < 256; y++) {
                    keys.add(BlockKey.pack(x, y, z));
                }
            }
        }
        assertEquals(edges.length * edges.length * 256, keys.size());
    }
}
