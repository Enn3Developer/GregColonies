package com.enn3developer.gregcolonies.network;

import java.util.Collection;
import java.util.UUID;

import com.enn3developer.gregcolonies.colony.ColonyRegistry;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class PacketCitizenGroup extends CitizenTargetPacket {

    public static final int MAX_GROUP_LENGTH = 24;

    private String group = "";

    public PacketCitizenGroup() {}

    public PacketCitizenGroup(int colonyId, String group, Collection<UUID> citizens) {
        super(colonyId, citizens);
        this.group = group == null ? "" : group;
    }

    @Override
    protected void readPayload(ByteBuf buf) {
        group = ByteBufUtils.readUTF8String(buf);
        if (group.length() > MAX_GROUP_LENGTH) {
            group = group.substring(0, MAX_GROUP_LENGTH);
        }
    }

    @Override
    protected void writePayload(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, group);
    }

    @Override
    protected void applyLive(EntityCitizen citizen) {
        citizen.setGroup(group);
    }

    @Override
    protected void applyStored(ColonyRegistry manager, int colonyId, UUID id) {
        manager.setCitizenGroup(colonyId, id, group);
    }

    public static class Handler extends CitizenTargetPacket.Handler<PacketCitizenGroup> {
    }
}
