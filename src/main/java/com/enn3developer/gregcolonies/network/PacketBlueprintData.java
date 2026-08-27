package com.enn3developer.gregcolonies.network;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.colony.Blueprint;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketBlueprintData implements IMessage {

    private int colonyId;
    private int index;
    private Blueprint blueprint;
    private Map<Integer, Integer> stock = new LinkedHashMap<>();

    public PacketBlueprintData() {}

    public PacketBlueprintData(int colonyId, int index, Blueprint blueprint, Map<Integer, Integer> stock) {
        this.colonyId = colonyId;
        this.index = index;
        this.blueprint = blueprint;
        this.stock = stock;
    }

    public int getColonyId() {
        return colonyId;
    }

    public int getIndex() {
        return index;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public Map<Integer, Integer> getStock() {
        return stock;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        colonyId = buf.readInt();
        index = buf.readInt();
        blueprint = Blueprint.read(buf);
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            stock.put(buf.readInt(), buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(colonyId);
        buf.writeInt(index);
        blueprint.write(buf);
        buf.writeInt(stock.size());
        for (Map.Entry<Integer, Integer> entry : stock.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static class Handler implements IMessageHandler<PacketBlueprintData, IMessage> {

        @Override
        public IMessage onMessage(PacketBlueprintData message, MessageContext ctx) {
            if (message.blueprint != null) {
                GregColonies.proxy.showBlueprint(message);
            }
            return null;
        }
    }
}
