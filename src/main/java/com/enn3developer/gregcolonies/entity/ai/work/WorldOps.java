package com.enn3developer.gregcolonies.entity.ai.work;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public final class WorldOps {

    private static final int PICKUP_DELAY = 10;

    private static final float VOLUME_SCALE = 2.0F;

    private static final float PITCH_SCALE = 0.8F;

    private WorldOps() {}

    public static void place(World world, int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, 3);
        playPlace(world, block, x, y, z);
    }

    public static boolean stepUp(EntityCitizen citizen, int x, int y, int z, ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null) {
            return false;
        }
        World world = citizen.worldObj;
        citizen.getNavigator()
            .clearPathEntity();
        place(world, x, y, z, block, stack.getItemDamage());
        citizen.setPosition(x + 0.5D, y + 1.0D, z + 0.5D);
        citizen.motionY = 0.0D;
        citizen.fallDistance = 0.0F;
        citizen.swingItem();
        return true;
    }

    public static void playPlace(World world, Block block, int x, int y, int z) {
        world.playSoundEffect(
            x + 0.5D,
            y + 0.5D,
            z + 0.5D,
            block.stepSound.func_150496_b(),
            (block.stepSound.getVolume() + 1.0F) / VOLUME_SCALE,
            block.stepSound.getPitch() * PITCH_SCALE);
    }

    public static void dropAt(World world, int x, int y, int z, ItemStack stack) {
        EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack);
        item.delayBeforeCanPickup = PICKUP_DELAY;
        world.spawnEntityInWorld(item);
    }
}
