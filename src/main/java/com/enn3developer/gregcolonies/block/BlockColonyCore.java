package com.enn3developer.gregcolonies.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;

public class BlockColonyCore extends BlockContainer {

    public static final String NAME = "colony_core";

    public BlockColonyCore() {
        super(Material.rock);
        setBlockName(GregColonies.MODID + "." + NAME);
        setBlockTextureName(GregColonies.MODID + ":" + NAME);
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileColonyCore();
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (world.isRemote || !(placer instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) placer;
        ColonyRegistry manager = ColonyManager.registry(world);
        int dimension = world.provider.dimensionId;

        Colony nearest = manager.getNearestColony(dimension, x, z);
        int minDistance = Config.minColonyDistance;
        if (nearest != null && nearest.distanceSqTo(dimension, x, z) < (double) minDistance * minDistance) {
            Chat.error(
                player,
                "Too close to colony " + nearest.getName()
                    + ", colonies must be at least "
                    + minDistance
                    + " blocks apart");
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
            return;
        }

        String name = stack.hasDisplayName() ? stack.getDisplayName() : player.getCommandSenderName() + "'s colony";
        Colony colony = manager
            .createColony(name, player.getUniqueID(), player.getCommandSenderName(), dimension, x, y, z);

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileColonyCore) {
            ((TileColonyCore) tile).setColonyId(colony.getId());
        }

        GregColonies.LOG.info("Created " + colony);
        Chat.info(
            player,
            EnumChatFormatting.GREEN + "Founded colony " + colony.getName() + " (#" + colony.getId() + ")");
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (!world.isRemote && !player.capabilities.isCreativeMode) {
            Colony colony = colonyAt(world, x, y, z);
            if (colony != null && !colony.canAccess(player)) {
                Chat.error(player, "Only " + colony.getOwnerName() + " can remove this colony core");
                return false;
            }
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }

    private static Colony colonyAt(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileColonyCore)) {
            return null;
        }
        return ColonyManager.registry(world)
            .getColony(((TileColonyCore) tile).getColonyId());
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileColonyCore) {
                int colonyId = ((TileColonyCore) tile).getColonyId();
                ColonyRegistry manager = ColonyManager.registry(world);
                Colony colony = manager.getColony(colonyId);
                if (colony != null && manager.removeColony(colonyId)) {
                    GregColonies.LOG.info("Removed " + colony);
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
