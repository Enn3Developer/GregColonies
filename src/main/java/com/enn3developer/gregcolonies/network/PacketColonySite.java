package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyActions;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.colony.ColonySiteKind;
import com.enn3developer.gregcolonies.colony.Outcome;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketColonySite implements IMessage {

    private int colonyId;
    private byte kind;
    private int x;
    private int y;
    private int z;
    private boolean clear;

    public PacketColonySite() {}

    public PacketColonySite(int colonyId, ColonySiteKind kind, int x, int y, int z, boolean clear) {
        this.colonyId = colonyId;
        this.kind = (byte) kind.ordinal();
        this.x = x;
        this.y = y;
        this.z = z;
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        kind = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        clear = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeByte(kind);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(clear);
    }

    public static class Handler extends ColonyPacketHandler<PacketColonySite> {

        @Override
        protected int colonyId(PacketColonySite message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketColonySite message) {
            ColonySiteKind kind = ColonySiteKind.byId(message.kind);
            if (kind == null) {
                return;
            }
            World world = player.worldObj;
            ColonyRegistry registry = ColonyManager.registry(world);
            Outcome outcome = message.clear ? ColonyActions.clearSite(registry, colony, kind)
                : ColonyActions.setSite(
                    registry,
                    colony,
                    kind,
                    world.provider.dimensionId,
                    Inventories.at(world, message.x, message.y, message.z) != null,
                    message.x,
                    message.y,
                    message.z);
            Chat.tell(player, outcome);
            if (outcome.isOk()) {
                GCNetwork.sendColony(player, colony);
            }
        }
    }
}
