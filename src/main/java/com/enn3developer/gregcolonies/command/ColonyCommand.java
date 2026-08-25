package com.enn3developer.gregcolonies.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

public class ColonyCommand extends CommandBase {

    private static final List<String> SUB_COMMANDS = Arrays.asList("list", "info", "spawn", "order", "cancel");

    private static final double CITIZEN_RANGE = 32.0D;

    @Override
    public String getCommandName() {
        return "colony";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/colony <list|info <id>|spawn [colonyId]|order <x> <y> <z>|cancel>";
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
            sender.addChatMessage(
                new ChatComponentText(
                    "center: dim " + colony
                        .getDimension() + " " + colony.getX() + "/" + colony.getY() + "/" + colony.getZ()));
            return;
        }

        if ("spawn".equals(args[0])) {
            Colony colony;
            int x;
            int y;
            int z;

            if (args.length >= 2) {
                colony = manager.getColony(parseInt(sender, args[1]));
                if (colony == null) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "No such colony"));
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
                colony = manager.getNearestColony(world.provider.dimensionId, x, z);
            }

            EntityCitizen citizen = new EntityCitizen(world);
            citizen.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
            if (colony != null) {
                citizen.setColonyId(colony.getId());
            }
            world.spawnEntityInWorld(citizen);

            sender.addChatMessage(
                new ChatComponentText(
                    "Spawned citizen at " + x
                        + "/"
                        + y
                        + "/"
                        + z
                        + (colony == null ? " (no colony)" : " for colony #" + colony.getId())));
            return;
        }

        if ("order".equals(args[0])) {
            if (args.length < 4) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            int x = parseInt(sender, args[1]);
            int y = parseInt(sender, args[2]);
            int z = parseInt(sender, args[3]);

            int ordered = 0;
            for (EntityCitizen citizen : findCitizens(sender, world)) {
                CitizenCommand command = new CitizenCommandMoveTo(x, y, z);
                citizen.getCommands()
                    .enqueue(command);
                ordered++;
            }
            sender.addChatMessage(new ChatComponentText("Queued move_to for " + ordered + " citizen(s)"));
            return;
        }

        if ("cancel".equals(args[0])) {
            int cleared = 0;
            for (EntityCitizen citizen : findCitizens(sender, world)) {
                citizen.getCommands()
                    .clear(citizen);
                cleared++;
            }
            sender.addChatMessage(new ChatComponentText("Cleared queues of " + cleared + " citizen(s)"));
            return;
        }

        throw new WrongUsageException(getCommandUsage(sender));
    }

    private List<EntityCitizen> findCitizens(ICommandSender sender, World world) {
        ChunkCoordinates at = sender.getPlayerCoordinates();
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(at.posX, at.posY, at.posZ, at.posX + 1.0D, at.posY + 1.0D, at.posZ + 1.0D)
            .expand(CITIZEN_RANGE, CITIZEN_RANGE / 2.0D, CITIZEN_RANGE);
        return world.getEntitiesWithinAABB(EntityCitizen.class, box);
    }
}
