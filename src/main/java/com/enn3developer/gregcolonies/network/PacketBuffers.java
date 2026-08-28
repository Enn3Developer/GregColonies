package com.enn3developer.gregcolonies.network;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;

public final class PacketBuffers {

    public static final int MAX_CITIZENS = 512;

    private PacketBuffers() {}

    public static void writeId(ByteBuf buf, UUID id) {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
    }

    public static UUID readId(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeIds(ByteBuf buf, List<UUID> ids) {
        int count = Math.min(ids.size(), MAX_CITIZENS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            writeId(buf, ids.get(i));
        }
    }

    public static void readIds(ByteBuf buf, Collection<UUID> into) {
        int count = Math.min(buf.readInt(), MAX_CITIZENS);
        for (int i = 0; i < count; i++) {
            into.add(readId(buf));
        }
    }
}
