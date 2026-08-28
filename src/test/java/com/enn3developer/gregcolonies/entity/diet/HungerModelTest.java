package com.enn3developer.gregcolonies.entity.diet;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class HungerModelTest {

    @Test
    void startsFull() {
        HungerModel hunger = new HungerModel();
        assertEquals(HungerModel.MAX_FOOD_LEVEL, hunger.getFoodLevel());
        assertEquals(5.0F, hunger.getSaturation());
        assertFalse(hunger.isEmpty());
        assertTrue(hunger.isAtLeast(HungerModel.MAX_FOOD_LEVEL));
    }

    @Test
    void drainingNeedsExhaustionAboveTheThreshold() {
        HungerModel hunger = new HungerModel();
        hunger.addExhaustion(4.0F);
        assertEquals(0, hunger.drain(true));
        assertEquals(5.0F, hunger.getSaturation());
    }

    @Test
    void saturationIsSpentBeforeFood() {
        HungerModel hunger = new HungerModel();
        hunger.addExhaustion(4.5F);
        assertEquals(0, hunger.drain(true));
        assertEquals(4.0F, hunger.getSaturation());
        assertEquals(HungerModel.MAX_FOOD_LEVEL, hunger.getFoodLevel());
    }

    @Test
    void foodDropsOnceSaturationIsGone() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 5; i++) {
            hunger.addExhaustion(4.5F);
            hunger.drain(true);
        }
        assertEquals(0.0F, hunger.getSaturation());

        hunger.addExhaustion(4.5F);
        assertEquals(1, hunger.drain(true));
        assertEquals(HungerModel.MAX_FOOD_LEVEL - 1, hunger.getFoodLevel());
    }

    @Test
    void peacefulNeverLosesFood() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 40; i++) {
            hunger.addExhaustion(4.5F);
            assertEquals(0, hunger.drain(false));
        }
        assertEquals(HungerModel.MAX_FOOD_LEVEL, hunger.getFoodLevel());
    }

    @Test
    void foodNeverGoesBelowZero() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 200; i++) {
            hunger.addExhaustion(4.5F);
            hunger.drain(true);
        }
        assertEquals(0, hunger.getFoodLevel());
        assertTrue(hunger.isEmpty());
    }

    @Test
    void exhaustionIsCapped() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 100; i++) {
            hunger.addExhaustion(10.0F);
        }

        int lost = 0;
        for (int i = 0; i < 50; i++) {
            lost += hunger.drain(true);
        }

        assertEquals(0.0F, hunger.getSaturation());
        assertEquals(4, lost, "a full exhaustion bar should never cost more than four food");
        assertEquals(HungerModel.MAX_FOOD_LEVEL - 4, hunger.getFoodLevel());
    }

    @Test
    void eatingIsCappedAtMaximum() {
        HungerModel hunger = new HungerModel();
        hunger.eat(10, 10.0F);
        assertEquals(HungerModel.MAX_FOOD_LEVEL, hunger.getFoodLevel());
    }

    @Test
    void saturationNeverExceedsFoodLevel() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 20; i++) {
            hunger.addExhaustion(4.5F);
            hunger.drain(true);
        }
        int level = hunger.getFoodLevel();
        hunger.eat(0, 20.0F);
        assertEquals(level, hunger.getSaturation());
    }

    @Test
    void eatingRestoresAfterStarving() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 20; i++) {
            hunger.addExhaustion(4.5F);
            hunger.drain(true);
        }
        int before = hunger.getFoodLevel();
        hunger.eat(4, 2.4F);
        assertEquals(Math.min(before + 4, HungerModel.MAX_FOOD_LEVEL), hunger.getFoodLevel());
    }

    @Test
    void isBelowIsInclusive() {
        HungerModel hunger = new HungerModel();
        assertTrue(hunger.isBelow(20));
        assertFalse(hunger.isBelow(19));
    }

    @Test
    void tickFiresOnThePeriodThenRestarts() {
        HungerModel hunger = new HungerModel();
        assertFalse(hunger.tick(3));
        assertFalse(hunger.tick(3));
        assertTrue(hunger.tick(3));
        assertFalse(hunger.tick(3));
    }

    @Test
    void resetTimerPushesTheNextTickBack() {
        HungerModel hunger = new HungerModel();
        hunger.tick(3);
        hunger.tick(3);
        hunger.resetTimer();
        assertFalse(hunger.tick(3));
        assertFalse(hunger.tick(3));
        assertTrue(hunger.tick(3));
    }

    @Test
    void nbtRoundTrips() {
        HungerModel hunger = new HungerModel();
        for (int i = 0; i < 3; i++) {
            hunger.addExhaustion(4.5F);
            hunger.drain(true);
        }
        for (int i = 0; i < 9; i++) {
            hunger.tick(10);
        }

        NBTTagCompound tag = new NBTTagCompound();
        hunger.writeToNBT(tag);

        HungerModel read = new HungerModel();
        read.readFromNBT(tag);
        assertEquals(hunger.getFoodLevel(), read.getFoodLevel());
        assertEquals(2.0F, read.getSaturation());
        assertTrue(read.tick(10), "the food timer must survive the save");

        read.addExhaustion(3.0F);
        assertEquals(0, read.drain(true));
        assertEquals(1.0F, read.getSaturation(), "the exhaustion bar must survive the save");
    }

    @Test
    void missingFoodLevelKeepsTheDefault() {
        HungerModel read = new HungerModel();
        read.readFromNBT(new NBTTagCompound());
        assertEquals(HungerModel.MAX_FOOD_LEVEL, read.getFoodLevel());
    }
}
