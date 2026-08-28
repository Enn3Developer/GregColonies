package com.enn3developer.gregcolonies.network;

import java.util.Collection;
import java.util.UUID;

import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.entity.CitizenJob;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import io.netty.buffer.ByteBuf;

public class PacketCitizenJob extends CitizenTargetPacket {

    private CitizenJob job = CitizenJob.NONE;

    public PacketCitizenJob() {}

    public PacketCitizenJob(int colonyId, CitizenJob job, Collection<UUID> citizens) {
        super(colonyId, citizens);
        this.job = job == null ? CitizenJob.NONE : job;
    }

    @Override
    protected void readPayload(ByteBuf buf) {
        job = CitizenJob.byId(buf.readByte());
    }

    @Override
    protected void writePayload(ByteBuf buf) {
        buf.writeByte(CitizenJob.idOf(job));
    }

    @Override
    protected void applyLive(EntityCitizen citizen) {
        citizen.setJob(job);
    }

    @Override
    protected void applyStored(ColonyRegistry manager, int colonyId, UUID id) {
        manager.setCitizenJob(colonyId, id, job);
    }

    public static class Handler extends CitizenTargetPacket.Handler<PacketCitizenJob> {
    }
}
