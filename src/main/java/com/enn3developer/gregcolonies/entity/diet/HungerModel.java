package com.enn3developer.gregcolonies.entity.diet;

import net.minecraft.nbt.NBTTagCompound;

public class HungerModel {

    public static final int MAX_FOOD_LEVEL = 20;

    private static final float MAX_EXHAUSTION_LEVEL = 4.0F;

    private static final float EXHAUSTION_CAP = 40.0F;

    private static final float START_SATURATION = 5.0F;

    private int foodLevel = MAX_FOOD_LEVEL;

    private float saturation = START_SATURATION;

    private float exhaustion;

    private int timer;

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public boolean isEmpty() {
        return foodLevel <= 0;
    }

    public boolean isAtLeast(int level) {
        return foodLevel >= level;
    }

    public boolean isBelow(int level) {
        return foodLevel <= level;
    }

    public void addExhaustion(float amount) {
        exhaustion = Math.min(exhaustion + amount, EXHAUSTION_CAP);
    }

    public int drain(boolean starves) {
        if (exhaustion <= MAX_EXHAUSTION_LEVEL) {
            return 0;
        }
        exhaustion -= MAX_EXHAUSTION_LEVEL;
        if (saturation > 0.0F) {
            saturation = Math.max(saturation - 1.0F, 0.0F);
            return 0;
        }
        if (!starves) {
            return 0;
        }
        foodLevel = Math.max(foodLevel - 1, 0);
        return 1;
    }

    public void eat(int hunger, float saturationGain) {
        foodLevel = Math.min(foodLevel + hunger, MAX_FOOD_LEVEL);
        saturation = Math.min(saturation + saturationGain, foodLevel);
    }

    public boolean tick(int period) {
        if (++timer < period) {
            return false;
        }
        timer = 0;
        return true;
    }

    public void resetTimer() {
        timer = 0;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("foodLevel", foodLevel);
        tag.setFloat("saturation", saturation);
        tag.setFloat("exhaustion", exhaustion);
        tag.setInteger("foodTimer", timer);
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("foodLevel")) {
            foodLevel = tag.getInteger("foodLevel");
        }
        saturation = tag.getFloat("saturation");
        exhaustion = tag.getFloat("exhaustion");
        timer = tag.getInteger("foodTimer");
    }
}
