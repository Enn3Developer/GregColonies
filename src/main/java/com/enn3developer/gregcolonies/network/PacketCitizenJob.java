package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketCitizenJob implements IMessage {

    private static final int MAX_CITIZENS = 512;

    private int colonyId;
    private CitizenJob job = CitizenJob.NONE;
    private final List<UUID> citizens = new ArrayList<>();

    public PacketCitizenJob() {}

    public PacketCitizenJob(int colonyId, CitizenJob job, Collection<UUID> citizens) {
        this.colonyId = colonyId;
        this.job = job == null ? CitizenJob.NONE : job;
        this.citizens.addAll(citizens);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        job = CitizenJob.byId(buf.readByte());
        int count = Math.min(buf.readInt(), MAX_CITIZENS);
        for (int i = 0; i < count; i++) {
            citizens.add(new UUID(buf.readLong(), buf.readLong()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeByte(CitizenJob.idOf(job));
        buf.writeInt(Math.min(citizens.size(), MAX_CITIZENS));
        for (int i = 0; i < Math.min(citizens.size(), MAX_CITIZENS); i++) {
            UUID id = citizens.get(i);
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    public static class Handler implements IMessageHandler<PacketCitizenJob, IMessage> {

        @Override
        public IMessage onMessage(PacketCitizenJob message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GCNetwork.enqueue(() -> apply(player, message));
            return null;
        }

        private static void apply(EntityPlayerMP player, PacketCitizenJob message) {
            Colony colony = GCNetwork.accessibleColony(player, message.colonyId);
            if (colony == null) {
                return;
            }
            ColonyManager manager = ColonyManager.get(player.worldObj);
            Map<UUID, EntityCitizen> loaded = GCNetwork.loadedCitizens(player.worldObj, colony.getId());
            for (UUID id : message.citizens) {
                EntityCitizen citizen = loaded.get(id);
                if (citizen != null) {
                    citizen.setJob(message.job);
                } else {
                    manager.setCitizenJob(colony.getId(), id, message.job);
                }
            }
            GCNetwork.sendColony(player, colony);
        }
    }
}
