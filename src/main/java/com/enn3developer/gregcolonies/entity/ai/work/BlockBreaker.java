package com.enn3developer.gregcolonies.entity.ai.work;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.EntityCitizen;

public class BlockBreaker {

    private static final float BREAK_EXHAUSTION = 0.025F;

    private static final int SWING_INTERVAL = 6;

    private static final int BREAK_EFFECT = 2001;

    private int x;

    private int y;

    private int z;

    private boolean active;

    private int progress;

    private int required;

    public boolean isActive() {
        return active;
    }

    public boolean isAt(int x, int y, int z) {
        return active && this.x == x && this.y == y && this.z == z;
    }

    public void setTarget(EntityCitizen citizen, int x, int y, int z) {
        clear(citizen);
        this.x = x;
        this.y = y;
        this.z = z;
        this.active = true;
        this.progress = 0;
        this.required = 0;
    }

    public void clear(EntityCitizen citizen) {
        if (active && citizen != null) {
            citizen.worldObj.destroyBlockInWorldPartially(citizen.getEntityId(), x, y, z, -1);
        }
        active = false;
        progress = 0;
        required = 0;
    }

    public DigResult tick(EntityCitizen citizen, boolean collect) {
        World world = citizen.worldObj;
        Block block = world.getBlock(x, y, z);
        if (block == null || block.isAir(world, x, y, z)) {
            clear(citizen);
            return DigResult.GONE;
        }

        int meta = world.getBlockMetadata(x, y, z);
        ItemStack tool = citizen.getInventory()
            .getHeldTool();
        if (!WorkBlocks.canHarvest(tool, block, meta)) {
            clear(citizen);
            return DigResult.UNBREAKABLE;
        }
        if (required <= 0) {
            required = WorkBlocks.digTicks(tool, world, block, meta, x, y, z);
            if (required < 0) {
                clear(citizen);
                return DigResult.UNBREAKABLE;
            }
        }

        if (progress % SWING_INTERVAL == 0) {
            citizen.swingItem();
        }
        progress++;
        if (progress < required) {
            world.destroyBlockInWorldPartially(citizen.getEntityId(), x, y, z, progress * 10 / required);
            return DigResult.PROGRESS;
        }

        int fortune = EnchantmentHelper.getFortuneModifier(citizen);
        ArrayList<ItemStack> drops = block.getDrops(world, x, y, z, meta, fortune);
        if (collect && !citizen.getInventory()
            .canStore(drops)) {
            clear(citizen);
            return DigResult.INVENTORY_FULL;
        }
        breakBlock(citizen, world, block, meta);
        for (ItemStack drop : drops) {
            ItemStack rest = citizen.getInventory()
                .store(drop);
            if (rest != null) {
                dropAt(world, x, y, z, rest);
            }
        }

        citizen.getDiet()
            .addExhaustion(BREAK_EXHAUSTION);
        if (tool != null) {
            tool.getItem()
                .onBlockDestroyed(tool, world, block, x, y, z, citizen);
            if (tool.stackSize <= 0) {
                citizen.getInventory()
                    .getTool()
                    .setStackInSlot(0, null);
                clear(citizen);
                return DigResult.TOOL_BROKEN;
            }
        }
        clear(citizen);
        return DigResult.BROKEN;
    }

    private static void dropAt(World world, int x, int y, int z, ItemStack stack) {
        EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack);
        item.delayBeforeCanPickup = 10;
        world.spawnEntityInWorld(item);
    }

    private void breakBlock(EntityCitizen citizen, World world, Block block, int meta) {
        world.playAuxSFX(BREAK_EFFECT, x, y, z, Block.getIdFromBlock(block) + (meta << 12));
        world.destroyBlockInWorldPartially(citizen.getEntityId(), x, y, z, -1);
        world.setBlockToAir(x, y, z);
    }
}
