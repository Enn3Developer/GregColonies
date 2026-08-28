package com.enn3developer.gregcolonies.colony;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class BlueprintTest {

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    private static Blueprint box(int sizeX, int sizeY, int sizeZ) {
        return Blueprint.empty("box", sizeX, sizeY, sizeZ);
    }

    private static int stone(Blueprint blueprint) {
        return blueprint.getPalette()
            .cellFor(Blocks.stone, 0);
    }

    private static int planks(Blueprint blueprint, int meta) {
        return blueprint.getPalette()
            .cellFor(Blocks.planks, meta);
    }

    @Test
    void fitsRejectsDegenerateAndOversizedShapes() {
        assertTrue(Blueprint.fits(1, 1, 1));
        assertTrue(Blueprint.fits(Blueprint.MAX_SIDE, Blueprint.MAX_SIDE, Blueprint.MAX_SIDE));
        assertFalse(Blueprint.fits(0, 1, 1));
        assertFalse(Blueprint.fits(1, 0, 1));
        assertFalse(Blueprint.fits(1, 1, 0));
        assertFalse(Blueprint.fits(-1, 1, 1));
        assertFalse(Blueprint.fits(Blueprint.MAX_SIDE + 1, 1, 1));
        assertFalse(Blueprint.fits(1, Blueprint.MAX_SIDE + 1, 1));
        assertFalse(Blueprint.fits(1, 1, Blueprint.MAX_SIDE + 1));
    }

    @Test
    void emptyRejectsShapesThatDoNotFit() {
        assertNull(Blueprint.empty("bad", 0, 1, 1));
        assertNull(Blueprint.empty("bad", 64, 1, 1));
    }

    @Test
    void cleanNameStripsControlCharactersAndTrims() {
        assertEquals("hut", Blueprint.cleanName("  hut "));
        assertEquals("hut", Blueprint.cleanName("h\u0007u\nt"));
        assertEquals("hut", Blueprint.cleanName("hut\u007F"));
        assertEquals("", Blueprint.cleanName("\u0000\u001F"));
        assertEquals("", Blueprint.cleanName(null));
        assertEquals("", Blueprint.cleanName(" "));
    }

    @Test
    void cleanNameIsLengthLimited() {
        String overlong = "0123456789012345678901234567890123456789";
        assertEquals(
            Blueprint.MAX_NAME_LENGTH,
            Blueprint.cleanName(overlong)
                .length());
    }

    @Test
    void setNameGoesThroughCleaning() {
        Blueprint blueprint = box(1, 1, 1);
        blueprint.setName("  tower  ");
        assertEquals("tower", blueprint.getName());
    }

    @Test
    void newBlueprintIsEmptyAndCentred() {
        Blueprint blueprint = box(5, 3, 7);
        assertEquals(5 * 3 * 7, blueprint.volume());
        assertEquals(0, blueprint.blockCount());
        assertTrue(blueprint.isEmpty());
        assertEquals(2, blueprint.getOriginX());
        assertEquals(0, blueprint.getOriginY());
        assertEquals(3, blueprint.getOriginZ());
    }

    @Test
    void containsIsBoundsChecked() {
        Blueprint blueprint = box(2, 2, 2);
        assertTrue(blueprint.contains(0, 0, 0));
        assertTrue(blueprint.contains(1, 1, 1));
        assertFalse(blueprint.contains(2, 0, 0));
        assertFalse(blueprint.contains(-1, 0, 0));
        assertFalse(blueprint.contains(0, 0, 2));
    }

    @Test
    void cellsOutsideTheBoxReadAsAir() {
        Blueprint blueprint = box(2, 2, 2);
        blueprint.setCell(0, 0, 0, stone(blueprint));
        assertEquals(Blueprint.AIR, blueprint.cellAt(9, 9, 9));
        assertEquals(Blueprint.AIR, blueprint.cellAt(-1, 0, 0));
    }

    @Test
    void writesOutsideTheBoxAreIgnored() {
        Blueprint blueprint = box(2, 2, 2);
        blueprint.setCell(5, 5, 5, stone(blueprint));
        assertEquals(0, blueprint.blockCount());
    }

    @Test
    void everyCellHasItsOwnAddress() {
        Blueprint blueprint = box(3, 4, 5);
        int cell = stone(blueprint);
        for (int y = 0; y < 4; y++) {
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 3; x++) {
                    blueprint.setCell(x, y, z, cell);
                }
            }
        }
        assertEquals(3 * 4 * 5, blueprint.blockCount());
    }

    @Test
    void cellPacksSlotAndMeta() {
        int cell = Blueprint.cell(7, 13);
        assertEquals(7, Blueprint.slotOf(cell));
        assertEquals(13, Blueprint.metaOf(cell));
    }

    @Test
    void cellMasksMetaToFourBits() {
        int cell = Blueprint.cell(2, 0xFF);
        assertEquals(2, Blueprint.slotOf(cell));
        assertEquals(0xF, Blueprint.metaOf(cell));
    }

    @Test
    void slotZeroWithMetaZeroIsAir() {
        assertEquals(Blueprint.AIR, Blueprint.cell(0, 0));
    }

    @Test
    void trimmedReturnsItselfWhenAlreadyTight() {
        Blueprint blueprint = box(1, 1, 1);
        blueprint.setCell(0, 0, 0, stone(blueprint));
        assertSame(blueprint, blueprint.trimmed());
    }

    @Test
    void trimmedReturnsNullWhenNothingIsSet() {
        assertNull(box(3, 3, 3).trimmed());
    }

    @Test
    void trimmedShrinksToTheOccupiedBox() {
        Blueprint blueprint = box(5, 5, 5);
        int cell = stone(blueprint);
        blueprint.setCell(1, 2, 3, cell);
        blueprint.setCell(2, 2, 3, cell);

        Blueprint trimmed = blueprint.trimmed();
        assertEquals(2, trimmed.getSizeX());
        assertEquals(1, trimmed.getSizeY());
        assertEquals(1, trimmed.getSizeZ());
        assertEquals(2, trimmed.blockCount());
        assertEquals(cell, trimmed.cellAt(0, 0, 0));
        assertEquals(cell, trimmed.cellAt(1, 0, 0));
    }

    @Test
    void trimmedKeepsTheOriginRelativeToTheContent() {
        Blueprint blueprint = box(5, 1, 5);
        int cell = stone(blueprint);
        blueprint.setCell(1, 0, 1, cell);
        blueprint.setCell(3, 0, 3, cell);
        blueprint.setOrigin(2, 0, 3);

        Blueprint trimmed = blueprint.trimmed();
        assertEquals(3, trimmed.getSizeX());
        assertEquals(3, trimmed.getSizeZ());
        assertEquals(1, trimmed.getOriginX());
        assertEquals(2, trimmed.getOriginZ());
    }

    @Test
    void trimmedClampsAnOriginThatFallsOutside() {
        Blueprint blueprint = box(5, 1, 5);
        blueprint.setCell(4, 0, 4, stone(blueprint));
        blueprint.setOrigin(0, 0, 0);

        Blueprint trimmed = blueprint.trimmed();
        assertEquals(1, trimmed.getSizeX());
        assertEquals(0, trimmed.getOriginX());
        assertEquals(0, trimmed.getOriginZ());
    }

    @Test
    void transformIsIdentityForNoRotationAndNoMirror() {
        Blueprint blueprint = box(2, 1, 3);
        assertSame(blueprint, blueprint.transformed(0, false));
        assertSame(blueprint, blueprint.transformed(Blueprint.ROTATIONS, false));
    }

    @Test
    void quarterTurnSwapsTheHorizontalSizes() {
        Blueprint blueprint = box(2, 5, 3);
        Blueprint turned = blueprint.transformed(1, false);
        assertEquals(3, turned.getSizeX());
        assertEquals(5, turned.getSizeY());
        assertEquals(2, turned.getSizeZ());
    }

    @Test
    void halfTurnKeepsTheSizes() {
        Blueprint turned = box(2, 5, 3).transformed(2, false);
        assertEquals(2, turned.getSizeX());
        assertEquals(3, turned.getSizeZ());
    }

    @Test
    void quarterTurnMovesCellsToTheExpectedPlace() {
        Blueprint blueprint = box(2, 1, 3);
        int cell = stone(blueprint);
        blueprint.setCell(0, 0, 0, cell);

        Blueprint turned = blueprint.transformed(1, false);
        assertEquals(cell, turned.cellAt(2, 0, 0));
        assertEquals(1, turned.blockCount());
    }

    @Test
    void mirrorFlipsAlongX() {
        Blueprint blueprint = box(2, 1, 1);
        int cell = stone(blueprint);
        blueprint.setCell(0, 0, 0, cell);

        Blueprint mirrored = blueprint.transformed(0, true);
        assertEquals(Blueprint.AIR, mirrored.cellAt(0, 0, 0));
        assertEquals(cell, mirrored.cellAt(1, 0, 0));
    }

    @Test
    void fourQuarterTurnsComeBackToTheStart() {
        Blueprint blueprint = box(3, 2, 4);
        int cell = stone(blueprint);
        blueprint.setCell(1, 1, 2, cell);

        Blueprint turned = blueprint;
        for (int i = 0; i < 4; i++) {
            turned = turned.transformed(1, false);
        }
        assertEquals(3, turned.getSizeX());
        assertEquals(4, turned.getSizeZ());
        assertEquals(cell, turned.cellAt(1, 1, 2));
    }

    @Test
    void negativeRotationsNormalise() {
        Blueprint blueprint = box(2, 1, 3);
        blueprint.setCell(0, 0, 0, stone(blueprint));
        Blueprint back = blueprint.transformed(-1, false);
        Blueprint forward = blueprint.transformed(3, false);
        assertEquals(forward.getSizeX(), back.getSizeX());
        assertEquals(forward.cellAt(0, 0, 1), back.cellAt(0, 0, 1));
    }

    @Test
    void turningTheBlueprintTurnsTheBlocksInIt() {
        Blueprint blueprint = box(1, 1, 1);
        int north = blueprint.getPalette()
            .cellFor(Blocks.ladder, ForgeDirection.NORTH.ordinal());
        blueprint.setCell(0, 0, 0, north);

        Blueprint turned = blueprint.transformed(1, false);
        assertEquals(
            Blocks.ladder,
            turned.getPalette()
                .blockOf(turned.cellAt(0, 0, 0)));
        assertEquals(ForgeDirection.EAST.ordinal(), Blueprint.metaOf(turned.cellAt(0, 0, 0)));
    }

    @Test
    void mirroringTheBlueprintMirrorsTheBlocksInIt() {
        Blueprint blueprint = box(1, 1, 1);
        blueprint.setCell(
            0,
            0,
            0,
            blueprint.getPalette()
                .cellFor(Blocks.ladder, ForgeDirection.EAST.ordinal()));

        Blueprint mirrored = blueprint.transformed(0, true);
        assertEquals(ForgeDirection.WEST.ordinal(), Blueprint.metaOf(mirrored.cellAt(0, 0, 0)));
    }

    @Test
    void transformKeepsTheBlockCount() {
        Blueprint blueprint = box(3, 2, 4);
        int cell = stone(blueprint);
        for (int x = 0; x < 3; x++) {
            blueprint.setCell(x, 0, 0, cell);
        }
        assertEquals(
            3,
            blueprint.transformed(1, true)
                .blockCount());
    }

    @Test
    void resizedRejectsShapesThatDoNotFit() {
        assertNull(box(2, 2, 2).resized(0, 2, 2, 0, 0, 0));
        assertNull(box(2, 2, 2).resized(2, 2, Blueprint.MAX_SIDE + 1, 0, 0, 0));
    }

    @Test
    void resizedShiftsContent() {
        Blueprint blueprint = box(2, 1, 2);
        int cell = stone(blueprint);
        blueprint.setCell(0, 0, 0, cell);

        Blueprint resized = blueprint.resized(4, 1, 4, 1, 0, 1);
        assertEquals(4, resized.getSizeX());
        assertEquals(cell, resized.cellAt(1, 0, 1));
        assertEquals(Blueprint.AIR, resized.cellAt(0, 0, 0));
        assertEquals(1, resized.blockCount());
    }

    @Test
    void resizedDropsContentPushedOutside() {
        Blueprint blueprint = box(3, 1, 1);
        int cell = stone(blueprint);
        for (int x = 0; x < 3; x++) {
            blueprint.setCell(x, 0, 0, cell);
        }
        assertEquals(
            1,
            blueprint.resized(1, 1, 1, 0, 0, 0)
                .blockCount());
    }

    @Test
    void copyIsIndependent() {
        Blueprint blueprint = box(2, 2, 2);
        int cell = stone(blueprint);
        blueprint.setCell(0, 0, 0, cell);

        Blueprint clone = blueprint.copy();
        clone.setCell(1, 1, 1, cell);
        assertEquals(1, blueprint.blockCount());
        assertEquals(2, clone.blockCount());
        assertEquals(blueprint.getName(), clone.getName());
    }

    @Test
    void setOriginIsClampedToTheBox() {
        Blueprint blueprint = box(3, 3, 3);
        blueprint.setOrigin(99, -4, 1);
        assertEquals(2, blueprint.getOriginX());
        assertEquals(0, blueprint.getOriginY());
        assertEquals(1, blueprint.getOriginZ());
    }

    @Test
    void materialsCountsPerItemAndSortsByCount() {
        Blueprint blueprint = box(4, 1, 1);
        int stone = stone(blueprint);
        int planks = planks(blueprint, 0);
        blueprint.setCell(0, 0, 0, planks);
        blueprint.setCell(1, 0, 0, stone);
        blueprint.setCell(2, 0, 0, stone);
        blueprint.setCell(3, 0, 0, stone);

        Map<Integer, Integer> materials = blueprint.materials();
        assertEquals(2, materials.size());
        List<Integer> counts = new ArrayList<>(materials.values());
        assertEquals(3, counts.get(0));
        assertEquals(1, counts.get(1));
    }

    @Test
    void materialsFoldsCellsThatDropTheSameItem() {
        Blueprint blueprint = box(2, 1, 1);
        int upright = blueprint.getPalette()
            .cellFor(Blocks.log, 0);
        int sideways = blueprint.getPalette()
            .cellFor(Blocks.log, 4);
        assertNotEquals(upright, sideways);
        blueprint.setCell(0, 0, 0, upright);
        blueprint.setCell(1, 0, 0, sideways);

        Map<Integer, Integer> materials = blueprint.materials();
        assertEquals(1, materials.size());
        List<Integer> counts = new ArrayList<>(materials.values());
        assertEquals(2, counts.get(0));
    }

    @Test
    void materialsIsEmptyForAnEmptyBlueprint() {
        assertTrue(
            box(2, 2, 2).materials()
                .isEmpty());
    }

    @Test
    void nbtRoundTrips() {
        Blueprint blueprint = box(3, 2, 4);
        blueprint.setName("cabin");
        blueprint.setCell(0, 0, 0, stone(blueprint));
        blueprint.setCell(2, 1, 3, planks(blueprint, 2));
        blueprint.setOrigin(1, 1, 2);

        NBTTagCompound tag = blueprint.writeToNBT();
        Blueprint read = Blueprint.readFromNBT(tag);

        assertNotNull(read);
        assertEquals("cabin", read.getName());
        assertEquals(3, read.getSizeX());
        assertEquals(2, read.getSizeY());
        assertEquals(4, read.getSizeZ());
        assertEquals(1, read.getOriginX());
        assertEquals(1, read.getOriginY());
        assertEquals(2, read.getOriginZ());
        assertEquals(2, read.blockCount());
        assertEquals(
            Blocks.stone,
            read.getPalette()
                .blockOf(read.cellAt(0, 0, 0)));
        assertEquals(
            Blocks.planks,
            read.getPalette()
                .blockOf(read.cellAt(2, 1, 3)));
        assertEquals(2, Blueprint.metaOf(read.cellAt(2, 1, 3)));
    }

    @Test
    void nbtWithoutAnOriginCentres() {
        Blueprint blueprint = box(5, 1, 5);
        NBTTagCompound tag = blueprint.writeToNBT();
        tag.removeTag("ox");

        Blueprint read = Blueprint.readFromNBT(tag);
        assertEquals(2, read.getOriginX());
        assertEquals(2, read.getOriginZ());
    }

    @Test
    void nbtWithAMismatchedCellCountIsRejected() {
        Blueprint blueprint = box(2, 2, 2);
        NBTTagCompound tag = blueprint.writeToNBT();
        tag.setIntArray("cells", new int[] { 0, 0, 0 });
        assertNull(Blueprint.readFromNBT(tag));
    }

    @Test
    void nbtWithZeroVolumeIsRejected() {
        NBTTagCompound tag = box(2, 2, 2).writeToNBT();
        tag.setInteger("sx", 0);
        assertNull(Blueprint.readFromNBT(tag));
    }

    @Test
    void bufferRoundTrips() {
        Blueprint blueprint = box(4, 2, 3);
        blueprint.setName("wire");
        int cell = stone(blueprint);
        for (int x = 0; x < 4; x++) {
            blueprint.setCell(x, 0, 0, cell);
        }
        blueprint.setOrigin(2, 1, 1);

        ByteBuf buf = Unpooled.buffer();
        blueprint.write(buf);
        Blueprint read = Blueprint.read(buf);

        assertNotNull(read);
        assertEquals(0, buf.readableBytes());
        assertEquals("wire", read.getName());
        assertEquals(4, read.getSizeX());
        assertEquals(4, read.blockCount());
        assertEquals(2, read.getOriginX());
        assertEquals(1, read.getOriginY());
        assertEquals(1, read.getOriginZ());
        assertEquals(cell, read.cellAt(3, 0, 0));
    }

    @Test
    void runLengthEncodingSurvivesLongRuns() {
        Blueprint blueprint = box(Blueprint.MAX_SIDE, 2, 2);
        int cell = stone(blueprint);
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < Blueprint.MAX_SIDE; x++) {
                blueprint.setCell(x, 0, z, cell);
            }
        }

        ByteBuf buf = Unpooled.buffer();
        blueprint.write(buf);
        Blueprint read = Blueprint.read(buf);
        assertEquals(blueprint.blockCount(), read.blockCount());
    }

    @Test
    void bufferWithAnImpossibleSizeIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeUTF8String(buf, "bad");
        buf.writeShort(999);
        buf.writeShort(1);
        buf.writeShort(1);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(1);
        assertNull(Blueprint.read(buf));
    }

    @Test
    void bufferWithARunPastTheEndIsRejected() {
        ByteBuf buf = oneCellHeader();
        ByteBufUtils.writeVarInt(buf, 5, 5);
        ByteBufUtils.writeVarInt(buf, 0, 5);
        assertNull(Blueprint.read(buf));
    }

    @Test
    void bufferWithAnEmptyRunIsRejected() {
        ByteBuf buf = oneCellHeader();
        ByteBufUtils.writeVarInt(buf, 0, 5);
        ByteBufUtils.writeVarInt(buf, 0, 5);
        assertNull(Blueprint.read(buf));
    }

    private static ByteBuf oneCellHeader() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeUTF8String(buf, "bad");
        buf.writeShort(1);
        buf.writeShort(1);
        buf.writeShort(1);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(1);
        ByteBufUtils.writeUTF8String(buf, "");
        ByteBufUtils.writeVarInt(buf, 1, 5);
        return buf;
    }

    @Test
    void bufferWithNoPaletteIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufUtils.writeUTF8String(buf, "bad");
        buf.writeShort(1);
        buf.writeShort(1);
        buf.writeShort(1);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(0);
        assertNull(Blueprint.read(buf));
    }

    @Test
    void isPlaceableAcceptsRealBlocksAndRejectsUnknownOnes() {
        Blueprint blueprint = box(2, 1, 1);
        blueprint.setCell(0, 0, 0, stone(blueprint));
        assertTrue(blueprint.isPlaceable());

        Blueprint broken = box(2, 1, 1);
        broken.getPalette()
            .add("thismod:nope");
        broken.setCell(0, 0, 0, Blueprint.cell(1, 0));
        assertFalse(broken.isPlaceable());
    }

    @Test
    void emptyBlueprintIsTriviallyPlaceable() {
        assertTrue(box(2, 2, 2).isPlaceable());
    }

    @Test
    void adoptCarriesACellIntoAnotherPalette() {
        Blueprint source = box(1, 1, 1);
        int cell = source.getPalette()
            .cellFor(Blocks.planks, 3);

        Blueprint target = box(1, 1, 1);
        int adopted = target.adopt(source, cell);

        assertEquals(
            Blocks.planks,
            target.getPalette()
                .blockOf(adopted));
        assertEquals(3, Blueprint.metaOf(adopted));
    }

    @Test
    void adoptingAirStaysAir() {
        assertEquals(Blueprint.AIR, box(1, 1, 1).adopt(box(1, 1, 1), Blueprint.AIR));
    }
}
