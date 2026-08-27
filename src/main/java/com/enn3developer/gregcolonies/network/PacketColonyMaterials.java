package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;
import com.enn3developer.gregcolonies.entity.ai.work.Inventories;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketColonyMaterials implements IMessage {

    private int colonyId;
    private int x;
    private int y;
    private int z;
    private boolean clear;

    public PacketColonyMaterials() {}

    public PacketColonyMaterials(int colonyId, int x, int y, int z, boolean clear) {
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

    public static class Handler implements IMessageHandler<PacketColonyMaterials, IMessage> {

        @Override
        public IMessage onMessage(PacketColonyMaterials message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GCNetwork.enqueue(() -> apply(player, message));
            return null;
        }

        private static void apply(EntityPlayerMP player, PacketColonyMaterials message) {
            Colony colony = GCNetwork.accessibleColony(player, message.colonyId);
            if (colony == null) {
                return;
            }
            ColonyManager manager = ColonyManager.get(player.worldObj);
            if (message.clear) {
                manager.clearMaterials(colony.getId());
                player.addChatMessage(new ChatComponentText("Colony materials pick-up cleared"));
                GCNetwork.sendColony(player, colony);
                return;
            }

            World world = player.worldObj;
            if (colony.getDimension() != world.provider.dimensionId) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "The materials pick-up must be in the colony dimension"));
                return;
            }
            if (Inventories.at(world, message.x, message.y, message.z) == null) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "That block has no inventory"));
                return;
            }

            manager.setMaterials(colony.getId(), message.x, message.y, message.z);
            player.addChatMessage(
                new ChatComponentText(
                    "Colony materials pick-up set to " + message.x + "/" + message.y + "/" + message.z));
            GCNetwork.sendColony(player, colony);
        }
    }
}
