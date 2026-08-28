package com.enn3developer.gregcolonies.network;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketBlueprintSave implements IMessage {

    public static final int NEW = -1;

    public static final int CHUNK_BYTES = 16384;

    public static final int MAX_BYTES = 2097152;

    private static final Map<UUID, Transfer> PENDING = new HashMap<>();

    private int colonyId;
    private int index;
    private int chunk;
    private int chunks;
    private byte[] payload = new byte[0];

    public PacketBlueprintSave() {}

    public PacketBlueprintSave(int colonyId, int index, int chunk, int chunks, byte[] payload) {
        this.colonyId = colonyId;
        this.index = index;
        this.chunk = chunk;
        this.chunks = chunks;
        this.payload = payload;
    }

    public static void forget(EntityPlayer player) {
        if (player != null) {
            PENDING.remove(player.getUniqueID());
        }
    }

    public static PacketBlueprintSave[] split(int colonyId, int index, Blueprint blueprint) {
        ByteBuf buffer = Unpooled.buffer();
        blueprint.write(buffer);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.release();

        int chunks = Math.max(1, (bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
        PacketBlueprintSave[] packets = new PacketBlueprintSave[chunks];
        for (int at = 0; at < chunks; at++) {
            int from = at * CHUNK_BYTES;
            int size = Math.min(CHUNK_BYTES, bytes.length - from);
            byte[] slice = new byte[size];
            System.arraycopy(bytes, from, slice, 0, size);
            packets[at] = new PacketBlueprintSave(colonyId, index, at, chunks, slice);
        }
        return packets;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        index = buf.readInt();
        chunk = buf.readInt();
        chunks = buf.readInt();
        int size = buf.readInt();
        if (size < 0 || size > CHUNK_BYTES || size > buf.readableBytes()) {
            payload = null;
            return;
        }
        payload = new byte[size];
        buf.readBytes(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(index);
        buf.writeInt(chunk);
        buf.writeInt(chunks);
        buf.writeInt(payload.length);
        buf.writeBytes(payload);
    }

    public static class Handler extends ServerPacketHandler<PacketBlueprintSave> {

        @Override
        protected void apply(EntityPlayerMP player, PacketBlueprintSave message) {
            UUID sender = player.getUniqueID();
            if (message.payload == null || message.chunks <= 0
                || message.chunk < 0
                || message.chunk >= message.chunks) {
                PENDING.remove(sender);
                return;
            }

            Transfer transfer = PENDING.get(sender);
            if (message.chunk == 0) {
                transfer = new Transfer(message.colonyId, message.index, message.chunks);
                PENDING.put(sender, transfer);
            }
            if (transfer == null || !transfer.accepts(message)) {
                PENDING.remove(sender);
                return;
            }
            transfer.add(message.payload);
            if (transfer.size() > MAX_BYTES) {
                PENDING.remove(sender);
                player
                    .addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "That design is too large to save"));
                return;
            }
            if (message.chunk < message.chunks - 1) {
                return;
            }
            PENDING.remove(sender);
            store(player, transfer);
        }

        private static void store(EntityPlayerMP player, Transfer transfer) {
            Colony colony = GCNetwork.accessibleColony(player, transfer.colonyId);
            if (colony == null) {
                return;
            }
            ByteBuf buffer = Unpooled.wrappedBuffer(transfer.bytes());
            Blueprint decoded;
            try {
                decoded = Blueprint.read(buffer);
            } catch (RuntimeException error) {
                decoded = null;
            }
            Blueprint blueprint = decoded == null ? null : decoded.trimmed();
            if (blueprint == null || !blueprint.isPlaceable()) {
                Chat.info(
                    player,
                    EnumChatFormatting.RED
                        + "That design holds no buildable blocks, or uses blocks this server does not have");
                return;
            }

            World world = player.worldObj;
            ColonyManager manager = ColonyManager.get(world);
            int index = transfer.index;
            if (index >= 0) {
                if (!manager.replaceBlueprint(colony.getId(), index, blueprint)) {
                    return;
                }
            } else {
                if (colony.getBlueprints()
                    .size() >= Colony.MAX_BLUEPRINTS) {
                    Chat.error(
                        player,
                        "The blueprint library is full (" + Colony.MAX_BLUEPRINTS + "), delete one first");
                    return;
                }
                index = manager.addBlueprint(colony.getId(), blueprint);
                if (index < 0) {
                    return;
                }
            }

            Chat.info(
                player,
                "Blueprint saved: " + blueprint.getSizeX()
                    + "x"
                    + blueprint.getSizeY()
                    + "x"
                    + blueprint.getSizeZ()
                    + ", "
                    + blueprint.blockCount()
                    + " blocks");
            GCNetwork.sendColony(player, colony);
            GCNetwork.sendBlueprint(player, colony, index);
        }
    }

    private static class Transfer {

        private final int colonyId;

        private final int index;

        private final int chunks;

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private int received;

        Transfer(int colonyId, int index, int chunks) {
            this.colonyId = colonyId;
            this.index = index;
            this.chunks = chunks;
        }

        boolean accepts(PacketBlueprintSave message) {
            return message.colonyId == colonyId && message.index == index
                && message.chunks == chunks
                && message.chunk == received;
        }

        void add(byte[] payload) {
            buffer.write(payload, 0, payload.length);
            received++;
        }

        int size() {
            return buffer.size();
        }

        byte[] bytes() {
            return buffer.toByteArray();
        }
    }
}
