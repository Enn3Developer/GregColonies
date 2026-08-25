package com.enn3developer.gregcolonies.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

public class ColonyCommand extends CommandBase {

    private static final List<String> SUB_COMMANDS = Arrays.asList("list", "info");

    @Override
    public String getCommandName() {
        return "colony";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/colony <list|info <id>>";
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

        ColonyManager manager = ColonyManager.get(sender.getEntityWorld());

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

        throw new WrongUsageException(getCommandUsage(sender));
    }
}
