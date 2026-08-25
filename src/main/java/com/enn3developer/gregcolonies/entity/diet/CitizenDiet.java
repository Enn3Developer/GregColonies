package com.enn3developer.gregcolonies.entity.diet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;

import com.enn3developer.gregcolonies.entity.CitizenInventory;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import ca.wescook.nutrition.NutritionConfig;
import ca.wescook.nutrition.effects.Effect;
import ca.wescook.nutrition.effects.EffectsList;
import ca.wescook.nutrition.nutrients.Nutrient;
import ca.wescook.nutrition.nutrients.NutrientList;
import ca.wescook.nutrition.nutrients.NutrientUtils;
import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.foodtracker.FoodEaten;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.helpers.FoodHelper;

public class CitizenDiet {

    public static final int MAX_FOOD_LEVEL = 20;

    private static final float MAX_NUTRIENT = 100.0F;

    private static final float MAX_EXHAUSTION_LEVEL = 4.0F;

    private static final float EXHAUSTION_CAP = 40.0F;

    private static final float HUNGER_POTION_EXHAUSTION = 0.025F;

    private static final float DECAY_SCALE = 0.075F;

    private static final int EAT_THRESHOLD = 14;

    private static final int EAT_COOLDOWN = 32;

    private static final int EFFECT_INTERVAL = 110;

    private static final int EFFECT_DURATION = 619;

    private static final int FOOD_TIMER_PERIOD = 80;

    private static final int HEAL_FOOD_LEVEL = 18;

    private static final float HEAL_EXHAUSTION = 3.0F;

    private final Map<String, Float> nutrients = new HashMap<>();
    private final FoodHistory history = new FoodHistory();
    private int foodLevel = MAX_FOOD_LEVEL;
    private float saturation = 5.0F;
    private float exhaustion;
    private int eatCooldown;
    private int effectTimer;
    private int foodTimer;

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public FoodHistory getHistory() {
        return history;
    }

    public float get(Nutrient nutrient) {
        Float value = nutrients.get(nutrient.name);
        return value == null ? NutritionConfig.nutrition.startingNutrition : value;
    }

    public void set(Nutrient nutrient, float value) {
        nutrients.put(nutrient.name, MathHelper.clamp_float(value, 0.0F, MAX_NUTRIENT));
    }

    public void addExhaustion(float amount) {
        exhaustion = Math.min(exhaustion + amount, EXHAUSTION_CAP);
    }

    public void update(EntityCitizen citizen) {
        EnumDifficulty difficulty = citizen.worldObj.difficultySetting;

        PotionEffect hunger = citizen.getActivePotionEffect(Potion.hunger);
        if (hunger != null) {
            addExhaustion(HUNGER_POTION_EXHAUSTION * (hunger.getAmplifier() + 1));
        }

        if (exhaustion > MAX_EXHAUSTION_LEVEL) {
            exhaustion -= MAX_EXHAUSTION_LEVEL;
            if (saturation > 0.0F) {
                saturation = Math.max(saturation - 1.0F, 0.0F);
            } else if (difficulty != EnumDifficulty.PEACEFUL) {
                foodLevel = Math.max(foodLevel - 1, 0);
                decay(1);
            }
        }

        if (citizen.worldObj.getGameRules()
            .getGameRuleBooleanValue("naturalRegeneration") && foodLevel >= HEAL_FOOD_LEVEL && shouldHeal(citizen)) {
            if (++foodTimer >= FOOD_TIMER_PERIOD) {
                foodTimer = 0;
                citizen.heal(1.0F);
                addExhaustion(HEAL_EXHAUSTION);
            }
        } else if (foodLevel <= 0) {
            if (++foodTimer >= FOOD_TIMER_PERIOD) {
                foodTimer = 0;
                if (citizen.getHealth() > 10.0F || difficulty == EnumDifficulty.HARD
                    || citizen.getHealth() > 1.0F && difficulty == EnumDifficulty.NORMAL) {
                    citizen.attackEntityFrom(DamageSource.starve, 1.0F);
                }
            }
        } else {
            foodTimer = 0;
        }

        if (eatCooldown > 0) {
            eatCooldown--;
        } else if (foodLevel <= EAT_THRESHOLD) {
            tryEat(citizen);
        }

        if (++effectTimer > EFFECT_INTERVAL) {
            effectTimer = 0;
            applyEffects(citizen);
        }
    }

    private boolean shouldHeal(EntityCitizen citizen) {
        return citizen.getHealth() > 0.0F && citizen.getHealth() < citizen.getMaxHealth();
    }

