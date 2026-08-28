package com.enn3developer.gregcolonies.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.colony.CitizenControl;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyActions;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.LiveCitizens;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandGuard;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;
import com.enn3developer.gregcolonies.network.GCNetwork;

public class ColonyCommand extends CommandBase {

    private static final List<String> GROUP_SUB_COMMANDS = Arrays
        .asList("list", "set", "clear", "order", "guard", "cancel");

    private static final int DEFAULT_GROUP_RADIUS = 16;

    private static final List<String> SUB_COMMANDS = Arrays
        .asList("list", "info", "spawn", "order", "guard", "group", "cancel");

    @Override
    public String getCommandName() {
        return "colony";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/colony <list|info <id>|spawn [colonyId]|order <x> <y> <z> [colonyId]|guard [colonyId]|group ...|cancel [colonyId]>";
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
        if (args.length == 2 && "group".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, GROUP_SUB_COMMANDS.toArray(new String[0]));
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        World world = sender.getEntityWorld();
        ColonyRegistry manager = ColonyManager.registry(world);

        if ("list".equals(args[0])) {
            if (manager.getColonyCount() == 0) {
                Chat.info(sender, "No colonies");
                return;
            }
            Chat.info(sender, EnumChatFormatting.GOLD + "Colonies: " + manager.getColonyCount());
            for (Colony colony : manager.getColonies()) {
                Chat.info(
                    sender,
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
                        + colony.getZ());
            }
            return;
        }

        if ("info".equals(args[0])) {
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            Colony colony = manager.getColony(parseInt(sender, args[1]));
            if (colony == null) {
                Chat.error(sender, "No such colony");
                return;
            }
            Chat.info(sender, EnumChatFormatting.GOLD + colony.getName());
            Chat.info(sender, "id: " + colony.getId());
            Chat.info(sender, "owner: " + colony.getOwnerName() + " " + colony.getOwner());
            Chat.info(sender, "citizens: " + colony.getCitizenCount());
            Chat.info(sender, "pending orders: " + colony.getOrderCount());
            Chat.info(
                sender,
                "center: dim " + colony
                    .getDimension() + " " + colony.getX() + "/" + colony.getY() + "/" + colony.getZ());
            return;
        }

        if ("spawn".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int x;
            int y;
            int z;
            if (args.length >= 2) {
                if (colony.getDimension() != world.provider.dimensionId) {
                    Chat.error(sender, "Colony #" + colony.getId() + " is in dim " + colony.getDimension());
                    return;
                }
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
            citizen.ensureName();
            world.spawnEntityInWorld(citizen);

            Chat.info(
                sender,
                "Spawned " + citizen
                    .getCitizenName() + " at " + x + "/" + y + "/" + z + " for colony #" + colony.getId());
            return;
        }

        if ("order".equals(args[0])) {
            if (args.length < 4) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            Colony colony = findColony(sender, manager, args.length >= 5 ? parseInt(sender, args[4]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int x = parseInt(sender, args[1]);
            int y = parseInt(sender, args[2]);
            int z = parseInt(sender, args[3]);
            Chat.tell(sender, ColonyActions.enqueueOrder(manager, colony, new CitizenCommandMoveTo(x, y, z), ""));
            return;
        }

        if ("guard".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            Chat.tell(sender, ColonyActions.enqueueOrder(manager, colony, new CitizenCommandGuard(), ""));
            return;
        }

        if ("group".equals(args[0])) {
            processGroup(sender, args, world, manager);
            return;
        }

        if ("cancel".equals(args[0])) {
            Colony colony = findColony(sender, manager, args.length >= 2 ? parseInt(sender, args[1]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            Chat.tell(sender, ColonyActions.cancelOrders(manager, colony, "", control(world, colony, null)));
            return;
        }

        throw new WrongUsageException(getCommandUsage(sender));
    }

    private void processGroup(ICommandSender sender, String[] args, World world, ColonyRegistry manager) {
        if (args.length < 2) {
            throw new WrongUsageException(getGroupUsage());
        }

        if ("list".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 3 ? parseInt(sender, args[2]) : 0);
            if (colony == null || !canManage(sender, colony)) {
                if (colony == null) {
                    Chat.error(sender, "No colony found");
                }
                return;
            }
            Map<String, Integer> counts = new TreeMap<>();
            for (ColonyCitizen citizen : colony.getCitizens()) {
                String group = citizen.getGroup()
                    .isEmpty() ? "(none)" : citizen.getGroup();
                counts.put(group, counts.getOrDefault(group, 0) + 1);
            }
            if (counts.isEmpty()) {
                Chat.info(sender, "No citizens");
                return;
            }
            sender
                .addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Groups of colony #" + colony.getId()));
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                Chat.info(
                    sender,
                    entry.getKey() + ": "
                        + entry.getValue()
                        + " citizen(s), "
                        + colony.getOrderCount("(none)".equals(entry.getKey()) ? "" : entry.getKey())
                        + " order(s) pending");
            }
            return;
        }

        if ("set".equals(args[1]) || "clear".equals(args[1])) {
            boolean clearing = "clear".equals(args[1]);
            if (!clearing && args.length < 3) {
                throw new WrongUsageException(getGroupUsage());
            }
            if (!(sender instanceof EntityPlayer)) {
                Chat.error(sender, "Only a player can do this");
                return;
            }

            String group = clearing ? "" : args[2];
            int radiusArg = clearing ? 2 : 3;
            int radius = args.length > radiusArg ? parseInt(sender, args[radiusArg]) : DEFAULT_GROUP_RADIUS;

            Colony colony = findColony(sender, manager, 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            Chat.tell(
                sender,
                ColonyActions.assignGroup(
                    manager,
                    colony,
                    group,
                    world.provider.dimensionId,
                    player.posX,
                    player.posZ,
                    radius,
                    control(world, colony, player)));
            return;
        }

        if (args.length < 3) {
            throw new WrongUsageException(getGroupUsage());
        }
        String group = args[2];

        if ("order".equals(args[1])) {
            if (args.length < 6) {
                throw new WrongUsageException(getGroupUsage());
            }
            Colony colony = findColony(sender, manager, args.length >= 7 ? parseInt(sender, args[6]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            CitizenCommandMoveTo command = new CitizenCommandMoveTo(
                parseInt(sender, args[3]),
                parseInt(sender, args[4]),
                parseInt(sender, args[5]));
            Chat.tell(sender, ColonyActions.enqueueOrder(manager, colony, command, group));
            return;
        }

        if ("guard".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 4 ? parseInt(sender, args[3]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            Chat.tell(sender, ColonyActions.enqueueOrder(manager, colony, new CitizenCommandGuard(), group));
            return;
        }

        if ("cancel".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 4 ? parseInt(sender, args[3]) : 0);
            if (colony == null) {
                Chat.error(sender, "No colony found");
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            Chat.tell(sender, ColonyActions.cancelOrders(manager, colony, group, control(world, colony, null)));
            return;
        }

        throw new WrongUsageException(getGroupUsage());
    }

    private String getGroupUsage() {
        return "/colony group <list [colonyId]|set <group> [radius]|clear [radius]|order <group> <x> <y> <z> [colonyId]"
            + "|guard <group> [colonyId]|cancel <group> [colonyId]>";
    }

    private Colony findColony(ICommandSender sender, ColonyRegistry manager, int colonyId) {
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
        Chat.error(sender, "Colony #" + colony.getId() + " is not yours");
        return false;
    }

    private CitizenControl control(World world, Colony colony, Entity actor) {
        return new LiveCitizens(GCNetwork.loadedCitizens(world, colony.getId()), actor);
    }
}
