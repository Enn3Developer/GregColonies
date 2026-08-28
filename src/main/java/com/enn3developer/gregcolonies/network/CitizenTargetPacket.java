package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public abstract class CitizenTargetPacket implements IMessage {

    private int colonyId;

    private final List<UUID> citizens = new ArrayList<>();

    protected CitizenTargetPacket() {}

    protected CitizenTargetPacket(int colonyId, Collection<UUID> citizens) {
        this.colonyId = colonyId;
        this.citizens.addAll(citizens);
    }

    public int getColonyId() {
        return colonyId;
    }

    public List<UUID> getCitizens() {
        return citizens;
    }

    protected abstract void readPayload(ByteBuf buf);

    protected abstract void writePayload(ByteBuf buf);

    protected abstract void applyLive(EntityCitizen citizen);

    protected abstract void applyStored(ColonyRegistry manager, int colonyId, UUID id);

    @Override
    public final void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        readPayload(buf);
        PacketBuffers.readIds(buf, citizens);
    }

    @Override
    public final void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        writePayload(buf);
        PacketBuffers.writeIds(buf, citizens);
    }

    public abstract static class Handler<T extends CitizenTargetPacket> extends ColonyPacketHandler<T> {

        @Override
        protected int colonyId(T message) {
            return message.getColonyId();
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, T message) {
            ColonyRegistry manager = ColonyManager.registry(player.worldObj);
            Map<UUID, EntityCitizen> loaded = GCNetwork.loadedCitizens(player.worldObj, colony.getId());
            for (UUID id : message.getCitizens()) {
                EntityCitizen citizen = loaded.get(id);
                if (citizen != null) {
                    message.applyLive(citizen);
                } else {
                    message.applyStored(manager, colony.getId(), id);
                }
            }
            GCNetwork.sendColony(player, colony);
        }
    }
}
