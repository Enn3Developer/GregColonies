package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public abstract class ServerPacketHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {

    @Override
    public IMessage onMessage(T message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        GCNetwork.enqueue(() -> apply(player, message));
        return null;
    }

    protected abstract void apply(EntityPlayerMP player, T message);
}
