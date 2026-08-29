package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class WorkAreaTest {

    @Test
    void cornersAreNormalised() {
        WorkArea area = new WorkArea(10, 70, -5, 2, 64, 3);
        assertEquals(2, area.getMinX());
        assertEquals(64, area.getMinY());
        assertEquals(-5, area.getMinZ());
        assertEquals(10, area.getMaxX());
        assertEquals(70, area.getMaxY());
        assertEquals(3, area.getMaxZ());
    }

    @Test
    void centreIsTheMidpoint() {
        WorkArea area = new WorkArea(0, 0, 0, 10, 6, 4);
        assertEquals(5, area.getCenterX());
        assertEquals(3, area.getCenterY());
        assertEquals(2, area.getCenterZ());
    }

    @Test
    void overlapNeedsEveryAxisToMeet() {
        WorkArea area = new WorkArea(0, 0, 0, 4, 4, 4);
        assertTrue(area.overlaps(new WorkArea(4, 4, 4, 8, 8, 8)));
        assertTrue(area.overlaps(new WorkArea(-8, -8, -8, 8, 8, 8)));
        assertFalse(area.overlaps(new WorkArea(5, 0, 0, 8, 4, 4)));
        assertFalse(area.overlaps(new WorkArea(0, 5, 0, 4, 8, 4)));
        assertFalse(area.overlaps(new WorkArea(0, 0, 5, 4, 4, 8)));
    }

    @Test
    void copyFromTakesEveryCorner() {
        WorkArea area = new WorkArea();
        area.copyFrom(new WorkArea(1, 2, 3, 4, 5, 6));
        assertEquals("1/2/3 to 4/5/6", area.describe());
    }

    @Test
    void aRegionSurvivesTheWire() {
        ByteBuf buf = Unpooled.buffer();
        new WorkArea(-3, 12, 40, 7, 20, 44).write(buf);
        WorkArea area = new WorkArea();
        area.read(buf);
        assertEquals("-3/12/40 to 7/20/44", area.describe());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void containsIsInclusive() {
        WorkArea area = new WorkArea(0, 0, 0, 2, 2, 2);
        assertTrue(area.contains(0, 0, 0));
        assertTrue(area.contains(2, 2, 2));
        assertTrue(area.contains(1, 1, 1));
        assertFalse(area.contains(3, 1, 1));
        assertFalse(area.contains(1, -1, 1));
        assertFalse(area.contains(1, 1, 3));
    }

    @Test
    void capHeightTrimsFromTheBottom() {
        WorkArea area = new WorkArea(0, 60, 0, 0, 200, 0);
        area.capHeight(WorkArea.MAX_HEIGHT);
        assertEquals(60, area.getMinY());
        assertEquals(60 + WorkArea.MAX_HEIGHT - 1, area.getMaxY());
    }

    @Test
    void capHeightLeavesSmallAreasAlone() {
        WorkArea area = new WorkArea(0, 60, 0, 0, 64, 0);
        area.capHeight(WorkArea.MAX_HEIGHT);
        assertEquals(64, area.getMaxY());
    }

    @Test
    void capSideTrimsBothHorizontalAxes() {
        WorkArea area = new WorkArea(-100, 0, -100, 100, 0, 100);
        area.capSide(WorkArea.MAX_SIDE);
        assertEquals(-100 + WorkArea.MAX_SIDE - 1, area.getMaxX());
        assertEquals(-100 + WorkArea.MAX_SIDE - 1, area.getMaxZ());
        assertEquals(-100, area.getMinX());
    }

    @Test
    void nbtRoundTrips() {
        WorkArea area = new WorkArea(-8, 12, 40, 9, 70, 44);
        NBTTagCompound tag = new NBTTagCompound();
        area.writeToNBT(tag);

        WorkArea read = new WorkArea();
        read.readFromNBT(tag);
        assertEquals(area.getMinX(), read.getMinX());
        assertEquals(area.getMinY(), read.getMinY());
        assertEquals(area.getMinZ(), read.getMinZ());
        assertEquals(area.getMaxX(), read.getMaxX());
        assertEquals(area.getMaxY(), read.getMaxY());
        assertEquals(area.getMaxZ(), read.getMaxZ());
    }

    @Test
    void setReplacesEverything() {
        WorkArea area = new WorkArea(0, 0, 0, 1, 1, 1);
        area.set(5, 5, 5, 5, 5, 5);
        assertTrue(area.contains(5, 5, 5));
        assertFalse(area.contains(0, 0, 0));
    }
}
