package com.enn3developer.gregcolonies.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketOpenCitizen implements IMessage {

    public static final double OPEN_RANGE = 64.0D;

    private int colonyId;
    private UUID citizen;

    public PacketOpenCitizen() {}

    public PacketOpenCitizen(int colonyId, UUID citizen) {
        this.colonyId = colonyId;
        this.citizen = citizen;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        citizen = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeLong(citizen.getMostSignificantBits());
        buf.writeLong(citizen.getLeastSignificantBits());
    }

    public static class Handler extends ColonyPacketHandler<PacketOpenCitizen> {

        @Override
        protected int colonyId(PacketOpenCitizen message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketOpenCitizen message) {
            EntityCitizen citizen = GCNetwork.loadedCitizens(player.worldObj, colony.getId())
                .get(message.citizen);
            if (citizen == null) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "That citizen is not loaded right now"));
                return;
            }
            if (player.getDistanceSqToEntity(citizen) > OPEN_RANGE * OPEN_RANGE) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + citizen.getCitizenName() + " is too far away to reach"));
                return;
            }
            GuiFactories.entity()
                .open(player, citizen);
        }
    }
}
