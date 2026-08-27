package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketColonyBlueprint implements IMessage {

    private int colonyId;
    private int x1;
    private int y1;
    private int z1;
    private int x2;
    private int y2;
    private int z2;

    public PacketColonyBlueprint() {}

    public PacketColonyBlueprint(int colonyId, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.colonyId = colonyId;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        x1 = buf.readInt();
        y1 = buf.readInt();
        z1 = buf.readInt();
        x2 = buf.readInt();
        y2 = buf.readInt();
        z2 = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(x1);
        buf.writeInt(y1);
        buf.writeInt(z1);
        buf.writeInt(x2);
        buf.writeInt(y2);
        buf.writeInt(z2);
    }

    public static class Handler implements IMessageHandler<PacketColonyBlueprint, IMessage> {

        @Override
        public IMessage onMessage(PacketColonyBlueprint message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GCNetwork.enqueue(() -> apply(player, message));
            return null;
        }

        private static void apply(EntityPlayerMP player, PacketColonyBlueprint message) {
            Colony colony = GCNetwork.accessibleColony(player, message.colonyId);
            if (colony == null) {
                return;
            }
            World world = player.worldObj;
            if (colony.getDimension() != world.provider.dimensionId) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "The blueprint must be in the colony dimension"));
                return;
            }

            Blueprint blueprint = Blueprint
                .capture(world, message.x1, message.y1, message.z1, message.x2, message.y2, message.z2);
            if (blueprint == null) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "That region holds no buildable blocks, or is over "
                            + Blueprint.MAX_VOLUME
                            + " blocks"));
                return;
            }

            ColonyManager.get(world)
                .setBlueprint(colony.getId(), blueprint);
            player.addChatMessage(
                new ChatComponentText(
                    "Blueprint captured: " + blueprint.getSizeX()
                        + "x"
                        + blueprint.getSizeY()
                        + "x"
                        + blueprint.getSizeZ()
                        + ", "
                        + blueprint.blockCount()
                        + " blocks"));
            GCNetwork.sendColony(player, colony);
        }
    }
}
