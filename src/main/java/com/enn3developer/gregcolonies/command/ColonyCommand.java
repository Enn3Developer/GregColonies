package com.enn3developer.gregcolonies.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandGuard;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

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
            sender.addChatMessage(new ChatComponentText("citizens: " + colony.getCitizenCount()));
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
                if (colony.getDimension() != world.provider.dimensionId) {
                    sender.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.RED + "Colony #"
                                + colony.getId()
                                + " is in dim "
                                + colony.getDimension()));
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

            sender.addChatMessage(
                new ChatComponentText(
                    "Spawned " + citizen
                        .getCitizenName() + " at " + x + "/" + y + "/" + z + " for colony #" + colony.getId()));
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

        if ("group".equals(args[0])) {
            processGroup(sender, args, world, manager);
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

    private void processGroup(ICommandSender sender, String[] args, World world, ColonyManager manager) {
        if (args.length < 2) {
            throw new WrongUsageException(getGroupUsage());
        }

        if ("list".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 3 ? parseInt(sender, args[2]) : 0);
            if (colony == null || !canManage(sender, colony)) {
                if (colony == null) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
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
                sender.addChatMessage(new ChatComponentText("No citizens"));
                return;
            }
            sender
                .addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "Groups of colony #" + colony.getId()));
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                sender.addChatMessage(
                    new ChatComponentText(
                        entry.getKey() + ": "
                            + entry.getValue()
                            + " citizen(s), "
                            + colony.getOrderCount("(none)".equals(entry.getKey()) ? "" : entry.getKey())
                            + " order(s) pending"));
            }
            return;
        }

        if ("set".equals(args[1]) || "clear".equals(args[1])) {
            boolean clearing = "clear".equals(args[1]);
            if (!clearing && args.length < 3) {
                throw new WrongUsageException(getGroupUsage());
            }
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Only a player can do this"));
                return;
            }

            String group = clearing ? "" : args[2];
            int radiusArg = clearing ? 2 : 3;
            int radius = args.length > radiusArg ? parseInt(sender, args[radiusArg]) : DEFAULT_GROUP_RADIUS;

            Colony colony = findColony(sender, manager, 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            Map<UUID, EntityCitizen> loaded = new HashMap<>();
            for (EntityCitizen citizen : findCitizens(world, colony.getId())) {
                loaded.put(citizen.getUniqueID(), citizen);
            }

            int dimension = world.provider.dimensionId;
            int changed = 0;
            for (ColonyCitizen entry : colony.getCitizens()) {
                EntityCitizen citizen = loaded.get(entry.getId());
                double distanceSq = citizen != null ? citizen.getDistanceSqToEntity(player)
                    : entry.distanceSqTo(dimension, player.posX, player.posZ);
                if (distanceSq > (double) radius * radius) {
                    continue;
                }
                if (citizen != null) {
                    citizen.setGroup(group);
                } else {
                    entry.setGroup(group);
                }
                changed++;
            }
            manager.markDirty();
            sender.addChatMessage(
                new ChatComponentText(changed + " citizen(s) " + (clearing ? "ungrouped" : "put into group " + group)));
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
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            CitizenCommandMoveTo command = new CitizenCommandMoveTo(
                parseInt(sender, args[3]),
                parseInt(sender, args[4]),
                parseInt(sender, args[5]));
            command.setTargetGroup(group);
            manager.enqueueOrder(colony.getId(), command);
            sender.addChatMessage(
                new ChatComponentText(
                    "Queued move_to for group " + group
                        + " of colony #"
                        + colony.getId()
                        + " ("
                        + colony.getOrderCount(group)
                        + " order(s) pending)"));
            return;
        }

        if ("guard".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 4 ? parseInt(sender, args[3]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            CitizenCommandGuard command = new CitizenCommandGuard();
            command.setTargetGroup(group);
            manager.enqueueOrder(colony.getId(), command);
            sender.addChatMessage(
                new ChatComponentText(
                    "Queued guard for group " + group
                        + " of colony #"
                        + colony.getId()
                        + " ("
                        + colony.getOrderCount(group)
                        + " order(s) pending)"));
            return;
        }

        if ("cancel".equals(args[1])) {
            Colony colony = findColony(sender, manager, args.length >= 4 ? parseInt(sender, args[3]) : 0);
            if (colony == null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No colony found"));
                return;
            }
            if (!canManage(sender, colony)) {
                return;
            }

            int cleared = manager.clearOrders(colony.getId(), group);
            int stopped = 0;
            for (EntityCitizen citizen : findCitizens(world, colony.getId())) {
                if (!group.equals(citizen.getGroup())) {
                    continue;
                }
                citizen.getCommands()
                    .clear(citizen);
                stopped++;
            }
            sender.addChatMessage(
                new ChatComponentText(
                    "Dropped " + cleared + " order(s) for group " + group + ", stopped " + stopped + " citizen(s)"));
            return;
        }

        throw new WrongUsageException(getGroupUsage());
    }

    private String getGroupUsage() {
        return "/colony group <list [colonyId]|set <group> [radius]|clear [radius]|order <group> <x> <y> <z> [colonyId]"
            + "|guard <group> [colonyId]|cancel <group> [colonyId]>";
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
