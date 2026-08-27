package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
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

    public static class Handler implements IMessageHandler<PacketColonyBuild, IMessage> {

        @Override
        public IMessage onMessage(PacketColonyBuild message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GCNetwork.enqueue(() -> apply(player, message));
            return null;
        }

        private static void apply(EntityPlayerMP player, PacketColonyBuild message) {
            Colony colony = GCNetwork.accessibleColony(player, message.colonyId);
            if (colony == null) {
                return;
            }
            ColonyManager manager = ColonyManager.get(player.worldObj);
            if (message.clear) {
                manager.setBuildSite(colony.getId(), null);
                player.addChatMessage(new ChatComponentText("Build site cleared"));
                GCNetwork.sendColony(player, colony);
                return;
            }

            World world = player.worldObj;
            if (colony.getDimension() != world.provider.dimensionId) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "The build site must be in the colony dimension"));
                return;
            }
            Blueprint blueprint = colony.getActiveBlueprint();
            if (blueprint == null) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "Capture a blueprint before starting a build"));
                return;
            }
            if (!colony.hasMaterials()) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "Set a materials chest before starting a build"));
                return;
            }

            int y = message.y + 1;
            BuildSite site = new BuildSite(
                message.x,
                y,
                message.z,
                blueprint,
                colony.getPlaceRotation(),
                colony.isPlaceMirror());
            manager.setBuildSite(colony.getId(), site);
            player.addChatMessage(
                new ChatComponentText(
                    "Build site set at " + message.x
                        + "/"
                        + y
                        + "/"
                        + message.z
                        + ", "
                        + site.remaining(world)
                        + " blocks to place"));
            GCNetwork.sendColony(player, colony);
        }
    }
}
