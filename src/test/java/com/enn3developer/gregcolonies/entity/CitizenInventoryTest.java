package com.enn3developer.gregcolonies.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enn3developer.gregcolonies.testing.FakeInventory;
import com.enn3developer.gregcolonies.testing.MinecraftBootstrap;

class CitizenInventoryTest {

    private static final Predicate<ItemStack> DIRT = stack -> stack != null
        && net.minecraft.block.Block.getBlockFromItem(stack.getItem()) == Blocks.dirt;

    private CitizenInventory inventory;

    @BeforeAll
    static void boot() {
        MinecraftBootstrap.ensure();
    }

    @BeforeEach
    void freshInventory() {
        inventory = new CitizenInventory(null);
    }

    private void main(int slot, ItemStack stack) {
        inventory.getMain()
            .setStackInSlot(slot, stack);
    }

    @Test
    void armourOnlyFitsItsOwnSlot() {
        ItemStack helmet = new ItemStack(Items.iron_helmet);
        ItemStack boots = new ItemStack(Items.iron_boots);

        assertTrue(CitizenInventory.isArmor(helmet, 0, null));
        assertFalse(CitizenInventory.isArmor(helmet, 3, null));
        assertTrue(CitizenInventory.isArmor(boots, 3, null));
        assertFalse(CitizenInventory.isArmor(boots, 0, null));
    }

    @Test
    void nonArmourIsNeverArmour() {
        assertFalse(CitizenInventory.isArmor(new ItemStack(Blocks.dirt), 0, null));
        assertFalse(CitizenInventory.isArmor(null, 0, null));
    }

    @Test
    void foodIsRecognised() {
        assertTrue(CitizenInventory.isFood(new ItemStack(Items.apple)));
        assertTrue(CitizenInventory.isFood(new ItemStack(Items.bread)));
        assertFalse(CitizenInventory.isFood(new ItemStack(Blocks.dirt)));
        assertFalse(CitizenInventory.isFood(null));
    }

    @Test
    void toolsAreRecognised() {
        assertTrue(CitizenInventory.isTool(new ItemStack(Items.iron_pickaxe)));
        assertTrue(CitizenInventory.isTool(new ItemStack(Items.iron_axe)));
        assertTrue(CitizenInventory.isTool(new ItemStack(Items.iron_sword)));
        assertTrue(CitizenInventory.isTool(new ItemStack(Items.iron_hoe)));
    }

    @Test
    void itemsThatOnlyDeclareAToolClassAlsoCount() {
        Item declared = new Item();
        declared.setHarvestLevel("pickaxe", 1);
        ItemStack stack = new ItemStack(declared);

        assertFalse(declared instanceof ItemTool);
        assertEquals(
            1,
            declared.getToolClasses(stack)
                .size());
        assertTrue(CitizenInventory.isTool(stack));
    }

    @Test
    void itemsWithoutAToolClassAreNotTools() {
        ItemStack shears = new ItemStack(Items.shears);
        assertTrue(
            shears.getItem()
                .getToolClasses(shears)
                .isEmpty(),
            "shears are the no-tool-class case this test relies on");
        assertFalse(CitizenInventory.isTool(shears));
    }

    @Test
    void ordinaryItemsAreNotTools() {
        assertFalse(CitizenInventory.isTool(new ItemStack(Blocks.dirt)));
        assertFalse(CitizenInventory.isTool(new ItemStack(Items.apple)));
        assertFalse(CitizenInventory.isTool(null));
    }

    @Test
    void slotCountsAreWhatTheGuiExpects() {
        assertEquals(
            CitizenInventory.ARMOR_SLOTS,
            inventory.getArmor()
                .getSlots());
        assertEquals(
            CitizenInventory.FOOD_SLOTS,
            inventory.getFood()
                .getSlots());
        assertEquals(
            CitizenInventory.TOOL_SLOTS,
            inventory.getTool()
                .getSlots());
        assertEquals(
            CitizenInventory.MAIN_SLOTS,
            inventory.getMain()
                .getSlots());
    }

    @Test
    void theHeldToolIsTheToolSlot() {
        assertNull(inventory.getHeldTool());
        inventory.getTool()
            .setStackInSlot(0, new ItemStack(Items.iron_pickaxe));
        assertNotNull(inventory.getHeldTool());
    }

    @Test
    void storingFillsTheMainSlots() {
        assertNull(inventory.store(new ItemStack(Blocks.dirt, 10)));
        assertEquals(10, inventory.countMain(DIRT));
    }

    @Test
    void storingMoreThanFitsHandsBackTheRemainder() {
        for (int i = 0; i < CitizenInventory.MAIN_SLOTS; i++) {
            main(i, new ItemStack(Blocks.dirt, 64));
        }
        ItemStack rest = inventory.store(new ItemStack(Blocks.dirt, 5));
        assertNotNull(rest);
        assertEquals(5, rest.stackSize);
    }

    @Test
    void freeSlotsAreReported() {
        assertTrue(inventory.hasFreeMainSlot());
        for (int i = 0; i < CitizenInventory.MAIN_SLOTS; i++) {
            main(i, new ItemStack(Blocks.dirt, 1));
        }
        assertFalse(inventory.hasFreeMainSlot());
    }

