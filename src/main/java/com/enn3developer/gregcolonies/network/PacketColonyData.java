package com.enn3developer.gregcolonies.network;

import com.enn3developer.gregcolonies.GregColonies;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketColonyData implements IMessage {

    private ColonySnapshot snapshot;

    public PacketColonyData() {}

    public PacketColonyData(ColonySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public ColonySnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        snapshot = ColonySnapshot.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        snapshot.write(buf);
    }

    public static class Handler implements IMessageHandler<PacketColonyData, IMessage> {

        @Override
        public IMessage onMessage(PacketColonyData message, MessageContext ctx) {
            GregColonies.proxy.openColonyScreen(message.getSnapshot());
            return null;
        }
    }
}
