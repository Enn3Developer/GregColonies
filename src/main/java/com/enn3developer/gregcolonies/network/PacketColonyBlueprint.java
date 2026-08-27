package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketColonyBlueprint implements IMessage {

    private int colonyId;
    private int x1;
    private int z1;
    private int x2;
    private int z2;
    private int baseY;
    private int height;
    private String name = "";

    public PacketColonyBlueprint() {}

    public PacketColonyBlueprint(int colonyId, int x1, int z1, int x2, int z2, int baseY, int height, String name) {
        this.colonyId = colonyId;
        this.x1 = x1;
        this.z1 = z1;
        this.x2 = x2;
        this.z2 = z2;
        this.baseY = baseY;
        this.height = height;
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        x1 = buf.readInt();
        z1 = buf.readInt();
        x2 = buf.readInt();
        z2 = buf.readInt();
        baseY = buf.readInt();
        height = buf.readInt();
        name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(x1);
        buf.writeInt(z1);
        buf.writeInt(x2);
        buf.writeInt(z2);
        buf.writeInt(baseY);
        buf.writeInt(height);
        ByteBufUtils.writeUTF8String(buf, name);
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
            if (colony.getBlueprints()
                .size() >= Colony.MAX_BLUEPRINTS) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "The blueprint library is full ("
                            + Colony.MAX_BLUEPRINTS
                            + "), delete one first"));
                return;
            }

            int height = Math.max(1, Math.min(Blueprint.MAX_SIDE, message.height));
            int baseY = Math.max(0, Math.min(world.getHeight() - 1, message.baseY));
            Blueprint blueprint = Blueprint.capture(
                world,
                message.name,
                message.x1,
                baseY,
                message.z1,
                message.x2,
                baseY + height - 1,
                message.z2);
            if (blueprint == null) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "That region holds no buildable blocks"));
                return;
            }

            int index = ColonyManager.get(world)
                .addBlueprint(colony.getId(), blueprint);
            if (index < 0) {
                return;
            }
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
            GCNetwork.sendBlueprint(player, colony, index);
        }
    }
}
