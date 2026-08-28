package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketRequestColony implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler extends ServerPacketHandler<PacketRequestColony> {

        @Override
        protected void apply(EntityPlayerMP player, PacketRequestColony message) {
            GCNetwork.sendColony(player);
        }
    }
}
