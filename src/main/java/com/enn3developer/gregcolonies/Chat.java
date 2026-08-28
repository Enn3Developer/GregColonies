package com.enn3developer.gregcolonies;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public final class Chat {

    private Chat() {}

    public static void info(ICommandSender target, String message) {
        target.addChatMessage(new ChatComponentText(message));
    }

    public static void error(ICommandSender target, String message) {
        say(target, EnumChatFormatting.RED, message);
    }

    public static void say(ICommandSender target, EnumChatFormatting color, String message) {
        target.addChatMessage(new ChatComponentText(color + message));
    }
}
