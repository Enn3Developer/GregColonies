package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

class ColonySiteTest {

    @Test
    void startsAbsent() {
        ColonySite site = new ColonySite();
        assertFalse(site.isPresent());
        assertFalse(site.isAt(0, 0, 0));
    }

    @Test
    void setThenClear() {
        ColonySite site = new ColonySite();
        site.set(4, 64, -9);
        assertTrue(site.isPresent());
        assertTrue(site.isAt(4, 64, -9));
        assertFalse(site.isAt(4, 64, -8));
        assertEquals("4/64/-9", site.describe());

        site.clear();
        assertFalse(site.isPresent());
        assertEquals(0, site.getX());
        assertEquals(0, site.getY());
        assertEquals(0, site.getZ());
    }

    @Test
    void absentSiteIsNeverAtAnyPosition() {
        ColonySite site = new ColonySite();
        site.set(0, 0, 0);
        site.clear();
        assertFalse(site.isAt(0, 0, 0));
    }

    @Test
    void copyFromTakesEverything() {
        ColonySite source = new ColonySite();
        source.set(1, 2, 3);
        ColonySite target = new ColonySite();
        target.copyFrom(source);
        assertTrue(target.isAt(1, 2, 3));
    }

    @Test
    void nbtRoundTripsPerKind() {
        NBTTagCompound tag = new NBTTagCompound();
        ColonySite dropOff = new ColonySite();
        dropOff.set(10, 20, 30);
        ColonySite materials = new ColonySite();
        materials.set(-1, -2, -3);
        dropOff.writeToNBT(tag, ColonySiteKind.DROP_OFF);
        materials.writeToNBT(tag, ColonySiteKind.MATERIALS);

        ColonySite readDropOff = new ColonySite();
        ColonySite readPickUp = new ColonySite();
        ColonySite readMaterials = new ColonySite();
        readDropOff.readFromNBT(tag, ColonySiteKind.DROP_OFF);
        readPickUp.readFromNBT(tag, ColonySiteKind.PICK_UP);
        readMaterials.readFromNBT(tag, ColonySiteKind.MATERIALS);

        assertTrue(readDropOff.isAt(10, 20, 30));
        assertFalse(readPickUp.isPresent());
        assertTrue(readMaterials.isAt(-1, -2, -3));
    }

    @Test
    void absentSiteReadsBackAsZeroed() {
        NBTTagCompound tag = new NBTTagCompound();
        ColonySite site = new ColonySite();
        site.writeToNBT(tag, ColonySiteKind.PICK_UP);

        ColonySite read = new ColonySite();
        read.set(9, 9, 9);
        read.readFromNBT(tag, ColonySiteKind.PICK_UP);
        assertFalse(read.isPresent());
        assertEquals(0, read.getX());
    }

    @Test
    void bufferRoundTrips() {
        ColonySite site = new ColonySite();
        site.set(-40, 70, 400);
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        site.write(buf);

        ColonySite read = new ColonySite();
        read.read(buf);
        assertTrue(read.isAt(-40, 70, 400));
        assertEquals(0, buf.readableBytes());
    }
}
