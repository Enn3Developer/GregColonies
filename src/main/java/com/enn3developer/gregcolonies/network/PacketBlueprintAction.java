package com.enn3developer.gregcolonies.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyManager;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketBlueprintAction implements IMessage {

    public static final byte REQUEST = 0;

    public static final byte SELECT = 1;

    public static final byte RENAME = 2;

    public static final byte DELETE = 3;

    public static final byte PLACEMENT = 4;

    public static final byte PALETTE = 5;

    private int colonyId;
    private byte action;
    private int index;
    private int rotation;
    private boolean mirror;
    private String name = "";

    public PacketBlueprintAction() {}

    public PacketBlueprintAction(int colonyId, byte action, int index) {
        this(colonyId, action, index, 0, false, "");
    }

    public PacketBlueprintAction(int colonyId, byte action, int index, String name) {
        this(colonyId, action, index, 0, false, name);
    }

    public PacketBlueprintAction(int colonyId, byte action, int index, int rotation, boolean mirror, String name) {
        this.colonyId = colonyId;
        this.action = action;
        this.index = index;
        this.rotation = rotation;
        this.mirror = mirror;
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        action = buf.readByte();
        index = buf.readInt();
        rotation = buf.readInt();
        mirror = buf.readBoolean();
        name = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeByte(action);
        buf.writeInt(index);
        buf.writeInt(rotation);
        buf.writeBoolean(mirror);
        ByteBufUtils.writeUTF8String(buf, name);
    }

    public static class Handler extends ColonyPacketHandler<PacketBlueprintAction> {

        @Override
        protected int colonyId(PacketBlueprintAction message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketBlueprintAction message) {
            World world = player.worldObj;
            ColonyManager manager = ColonyManager.get(world);
            if (message.action == REQUEST) {
                GCNetwork.sendBlueprint(player, colony, message.index);
                return;
            }
            if (message.action == PALETTE) {
                GCNetwork.sendPalette(player, colony);
                return;
            }
            if (message.action == SELECT) {
                manager.setActiveBlueprint(colony.getId(), message.index);
            } else if (message.action == RENAME) {
                manager.renameBlueprint(colony.getId(), message.index, message.name);
            } else if (message.action == DELETE) {
                manager.removeBlueprint(colony.getId(), message.index);
            } else if (message.action == PLACEMENT) {
                manager.setPlacement(colony.getId(), message.rotation, message.mirror);
            } else {
                return;
            }
            GCNetwork.sendColony(player, colony);
            GCNetwork.sendBlueprint(
                player,
                colony,
                message.action == DELETE ? colony.getActiveBlueprintIndex() : message.index);
        }
    }
}
