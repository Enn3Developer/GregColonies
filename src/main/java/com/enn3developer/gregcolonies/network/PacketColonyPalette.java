package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.List;

import com.enn3developer.gregcolonies.GregColonies;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketColonyPalette implements IMessage {

    public static final int MAX_ENTRIES = 256;

    private int colonyId;
    private final List<Entry> entries = new ArrayList<>();

    public PacketColonyPalette() {}

    public PacketColonyPalette(int colonyId, List<Entry> entries) {
        this.colonyId = colonyId;
        this.entries.addAll(entries);
    }

    public int getColonyId() {
        return colonyId;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        int count = Math.min(buf.readShort(), MAX_ENTRIES);
        for (int i = 0; i < count; i++) {
            String block = ByteBufUtils.readUTF8String(buf);
            int meta = buf.readByte() & 0xFF;
            int held = buf.readInt();
            entries.add(new Entry(block, meta, held));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        int count = Math.min(entries.size(), MAX_ENTRIES);
        buf.writeShort(count);
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            ByteBufUtils.writeUTF8String(buf, entry.block);
            buf.writeByte(entry.meta);
            buf.writeInt(entry.held);
        }
    }

    public static class Entry {

        private final String block;
        private final int meta;
        private final int held;

        public Entry(String block, int meta, int held) {
            this.block = block;
            this.meta = meta;
            this.held = held;
        }

        public String getBlock() {
            return block;
        }

        public int getMeta() {
            return meta;
        }

        public int getHeld() {
            return held;
        }
    }

    public static class Handler implements IMessageHandler<PacketColonyPalette, IMessage> {

        @Override
        public IMessage onMessage(PacketColonyPalette message, MessageContext ctx) {
            GregColonies.proxy.showPalette(message);
            return null;
        }
    }
}
