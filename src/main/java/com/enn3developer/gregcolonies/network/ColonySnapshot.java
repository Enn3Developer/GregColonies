package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class ColonySnapshot {

    private int id;
    private String name = "";
    private String ownerName = "";
    private int dimension;
    private int x;
    private int y;
    private int z;
    private int radius;
    private int orderCount;
    private final List<CitizenSnapshot> citizens = new ArrayList<>();

    private ColonySnapshot() {}

    public static ColonySnapshot of(Colony colony, World world) {
        ColonySnapshot snapshot = new ColonySnapshot();
        snapshot.id = colony.getId();
        snapshot.name = colony.getName();
        snapshot.ownerName = colony.getOwnerName();
        snapshot.dimension = colony.getDimension();
        snapshot.x = colony.getX();
        snapshot.y = colony.getY();
        snapshot.z = colony.getZ();
        snapshot.radius = Config.colonyRadius;
        snapshot.orderCount = colony.getOrderCount();

        Map<UUID, EntityCitizen> loaded = new HashMap<>();
        for (Object object : world.loadedEntityList) {
            if (!(object instanceof EntityCitizen)) {
                continue;
            }
            EntityCitizen citizen = (EntityCitizen) object;
            if (citizen.getColonyId() == colony.getId()) {
                loaded.put(citizen.getUniqueID(), citizen);
            }
        }
        for (ColonyCitizen entry : colony.getCitizens()) {
            snapshot.citizens.add(CitizenSnapshot.of(entry, loaded.get(entry.getId())));
        }
        return snapshot;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getDimension() {
        return dimension;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getRadius() {
        return radius;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public List<CitizenSnapshot> getCitizens() {
        return citizens;
    }

    public void write(ByteBuf buf) {
        buf.writeInt(id);
        ByteBufUtils.writeUTF8String(buf, name);
        ByteBufUtils.writeUTF8String(buf, ownerName);
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(radius);
        buf.writeInt(orderCount);
        buf.writeInt(citizens.size());
        for (CitizenSnapshot citizen : citizens) {
            citizen.write(buf);
        }
    }

    public static ColonySnapshot read(ByteBuf buf) {
        ColonySnapshot snapshot = new ColonySnapshot();
        snapshot.id = buf.readInt();
        snapshot.name = ByteBufUtils.readUTF8String(buf);
        snapshot.ownerName = ByteBufUtils.readUTF8String(buf);
        snapshot.dimension = buf.readInt();
        snapshot.x = buf.readInt();
        snapshot.y = buf.readInt();
        snapshot.z = buf.readInt();
        snapshot.radius = buf.readInt();
        snapshot.orderCount = buf.readInt();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            snapshot.citizens.add(CitizenSnapshot.read(buf));
        }
        return snapshot;
    }
}