    @Test
    void takeMainTakesOneAndLeavesTheRest() {
        main(0, new ItemStack(Blocks.dirt, 4));
        ItemStack taken = inventory.takeMain(DIRT);

        assertNotNull(taken);
        assertEquals(1, taken.stackSize);
        assertEquals(3, inventory.countMain(DIRT));
    }

    @Test
    void takeMainReturnsNullWhenNothingMatches() {
        main(0, new ItemStack(Blocks.stone, 4));
        assertNull(inventory.takeMain(DIRT));
    }

    @Test
    void peekMainCopiesWithoutRemoving() {
        main(0, new ItemStack(Blocks.dirt, 4));
        ItemStack peeked = inventory.peekMain(DIRT);

        assertNotNull(peeked);
        assertEquals(4, peeked.stackSize);
        assertEquals(4, inventory.countMain(DIRT));

        peeked.stackSize = 1;
        assertEquals(4, inventory.countMain(DIRT), "peeking must not hand out the live stack");
    }

    @Test
    void peekMainReturnsNullWhenNothingMatches() {
        assertNull(inventory.peekMain(DIRT));
    }

    @Test
    void countingAndCheckingScanEverySlot() {
        main(0, new ItemStack(Blocks.stone, 2));
        main(4, new ItemStack(Blocks.dirt, 7));
        main(8, new ItemStack(Blocks.dirt, 3));

        assertEquals(10, inventory.countMain(DIRT));
        assertTrue(inventory.hasMain(DIRT));
        assertFalse(inventory.hasMain(stack -> stack != null && stack.getItem() == Items.apple));
    }

    @Test
    void foodIsCountedAcrossTheFoodSlots() {
        assertEquals(0, inventory.countFood());
        inventory.getFood()
            .setStackInSlot(0, new ItemStack(Items.apple, 3));
        inventory.getFood()
            .setStackInSlot(2, new ItemStack(Items.bread, 2));
        assertEquals(5, inventory.countFood());
    }

    @Test
    void canStoreAcceptsWhatFits() {
        assertTrue(inventory.canStore(null));
        assertTrue(inventory.canStore(new ArrayList<>()));
        assertTrue(inventory.canStore(Arrays.asList(new ItemStack(Blocks.dirt, 64))));
    }

    @Test
    void canStoreRejectsWhatDoesNotFit() {
        for (int i = 0; i < CitizenInventory.MAIN_SLOTS; i++) {
            main(i, new ItemStack(Blocks.stone, 64));
        }
        assertFalse(inventory.canStore(Arrays.asList(new ItemStack(Blocks.dirt, 1))));
    }

    @Test
    void canStoreDoesNotActuallyStoreAnything() {
        List<ItemStack> incoming = Arrays.asList(new ItemStack(Blocks.dirt, 10));
        assertTrue(inventory.canStore(incoming));
        assertEquals(0, inventory.countMain(DIRT));
        assertEquals(10, incoming.get(0).stackSize);
    }

    @Test
    void canStoreCountsRoomAcrossSeveralStacks() {
        for (int i = 0; i < CitizenInventory.MAIN_SLOTS - 1; i++) {
            main(i, new ItemStack(Blocks.stone, 64));
        }
        assertTrue(inventory.canStore(Arrays.asList(new ItemStack(Blocks.dirt, 64))));
        assertFalse(
            inventory.canStore(Arrays.asList(new ItemStack(Blocks.dirt, 64), new ItemStack(Blocks.gravel, 64))));
    }

    @Test
    void canStoreIgnoresNullEntries() {
        assertTrue(inventory.canStore(Arrays.asList((ItemStack) null)));
    }

    @Test
    void depositingEmptiesTheMainSlots() {
        main(0, new ItemStack(Blocks.dirt, 10));
        main(1, new ItemStack(Blocks.stone, 5));
        FakeInventory chest = new FakeInventory(9);

        assertEquals(15, inventory.deposit(chest));
        assertEquals(0, inventory.countMain(stack -> true));
        assertEquals(15, chest.total());
        assertEquals(1, chest.getDirtied());
    }

    @Test
    void depositingIntoAFullChestKeepsWhatDoesNotFit() {
        main(0, new ItemStack(Blocks.dirt, 10));
        FakeInventory chest = new FakeInventory(1).with(0, new ItemStack(Blocks.dirt, 60));

        assertEquals(4, inventory.deposit(chest));
        assertEquals(6, inventory.countMain(DIRT));
        assertEquals(64, chest.total());
    }

    @Test
    void depositingNothingDoesNotDirtyTheChest() {
        FakeInventory chest = new FakeInventory(9);
        assertEquals(0, inventory.deposit(chest));
        assertEquals(0, chest.getDirtied());
    }

    @Test
    void depositingWithAReserveKeepsThatMany() {
        main(0, new ItemStack(Blocks.dirt, 40));
        main(1, new ItemStack(Blocks.dirt, 40));
        FakeInventory chest = new FakeInventory(9);

        assertEquals(30, inventory.deposit(chest, DIRT, 50));
        assertEquals(50, inventory.countMain(DIRT));
        assertEquals(30, chest.total());
    }

