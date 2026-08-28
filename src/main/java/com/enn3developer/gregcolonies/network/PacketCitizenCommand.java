package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandQueue;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandChop;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandFarm;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandGuard;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMine;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketCitizenCommand implements IMessage {

    public static final byte MOVE = 0;

    public static final byte GUARD = 1;

    public static final byte CANCEL = 2;

    public static final byte CHOP = 3;

    public static final byte MINE = 4;

    public static final byte FARM = 5;

    private int colonyId;
    private byte action;
    private boolean append;
    private int x;
    private int y;
    private int z;
    private int x2;
    private int y2;
    private int z2;
    private final List<UUID> citizens = new ArrayList<>();

    public PacketCitizenCommand() {}

    public PacketCitizenCommand(int colonyId, byte action, boolean append, int x, int y, int z,
        Collection<UUID> citizens) {
        this(colonyId, action, append, x, y, z, x, y, z, citizens);
    }

    public PacketCitizenCommand(int colonyId, byte action, boolean append, int x, int y, int z, int x2, int y2, int z2,
        Collection<UUID> citizens) {
        this.colonyId = colonyId;
        this.action = action;
        this.append = append;
        this.x = x;
        this.y = y;
        this.z = z;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.citizens.addAll(citizens);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        action = buf.readByte();
        append = buf.readBoolean();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        x2 = buf.readInt();
        y2 = buf.readInt();
        z2 = buf.readInt();
        PacketBuffers.readIds(buf, citizens);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeByte(action);
        buf.writeBoolean(append);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(x2);
        buf.writeInt(y2);
        buf.writeInt(z2);
        PacketBuffers.writeIds(buf, citizens);
    }

    public static class Handler extends ColonyPacketHandler<PacketCitizenCommand> {

        @Override
        protected int colonyId(PacketCitizenCommand message) {
            return message.colonyId;
        }

        @Override
        protected void apply(EntityPlayerMP player, Colony colony, PacketCitizenCommand message) {
            Map<UUID, EntityCitizen> loaded = GCNetwork.loadedCitizens(player.worldObj, colony.getId());
            for (UUID id : message.citizens) {
                EntityCitizen citizen = loaded.get(id);
                if (citizen == null) {
                    continue;
                }
                CitizenCommandQueue commands = citizen.getCommands();
                if (message.action == CANCEL) {
                    commands.clear(citizen);
                    continue;
                }
                if (!citizen.canWork()) {
                    continue;
                }
                CitizenCommand command = build(message);
                if (command == null) {
                    continue;
                }
                if (!message.append) {
                    commands.clear(citizen);
                }
                commands.enqueue(command);
            }
            GCNetwork.sendColony(player, colony);
        }

        private static CitizenCommand build(PacketCitizenCommand message) {
            switch (message.action) {
                case GUARD:
                    return new CitizenCommandGuard();
                case MOVE:
                    return new CitizenCommandMoveTo(message.x, message.y, message.z);
                case CHOP:
                    return new CitizenCommandChop(message.x, message.y, message.z, message.x2, message.y2, message.z2);
                case MINE:
                    return new CitizenCommandMine(message.x, message.z);
                case FARM:
                    return new CitizenCommandFarm(message.x, message.y, message.z, message.x2, message.y2, message.z2);
                default:
                    return null;
            }
        }
    }
}
