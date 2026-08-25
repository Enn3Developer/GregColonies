package com.enn3developer.gregcolonies.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandGuard;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

public class ColonyCommand extends CommandBase {

    private static final List<String> SUB_COMMANDS = Arrays.asList("list", "info", "spawn", "order", "guard", "cancel");

    @Override
    public String getCommandName() {
        return "colony";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/colony <list|info <id>|spawn [colonyId]|order <x> <y> <z> [colonyId]|guard [colonyId]|cancel [colonyId]>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB_COMMANDS.toArray(new String[0]));
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        World world = sender.getEntityWorld();
        ColonyManager manager = ColonyManager.get(world);

        if ("list".equals(args[0])) {
            if (manager.getColonyCount() == 0) {
                sender.addChatMessage(new ChatComponentText("No colonies"));
                return;
            }
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GOLD + "Colonies: " + manager.getColonyCount()));
            for (Colony colony : manager.getColonies()) {
                sender.addChatMessage(
                    new ChatComponentText(
                        "#" + colony.getId()
                            + " "
                            + colony.getName()
                            + " ("
                            + colony.getOwnerName()
                            + ") dim "
                            + colony.getDimension()
                            + " at "
                            + colony.getX()
                            + "/"
                            + colony.getY()
                            + "/"
                            + colony.getZ()));
            }
            return;
        }

        if ("info".equals(args[0])) {
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            Colony colony = manager.getColony(parseInt(sender, args[1]));
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No such colony"));
                return;
            }
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + colony.getName()));
            sender.addChatMessage(new ChatComponentText("id: " + colony.getId()));
            sender.addChatMessage(new ChatComponentText("owner: " + colony.getOwnerName() + " " + colony.getOwner()));
            sender.addChatMessage(new ChatComponentText("pending orders: " + colony.getOrderCount()));
            sender.addChatMessage(
                new ChatComponentText(
                    "center: dim " + colony
                        .getDimension() + " " + colony.getX() + "/" + colony.getY() + "/" + colony.getZ()));
            return;
        }

        if ("spawn".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int x;
            int y;
            int z;
            if (args.length >= 2) {
                x = colony.getX();
                y = colony.getY() + 1;
                z = colony.getZ();
            } else {
                ChunkCoordinates at = sender.getPlayerCoordinates();
                x = at.posX;
                y = at.posY;
                z = at.posZ;
            }

            EntityCitizen citizen = new EntityCitizen(world);
            citizen.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
            citizen.setColonyId(colony.getId());
            world.spawnEntityInWorld(citizen);

            sender.addChatMessage(
                new ChatComponentText(
                    "Spawned citizen at " + x + "/" + y + "/" + z + " for colony #" + colony.getId()));
            return;
        }

        if ("order".equals(args[0])) {
            if (args.length < 4) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            Colony colony = findColony(sender, manager, args.length >= 5 ? parseInt(sender, args[4]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int x = parseInt(sender, args[1]);
            int y = parseInt(sender, args[2]);
            int z = parseInt(sender, args[3]);
            manager.enqueueOrder(colony.getId(), new CitizenCommandMoveTo(x, y, z));

            sender.addChatMessage(
                new ChatComponentText(
                    "Queued move_to for colony #" + colony.getId()
                        + " ("
                        + colony.getOrderCount()
                        + " order(s) pending)"));
            return;
        }

        if ("guard".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            manager.enqueueOrder(colony.getId(), new CitizenCommandGuard());
            sender.addChatMessage(
                new ChatComponentText(
                    "Queued guard for colony #" + colony.getId()
                        + " ("
                        + colony.getOrderCount()
                        + " order(s) pending)"));
            return;
        }

        if ("cancel".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int cleared = manager.clearOrders(colony.getId());
            int stopped = 0;
            for (EntityCitizen citizen : findCitizens(world, colony.getId())) {
                citizen.getCommands()
                    .clear(citizen);
                stopped++;
            }
            sender.addChatMessage(
                new ChatComponentText("Dropped " + cleared + " pending order(s), stopped " + stopped + " citizen(s)"));
            return;
        }

        throw new WrongUsageException(getCommandUsage(sender));
    }

    private Colony findColony(ICommandSender sender, ColonyManager manager, int colonyId) {
        if (colonyId != 0) {
            return manager.getColony(colonyId);
        }
        if (!(sender instanceof EntityPlayer)) {
            return null;
        }
        ChunkCoordinates at = sender.getPlayerCoordinates();
        return manager.getNearestColonyOf(
            ((EntityPlayer) sender).getUniqueID(),
            sender.getEntityWorld().provider.dimensionId,
            at.posX,
            at.posZ);
    }

    private boolean canManage(ICommandSender sender, Colony colony) {
        if (!(sender instanceof EntityPlayer) || colony.canAccess((EntityPlayer) sender)) {
            return true;
        }
        sender.addChatMessage(
            new ChatComponentText(EnumChatFormatting.RED + "Colony #" + colony.getId() + " is not yours"));
        return false;
    }

    private List<EntityCitizen> findCitizens(World world, int colonyId) {
        List<EntityCitizen> citizens = new ArrayList<>();
        for (Object entity : world.loadedEntityList) {
            if (entity instanceof EntityCitizen && ((EntityCitizen) entity).getColonyId() == colonyId) {
                citizens.add((EntityCitizen) entity);
            }
        }
        return citizens;
    }
}
