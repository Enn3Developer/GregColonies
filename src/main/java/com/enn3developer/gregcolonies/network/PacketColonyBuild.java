package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyActions;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.colony.Outcome;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketColonyBuild implements IMessage {

    private int colonyId;
    private int x;
    private int y;
    private int z;
    private boolean clear;

    public PacketColonyBuild() {}

    public PacketColonyBuild(int colonyId, int x, int y, int z, boolean clear) {
        this.colonyId = colonyId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        clear = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeBoolean(clear);
    }

    public static class Handler extends ColonyPacketHandler<PacketColonyBuild> {

        @Override
        protected int colonyId(PacketColonyBuild message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketColonyBuild message) {
            World world = player.worldObj;
            ColonyRegistry registry = ColonyManager.registry(world);
            Outcome outcome = message.clear ? ColonyActions.clearBuildSite(registry, colony)
                : ColonyActions.setBuildSite(
                    registry,
                    colony,
                    world.provider.dimensionId,
                    message.x,
                    message.y,
                    message.z,
                    site -> site.remaining(world));
            Chat.tell(player, outcome);
            if (outcome.isOk()) {
                GCNetwork.sendColony(player, colony);
            }
        }
    }
}