    @Test
    void aReserveDoesNotProtectOtherItems() {
        main(0, new ItemStack(Blocks.dirt, 10));
        main(1, new ItemStack(Blocks.stone, 10));
        FakeInventory chest = new FakeInventory(9);

        assertEquals(10, inventory.deposit(chest, DIRT, 64));
        assertEquals(10, inventory.countMain(DIRT));
        assertEquals(0, inventory.countMain(stack -> stack != null && !DIRT.test(stack)));
    }

    @Test
    void aReserveBiggerThanTheStockDepositsNothing() {
        main(0, new ItemStack(Blocks.dirt, 5));
        FakeInventory chest = new FakeInventory(9);

        assertEquals(0, inventory.deposit(chest, DIRT, 64));
        assertEquals(5, inventory.countMain(DIRT));
    }

    @Test
    void excessIsDetectedAgainstTheReserve() {
        main(0, new ItemStack(Blocks.dirt, 40));
        assertFalse(inventory.hasExcess(DIRT, 50));

        main(1, new ItemStack(Blocks.dirt, 40));
        assertTrue(inventory.hasExcess(DIRT, 50));
    }

    @Test
    void anythingNotKeptCountsAsExcess() {
        main(0, new ItemStack(Blocks.stone, 1));
        assertTrue(inventory.hasExcess(DIRT, 64));
    }

    @Test
    void anEmptyInventoryHasNoExcess() {
        assertFalse(inventory.hasExcess(DIRT, 0));
    }

    @Test
    void stockingFoodPullsUpToTheTarget() {
        FakeInventory chest = new FakeInventory(2).with(0, new ItemStack(Items.apple, 30));

        assertEquals(8, inventory.stockFood(chest, 8));
        assertEquals(8, inventory.countFood());
        assertEquals(22, chest.total());
        assertEquals(1, chest.getDirtied());
    }

    @Test
    void stockingFoodStopsWhenTheChestRunsDry() {
        FakeInventory chest = new FakeInventory(2).with(0, new ItemStack(Items.apple, 3));

        assertEquals(3, inventory.stockFood(chest, 8));
        assertEquals(3, inventory.countFood());
    }

    @Test
    void stockingFoodTakesNothingWhenAlreadyStocked() {
        inventory.getFood()
            .setStackInSlot(0, new ItemStack(Items.apple, 8));
        FakeInventory chest = new FakeInventory(2).with(0, new ItemStack(Items.apple, 30));

        assertEquals(0, inventory.stockFood(chest, 8));
        assertEquals(30, chest.total());
        assertEquals(0, chest.getDirtied());
    }

    @Test
    void stockingFoodIgnoresNonFood() {
        FakeInventory chest = new FakeInventory(2).with(0, new ItemStack(Blocks.dirt, 30));
        assertEquals(0, inventory.stockFood(chest, 8));
        assertEquals(0, inventory.countFood());
    }

    @Test
    void takeAllEmptiesEverySection() {
        inventory.getArmor()
            .setStackInSlot(0, new ItemStack(Items.iron_helmet));
        inventory.getFood()
            .setStackInSlot(0, new ItemStack(Items.apple, 2));
        inventory.getTool()
            .setStackInSlot(0, new ItemStack(Items.iron_pickaxe));
        main(0, new ItemStack(Blocks.dirt, 5));

        List<ItemStack> dropped = inventory.takeAll();

        assertEquals(4, dropped.size());
        assertNull(inventory.getHeldTool());
        assertEquals(0, inventory.countFood());
        assertEquals(0, inventory.countMain(stack -> true));
        assertTrue(
            inventory.takeAll()
                .isEmpty());
    }

    @Test
    void nbtRoundTrips() {
        inventory.getArmor()
            .setStackInSlot(0, new ItemStack(Items.iron_helmet));
        inventory.getFood()
            .setStackInSlot(1, new ItemStack(Items.bread, 4));
        inventory.getTool()
            .setStackInSlot(0, new ItemStack(Items.iron_pickaxe));
        main(3, new ItemStack(Blocks.dirt, 17));

        NBTTagCompound tag = new NBTTagCompound();
        inventory.writeToNBT(tag);

        CitizenInventory read = new CitizenInventory(null);
        read.readFromNBT(tag);

        assertEquals(
            Items.iron_helmet,
            read.getArmor()
                .getStackInSlot(0)
                .getItem());
        assertEquals(4, read.countFood());
        assertEquals(
            Items.iron_pickaxe,
            read.getHeldTool()
                .getItem());
        assertEquals(17, read.countMain(DIRT));
    }

    @Test
    void readingEmptyNbtGivesAnEmptyInventory() {
        CitizenInventory read = new CitizenInventory(null);
        read.readFromNBT(new NBTTagCompound());

        assertEquals(0, read.countFood());
        assertNull(read.getHeldTool());
        assertTrue(read.hasFreeMainSlot());
    }
}
