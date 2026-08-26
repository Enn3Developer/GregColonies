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
    private boolean hasDropOff;
    private int dropOffX;
    private int dropOffY;
    private int dropOffZ;
    private boolean hasPickUp;
    private int pickUpX;
    private int pickUpY;
    private int pickUpZ;
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
        snapshot.hasDropOff = colony.hasDropOff();
        snapshot.dropOffX = colony.getDropOffX();
        snapshot.dropOffY = colony.getDropOffY();
        snapshot.dropOffZ = colony.getDropOffZ();
        snapshot.hasPickUp = colony.hasPickUp();
        snapshot.pickUpX = colony.getPickUpX();
        snapshot.pickUpY = colony.getPickUpY();
        snapshot.pickUpZ = colony.getPickUpZ();

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

    public boolean hasDropOff() {
        return hasDropOff;
    }

    public int getDropOffX() {
        return dropOffX;
    }

    public int getDropOffY() {
        return dropOffY;
    }

    public int getDropOffZ() {
        return dropOffZ;
    }

    public boolean isDropOffAt(int x, int y, int z) {
        return hasDropOff && dropOffX == x && dropOffY == y && dropOffZ == z;
    }

    public boolean hasPickUp() {
        return hasPickUp;
    }

    public int getPickUpX() {
        return pickUpX;
    }

    public int getPickUpY() {
        return pickUpY;
    }

    public int getPickUpZ() {
        return pickUpZ;
    }

    public boolean isPickUpAt(int x, int y, int z) {
        return hasPickUp && pickUpX == x && pickUpY == y && pickUpZ == z;
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
        buf.writeBoolean(hasDropOff);
        buf.writeInt(dropOffX);
        buf.writeInt(dropOffY);
        buf.writeInt(dropOffZ);
        buf.writeBoolean(hasPickUp);
        buf.writeInt(pickUpX);
        buf.writeInt(pickUpY);
        buf.writeInt(pickUpZ);
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
        snapshot.hasDropOff = buf.readBoolean();
        snapshot.dropOffX = buf.readInt();
        snapshot.dropOffY = buf.readInt();
        snapshot.dropOffZ = buf.readInt();
        snapshot.hasPickUp = buf.readBoolean();
        snapshot.pickUpX = buf.readInt();
        snapshot.pickUpY = buf.readInt();
        snapshot.pickUpZ = buf.readInt();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            snapshot.citizens.add(CitizenSnapshot.read(buf));
        }
        return snapshot;
    }
}