    private void tryEat(EntityCitizen citizen) {
        CitizenInventory inventory = citizen.getInventory();
        int bestSlot = -1;
        FoodValues bestValues = null;

        for (int slot = 0; slot < inventory.getFood()
            .getSlots(); slot++) {
            ItemStack stack = inventory.getFood()
                .getStackInSlot(slot);
            if (stack == null || !FoodHelper.isFood(stack)) {
                continue;
            }
            FoodValues base = FoodHelper.getFoodValues(stack);
            if (base == null || base.hunger <= 0) {
                continue;
            }
            float modifier = CitizenFoodModifier.get(history, stack, foodLevel, saturation);
            FoodValues modified = FoodModifier.getModifiedFoodValues(base, modifier);
            if (modified.hunger <= 0) {
                continue;
            }
            if (bestValues == null || modified.hunger > bestValues.hunger) {
                bestSlot = slot;
                bestValues = modified;
            }
        }

        if (bestSlot < 0) {
            return;
        }

        ItemStack stack = inventory.getFood()
            .getStackInSlot(bestSlot);
        ItemStack eaten = stack.copy();
        eaten.stackSize = 1;

        foodLevel = Math.min(foodLevel + bestValues.hunger, MAX_FOOD_LEVEL);
        saturation = Math.min(saturation + bestValues.getSaturationIncrement(), foodLevel);

        FoodEaten foodEaten = new FoodEaten(eaten);
        foodEaten.foodValues = bestValues;
        history.addFood(foodEaten);

        List<Nutrient> found = NutrientUtils.getFoodNutrients(eaten);
        if (!found.isEmpty()) {
            float value = NutrientUtils.calculateNutrition(bestValues, found);
            for (Nutrient nutrient : found) {
                set(nutrient, get(nutrient) + value);
            }
        }

        stack.stackSize--;
        if (stack.stackSize <= 0) {
            inventory.getFood()
                .setStackInSlot(bestSlot, null);
        }
        eatCooldown = EAT_COOLDOWN;
    }

    private void decay(int hungerLost) {
        if (!NutritionConfig.decay.enable) {
            return;
        }
        for (Nutrient nutrient : nutrientList()) {
            set(nutrient, get(nutrient) - hungerLost * DECAY_SCALE * nutrient.decay);
        }
    }

    private void applyEffects(EntityCitizen citizen) {
        Map<Potion, Integer> strongest = new HashMap<>();
        for (Effect effect : EffectsList.get()) {
            Integer amplifier = amplifierFor(effect);
            if (amplifier == null) {
                continue;
            }
            Integer current = strongest.get(effect.potion);
            if (current == null || amplifier > current) {
                strongest.put(effect.potion, amplifier);
            }
        }
        for (Map.Entry<Potion, Integer> entry : strongest.entrySet()) {
            citizen.addPotionEffect(new PotionEffect(entry.getKey().id, EFFECT_DURATION, entry.getValue()));
        }
    }

    private Integer amplifierFor(Effect effect) {
        List<Nutrient> targets = effect.nutrients;
        if (targets == null || targets.isEmpty()) {
            return null;
        }

        if (effect.detectionType == Effect.EnumDetectionType.ANY) {
            for (Nutrient nutrient : targets) {
                if (inRange(effect, get(nutrient))) {
                    return effect.amplifier;
                }
            }
            return null;
        }

        if (effect.detectionType == Effect.EnumDetectionType.ALL) {
            for (Nutrient nutrient : targets) {
                if (!inRange(effect, get(nutrient))) {
                    return null;
                }
            }
            return effect.amplifier;
        }

        if (effect.detectionType == Effect.EnumDetectionType.AVERAGE) {
            float total = 0.0F;
            for (Nutrient nutrient : targets) {
                total += get(nutrient);
            }
            return inRange(effect, total / targets.size()) ? effect.amplifier : null;
        }

        int count = 0;
        for (Nutrient nutrient : targets) {
            if (inRange(effect, get(nutrient))) {
                count++;
            }
        }
        return count > 0 ? count * effect.cumulativeModifier - 1 : null;
    }

    private boolean inRange(Effect effect, float value) {
        return value >= effect.minimum && value <= effect.maximum;
    }

    private static List<Nutrient> nutrientList() {
        List<Nutrient> list = NutrientList.get();
        return list == null ? Collections.emptyList() : list;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("foodLevel", foodLevel);
        tag.setFloat("saturation", saturation);
        tag.setFloat("exhaustion", exhaustion);
        tag.setInteger("foodTimer", foodTimer);
        tag.setInteger("totalFoodsEaten", history.totalFoodsEatenAllTime);

        NBTTagCompound nutrientTag = new NBTTagCompound();
        for (Map.Entry<String, Float> entry : nutrients.entrySet()) {
            nutrientTag.setFloat(entry.getKey(), entry.getValue());
        }
        tag.setTag("nutrients", nutrientTag);

        NBTTagList historyTag = new NBTTagList();
        for (FoodEaten foodEaten : history.getRecentHistory()) {
            NBTTagCompound entry = new NBTTagCompound();
            foodEaten.writeToNBTData(entry);
            historyTag.appendTag(entry);
        }
        tag.setTag("history", historyTag);
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("foodLevel")) {
            foodLevel = tag.getInteger("foodLevel");
        }
        saturation = tag.getFloat("saturation");
        exhaustion = tag.getFloat("exhaustion");
        foodTimer = tag.getInteger("foodTimer");

        nutrients.clear();
        NBTTagCompound nutrientTag = tag.getCompoundTag("nutrients");
        for (Object key : nutrientTag.func_150296_c()) {
            nutrients.put((String) key, nutrientTag.getFloat((String) key));
        }

        history.reset();
        NBTTagList historyTag = tag.getTagList("history", 10);
        for (int i = 0; i < historyTag.tagCount(); i++) {
            FoodEaten foodEaten = FoodEaten.loadFromNBTData(historyTag.getCompoundTagAt(i));
            if (foodEaten.itemStack != null) {
                history.addFoodRecent(foodEaten);
            }
        }
        history.totalFoodsEatenAllTime = tag.getInteger("totalFoodsEaten");
    }
}
