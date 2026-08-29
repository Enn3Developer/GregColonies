package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Chat;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyActions;
import com.enn3developer.gregcolonies.colony.ColonyHome;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.colony.Homes;
import com.enn3developer.gregcolonies.colony.Outcome;
import com.enn3developer.gregcolonies.colony.WorkArea;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketColonyHome implements IMessage {

    private int colonyId;
    private int homeId;
    private boolean clear;
    private final WorkArea area = new WorkArea();

    public PacketColonyHome() {}

    private PacketColonyHome(int colonyId, int homeId, boolean clear) {
        this.colonyId = colonyId;
        this.homeId = homeId;
        this.clear = clear;
    }

    public static PacketColonyHome clear(int colonyId, int homeId) {
        return new PacketColonyHome(colonyId, homeId, true);
    }

    public static PacketColonyHome set(int colonyId, int x1, int y1, int z1, int x2, int y2, int z2) {
        PacketColonyHome message = new PacketColonyHome(colonyId, 0, false);
        message.area.set(x1, y1, z1, x2, y2, z2);
        return message;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        homeId = buf.readInt();
        clear = buf.readBoolean();
        area.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(homeId);
        buf.writeBoolean(clear);
        area.write(buf);
    }

    public static class Handler extends ColonyPacketHandler<PacketColonyHome> {

        @Override
        protected int colonyId(PacketColonyHome message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketColonyHome message) {
            World world = player.worldObj;
            ColonyRegistry registry = ColonyManager.registry(world);
            Outcome outcome;
            if (message.clear) {
                outcome = ColonyActions.clearHome(registry, colony, message.homeId);
            } else {
                WorkArea area = new WorkArea();
                area.copyFrom(message.area);
                area.capSide(WorkArea.MAX_SIDE);
                area.capHeight(ColonyHome.MAX_HEIGHT);
                outcome = ColonyActions
                    .setHome(registry, colony, world.provider.dimensionId, area, Homes.countBeds(world, area));
            }
            Chat.tell(player, outcome);
            if (outcome.isOk()) {
                GCNetwork.sendColony(player, colony);
            }
        }
    }
}
