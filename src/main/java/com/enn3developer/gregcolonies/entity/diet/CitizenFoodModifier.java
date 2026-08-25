package com.enn3developer.gregcolonies.entity.diet;

import java.math.BigDecimal;

import net.minecraft.item.ItemStack;

import squeek.applecore.api.food.FoodValues;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.foodtracker.FoodHistory;
import squeek.spiceoflife.foodtracker.FoodModifier;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroup;
import squeek.spiceoflife.foodtracker.foodgroups.FoodGroupRegistry;
import squeek.spiceoflife.helpers.FoodHelper;
import squeek.spiceoflife.shadow.com.udojava.evalex.Expression;

public final class CitizenFoodModifier {

    private CitizenFoodModifier() {}

    public static float get(FoodHistory history, ItemStack food, int foodLevel, float saturation) {
        if (!ModConfig.FOOD_MODIFIER_ENABLED) {
            return 1.0F;
        }
        if (!FoodHelper.canFoodDiminish(food)) {
            return 1.0F;
        }
        if (ModConfig.FOOD_EATEN_THRESHOLD > 0 && history.totalFoodsEatenAllTime < ModConfig.FOOD_EATEN_THRESHOLD) {
            return 1.0F;
        }

        FoodGroup[] groups = FoodGroupRegistry.getFoodGroupsForFood(food)
            .toArray(new FoodGroup[0]);
        int iterations = Math.max(1, groups.length);
        float sum = 0.0F;
        for (int i = 0; i < iterations; i++) {
            sum += forGroup(history, food, i < groups.length ? groups[i] : null, foodLevel, saturation);
        }
        return sum / iterations;
    }

    private static float forGroup(FoodHistory history, ItemStack food, FoodGroup group, int foodLevel,
        float saturation) {
        FoodValues foodValues = FoodValues.get(food);
        if (foodValues == null) {
            return 0.0F;
        }

        FoodModifier modifier = group != null ? group.getFoodModifier() : FoodModifier.GLOBAL;
        FoodValues totals = history.getTotalFoodValuesForFoodGroup(food, group);
        Expression expression = modifier.expression;

        return expression.with("count", new BigDecimal(history.getFoodCountForFoodGroup(food, group)))
            .and("cur_history_length", new BigDecimal(history.getHistoryLength()))
            .and("food_hunger_value", new BigDecimal(foodValues.hunger))
            .and("food_saturation_mod", new BigDecimal(foodValues.saturationModifier))
            .and("cur_hunger", new BigDecimal(foodLevel))
            .and("cur_saturation", new BigDecimal(saturation))
            .and("total_food_eaten", new BigDecimal(history.totalFoodsEatenAllTime))
            .and("max_history_length", new BigDecimal(ModConfig.FOOD_HISTORY_LENGTH))
            .and("hunger_count", new BigDecimal(totals.hunger))
            .and("saturation_count", new BigDecimal(totals.saturationModifier))
            .and(
                "food_group_count",
                new BigDecimal(
                    FoodGroupRegistry.getFoodGroupsForFood(food)
                        .size()))
            .and(
                "distinct_food_groups_eaten",
                new BigDecimal(
                    history.getDistinctFoodGroups()
                        .size()))
            .and("total_food_groups", new BigDecimal(FoodGroupRegistry.numFoodGroups()))
            .and("exact_count", new BigDecimal(history.getFoodCountIgnoringFoodGroups(food)))
            .eval()
            .floatValue();
    }
}
