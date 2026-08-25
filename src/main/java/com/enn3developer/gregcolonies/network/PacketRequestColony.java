package com.enn3developer.gregcolonies.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestColony implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestColony, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestColony message, MessageContext ctx) {
            GCNetwork.queueRequest(ctx.getServerHandler().playerEntity);
            return null;
        }
    }
}
