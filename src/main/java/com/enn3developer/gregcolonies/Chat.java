package com.enn3developer.gregcolonies;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.enn3developer.gregcolonies.colony.Outcome;

public final class Chat {

    private Chat() {}

    public static void info(ICommandSender target, String message) {
        target.addChatMessage(new ChatComponentText(message));
    }

    public static void error(ICommandSender target, String message) {
        say(target, EnumChatFormatting.RED, message);
    }

    public static void tell(ICommandSender target, Outcome outcome) {
        if (!outcome.hasMessage()) {
            return;
        }
        if (outcome.isOk()) {
            info(target, outcome.getMessage());
        } else {
            error(target, outcome.getMessage());
        }
    }

    public static void say(ICommandSender target, EnumChatFormatting color, String message) {
        target.addChatMessage(new ChatComponentText(color + message));
    }
}
