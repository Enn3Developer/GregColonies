package com.enn3developer.gregcolonies.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import tconstruct.library.tools.AbilityHelper;
import tconstruct.library.tools.ToolCore;

public final class TinkersTools {

    private static final String ROOT_TAG = "InfiTool";

    private static final String BROKEN_TAG = "Broken";

    private TinkersTools() {}

    public static boolean isTool(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ToolCore;
    }

    public static boolean isBroken(ItemStack stack) {
        if (!isTool(stack) || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tags = stack.getTagCompound()
            .getCompoundTag(ROOT_TAG);
        return tags != null && tags.getBoolean(BROKEN_TAG);
    }

    public static boolean attack(ItemStack stack, EntityLivingBase attacker, Entity target) {
        return AbilityHelper.onLeftClickEntity(stack, attacker, target, (ToolCore) stack.getItem());
    }
}
