package com.enn3developer.gregcolonies.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class PacketBuffersTest {

    @Test
    void idRoundTrips() {
        UUID id = UUID.fromString("d8a80763-8175-47f0-9ec5-a997c82108f4");
        ByteBuf buf = Unpooled.buffer();
        PacketBuffers.writeId(buf, id);
        assertEquals(id, PacketBuffers.readId(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void idListRoundTrips() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(UUID.randomUUID());
        }
        ByteBuf buf = Unpooled.buffer();
        PacketBuffers.writeIds(buf, ids);

        List<UUID> read = new ArrayList<>();
        PacketBuffers.readIds(buf, read);
        assertEquals(ids, read);
    }

    @Test
    void writingIsCappedAtTheLimit() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < PacketBuffers.MAX_CITIZENS + 50; i++) {
            ids.add(UUID.randomUUID());
        }
        ByteBuf buf = Unpooled.buffer();
        PacketBuffers.writeIds(buf, ids);

        List<UUID> read = new ArrayList<>();
        PacketBuffers.readIds(buf, read);
        assertEquals(PacketBuffers.MAX_CITIZENS, read.size());
        assertEquals(ids.subList(0, PacketBuffers.MAX_CITIZENS), read);
    }

    @Test
    void readingIsCappedEvenWhenTheHeaderLies() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(Integer.MAX_VALUE);
        for (int i = 0; i < PacketBuffers.MAX_CITIZENS; i++) {
            PacketBuffers.writeId(buf, UUID.randomUUID());
        }

        List<UUID> read = new ArrayList<>();
        PacketBuffers.readIds(buf, read);
        assertEquals(PacketBuffers.MAX_CITIZENS, read.size());
    }

    @Test
    void emptyListRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        PacketBuffers.writeIds(buf, new ArrayList<>());
        List<UUID> read = new ArrayList<>();
        PacketBuffers.readIds(buf, read);
        assertTrue(read.isEmpty());
    }
}
