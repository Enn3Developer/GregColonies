package com.enn3developer.gregcolonies.entity.ai.work;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.FakeInventory;
import com.enn3developer.gregcolonies.testing.FakeSidedInventory;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class InventoriesTest {

    private static final Predicate<ItemStack> ANY = stack -> true;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    private static Predicate<ItemStack> is(net.minecraft.block.Block block) {
        return stack -> net.minecraft.block.Block.getBlockFromItem(stack.getItem()) == block;
    }

    @Test
    void countingANullInventoryIsZero() {
        assertEquals(0, Inventories.count(null, ANY));
    }

    @Test
    void countingSumsMatchingStacks() {
        FakeInventory inventory = new FakeInventory(4).with(0, new ItemStack(Blocks.dirt, 10))
            .with(2, new ItemStack(Blocks.dirt, 5))
            .with(3, new ItemStack(Blocks.stone, 7));

        assertEquals(15, Inventories.count(inventory, is(Blocks.dirt)));
        assertEquals(22, Inventories.count(inventory, ANY));
        assertEquals(0, Inventories.count(inventory, is(Blocks.gold_block)));
    }

    @Test
    void emptyStacksAreIgnored() {
        FakeInventory inventory = new FakeInventory(2).with(0, new ItemStack(Blocks.dirt, 0));
        assertEquals(0, Inventories.count(inventory, ANY));
    }

    @Test
    void forEachExtractableVisitsEveryFilledSlot() {
        FakeInventory inventory = new FakeInventory(3).with(0, new ItemStack(Blocks.dirt, 1))
            .with(2, new ItemStack(Blocks.stone, 1));

        List<ItemStack> seen = new ArrayList<>();
        Inventories.forEachExtractable(inventory, seen::add);
        assertEquals(2, seen.size());
    }

    @Test
    void forEachExtractableOnNullIsHarmless() {
        Inventories.forEachExtractable(null, stack -> fail("nothing to visit"));
    }

    @Test
    void extractingFromNothingOrForNothingIsNull() {
        FakeInventory inventory = new FakeInventory(1).with(0, new ItemStack(Blocks.dirt, 5));
        assertNull(Inventories.extract(null, ANY, 1));
        assertNull(Inventories.extract(inventory, ANY, 0));
        assertNull(Inventories.extract(inventory, ANY, -1));
        assertNull(Inventories.extract(new FakeInventory(3), ANY, 1));
    }

    @Test
    void extractingTakesFromTheFirstMatchingSlot() {
        FakeInventory inventory = new FakeInventory(3).with(0, new ItemStack(Blocks.stone, 4))
            .with(1, new ItemStack(Blocks.dirt, 9));

        ItemStack taken = Inventories.extract(inventory, is(Blocks.dirt), 3);
        assertNotNull(taken);
        assertEquals(3, taken.stackSize);
        assertEquals(6, inventory.getStackInSlot(1).stackSize);
        assertEquals(4, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    void extractingMoreThanIsThereTakesWhatIsThere() {
        FakeInventory inventory = new FakeInventory(1).with(0, new ItemStack(Blocks.dirt, 2));
        ItemStack taken = Inventories.extract(inventory, ANY, 64);
        assertEquals(2, taken.stackSize);
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    void extractingSkipsSlotsTheFilterRejects() {
        FakeInventory inventory = new FakeInventory(2).with(0, new ItemStack(Blocks.stone, 4));
        assertNull(Inventories.extract(inventory, is(Blocks.dirt), 1));
        assertEquals(4, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    void insertingNothingReturnsWhatItWasGiven() {
        FakeInventory inventory = new FakeInventory(2);
        assertNull(Inventories.insert(inventory, null));

        ItemStack empty = new ItemStack(Blocks.dirt, 0);
        assertSame(empty, Inventories.insert(inventory, empty));

        ItemStack stack = new ItemStack(Blocks.dirt, 1);
        assertSame(stack, Inventories.insert(null, stack));
    }

    @Test
    void insertingFillsAnEmptySlot() {
        FakeInventory inventory = new FakeInventory(2);
        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.dirt, 10)));
        assertEquals(10, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    void insertingMergesBeforeOpeningANewSlot() {
        FakeInventory inventory = new FakeInventory(3).with(1, new ItemStack(Blocks.dirt, 60));

        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.dirt, 4)));
        assertEquals(64, inventory.getStackInSlot(1).stackSize);
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    void overflowSpillsIntoTheNextSlot() {
        FakeInventory inventory = new FakeInventory(3).with(1, new ItemStack(Blocks.dirt, 60));

        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.dirt, 10)));
        assertEquals(64, inventory.getStackInSlot(1).stackSize);
        assertEquals(6, inventory.getStackInSlot(0).stackSize);
        assertEquals(70, inventory.total());
    }

    @Test
    void aFullInventoryHandsTheRemainderBack() {
        FakeInventory inventory = new FakeInventory(1).with(0, new ItemStack(Blocks.dirt, 64));

        ItemStack rest = Inventories.insert(inventory, new ItemStack(Blocks.dirt, 10));
        assertNotNull(rest);
        assertEquals(10, rest.stackSize);
        assertEquals(64, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    void aPartiallyFullInventoryHandsBackWhatDidNotFit() {
        FakeInventory inventory = new FakeInventory(1).with(0, new ItemStack(Blocks.dirt, 60));

        ItemStack rest = Inventories.insert(inventory, new ItemStack(Blocks.dirt, 10));
        assertNotNull(rest);
        assertEquals(6, rest.stackSize);
        assertEquals(64, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    void differentItemsNeverMerge() {
        FakeInventory inventory = new FakeInventory(2).with(0, new ItemStack(Blocks.dirt, 1));

        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.stone, 1)));
        assertEquals(1, inventory.getStackInSlot(0).stackSize);
        assertEquals(1, inventory.getStackInSlot(1).stackSize);
    }

    @Test
    void differentDamageValuesNeverMerge() {
        FakeInventory inventory = new FakeInventory(2).with(0, new ItemStack(Blocks.planks, 1, 0));

        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.planks, 1, 3)));
        assertEquals(1, inventory.getStackInSlot(1).stackSize);
    }

    @Test
    void differentNbtNeverMerges() {
        ItemStack tagged = new ItemStack(Blocks.dirt, 1);
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound()
            .setInteger("mark", 1);
        assertTrue(tagged.isStackable(), "this test only says something about NBT if the item stacks");

        FakeInventory inventory = new FakeInventory(2).with(0, tagged);
        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.dirt, 1)));
        assertEquals(1, inventory.getStackInSlot(0).stackSize);
        assertEquals(1, inventory.getStackInSlot(1).stackSize);
    }

    @Test
    void unstackableItemsNeverMerge() {
        FakeInventory inventory = new FakeInventory(2).with(0, new ItemStack(Items.diamond_sword));
        assertNull(Inventories.insert(inventory, new ItemStack(Items.diamond_sword)));
        assertNotNull(inventory.getStackInSlot(1));
    }

    @Test
    void theInventoryStackLimitIsRespected() {
        FakeInventory inventory = new FakeInventory(2).stackLimit(4);

        ItemStack rest = Inventories.insert(inventory, new ItemStack(Blocks.dirt, 20));
        assertEquals(4, inventory.getStackInSlot(0).stackSize);
        assertEquals(4, inventory.getStackInSlot(1).stackSize);
        assertNotNull(rest);
        assertEquals(12, rest.stackSize);
    }

    @Test
    void sidedInventoriesOnlyExposeTheirAccessibleSlots() {
        FakeSidedInventory inventory = new FakeSidedInventory(3);
        inventory.with(0, new ItemStack(Blocks.dirt, 5))
            .with(1, new ItemStack(Blocks.stone, 5));
        inventory.expose(2, 1);

        assertEquals(5, Inventories.count(inventory, ANY));
        ItemStack taken = Inventories.extract(inventory, ANY, 64);
        assertEquals(Blocks.stone, net.minecraft.block.Block.getBlockFromItem(taken.getItem()));
    }

    @Test
    void aSlotVisibleFromTwoSidesIsCountedOnce() {
        FakeSidedInventory inventory = new FakeSidedInventory(2);
        inventory.with(0, new ItemStack(Blocks.dirt, 5));
        inventory.expose(0, 0)
            .expose(1, 0)
            .expose(2, 0);

        assertEquals(5, Inventories.count(inventory, ANY));
    }

    @Test
    void sidedInventoriesThatRefuseExtractionGiveNothing() {
        FakeSidedInventory inventory = new FakeSidedInventory(2);
        inventory.with(0, new ItemStack(Blocks.dirt, 5));
        inventory.expose(0, 0)
            .noExtract();

        assertEquals(0, Inventories.count(inventory, ANY));
        assertNull(Inventories.extract(inventory, ANY, 1));
    }

    @Test
    void sidedInventoriesThatRefuseInsertionKeepTheStack() {
        FakeSidedInventory inventory = new FakeSidedInventory(2);
        inventory.expose(0, 0, 1)
            .noInsert();

        ItemStack rest = Inventories.insert(inventory, new ItemStack(Blocks.dirt, 5));
        assertNotNull(rest);
        assertEquals(5, rest.stackSize);
    }

    @Test
    void sidedInsertionUsesTheAccessibleSlots() {
        FakeSidedInventory inventory = new FakeSidedInventory(3);
        inventory.expose(0, 2);

        assertNull(Inventories.insert(inventory, new ItemStack(Blocks.dirt, 5)));
        assertNull(inventory.getStackInSlot(0));
        assertEquals(5, inventory.getStackInSlot(2).stackSize);
    }

    @Test
    void aSidedInventoryWithNoExposedSlotsAcceptsNothing() {
        FakeSidedInventory inventory = new FakeSidedInventory(3);
        ItemStack rest = Inventories.insert(inventory, new ItemStack(Blocks.dirt, 5));
        assertNotNull(rest);
        assertEquals(0, Inventories.count(inventory, ANY));
    }
}
