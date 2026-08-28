package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketCitizenGroup implements IMessage {

    public static final int MAX_GROUP_LENGTH = 24;

    private static final int MAX_CITIZENS = 512;

    private int colonyId;
    private String group = "";
    private final List<UUID> citizens = new ArrayList<>();

    public PacketCitizenGroup() {}

    public PacketCitizenGroup(int colonyId, String group, Collection<UUID> citizens) {
        this.colonyId = colonyId;
        this.group = group == null ? "" : group;
        this.citizens.addAll(citizens);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        group = ByteBufUtils.readUTF8String(buf);
        if (group.length() > MAX_GROUP_LENGTH) {
            group = group.substring(0, MAX_GROUP_LENGTH);
        }
        int count = Math.min(buf.readInt(), MAX_CITIZENS);
        for (int i = 0; i < count; i++) {
            citizens.add(new UUID(buf.readLong(), buf.readLong()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        ByteBufUtils.writeUTF8String(buf, group);
        buf.writeInt(Math.min(citizens.size(), MAX_CITIZENS));
        for (int i = 0; i < Math.min(citizens.size(), MAX_CITIZENS); i++) {
            UUID id = citizens.get(i);
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    public static class Handler extends ColonyPacketHandler<PacketCitizenGroup> {

        @Override
        protected int colonyId(PacketCitizenGroup message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketCitizenGroup message) {
            ColonyManager manager = ColonyManager.get(player.worldObj);
            Map<UUID, EntityCitizen> loaded = GCNetwork.loadedCitizens(player.worldObj, colony.getId());
            for (UUID id : message.citizens) {
                EntityCitizen citizen = loaded.get(id);
                if (citizen != null) {
                    citizen.setGroup(message.group);
                } else {
                    manager.setCitizenGroup(colony.getId(), id, message.group);
                }
            }
            GCNetwork.sendColony(player, colony);
        }
    }
}
