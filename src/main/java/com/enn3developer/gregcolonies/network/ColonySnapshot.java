package com.enn3developer.gregcolonies.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.World;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.colony.Blueprint;
import com.enn3developer.gregcolonies.colony.BuildSite;
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
    private boolean hasMaterials;
    private int materialsX;
    private int materialsY;
    private int materialsZ;
    private final List<BlueprintEntry> blueprints = new ArrayList<>();
    private int activeBlueprint = -1;
    private int placeRotation;
    private boolean placeMirror;
    private boolean hasBuildSite;
    private String buildName = "";
    private int buildX;
    private int buildY;
    private int buildZ;
    private int buildRemaining;
    private int buildTotal;
    private final List<CitizenSnapshot> citizens = new ArrayList<>();

    public static class BlueprintEntry {

        private String name = "";
        private int sizeX;
        private int sizeY;
        private int sizeZ;
        private int blocks;

        public String getName() {
            return name;
        }

        public String getLabel(int index) {
            return name.isEmpty() ? "blueprint " + (index + 1) : name;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeZ() {
            return sizeZ;
        }

        public int getBlocks() {
            return blocks;
        }

        public String getSizeLabel() {
            return sizeX + "x" + sizeY + "x" + sizeZ;
        }
    }

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
        snapshot.hasMaterials = colony.hasMaterials();
        snapshot.materialsX = colony.getMaterialsX();
        snapshot.materialsY = colony.getMaterialsY();
        snapshot.materialsZ = colony.getMaterialsZ();
        for (Blueprint blueprint : colony.getBlueprints()) {
            BlueprintEntry entry = new BlueprintEntry();
            entry.name = blueprint.getName();
            entry.sizeX = blueprint.getSizeX();
            entry.sizeY = blueprint.getSizeY();
            entry.sizeZ = blueprint.getSizeZ();
            entry.blocks = blueprint.blockCount();
            snapshot.blueprints.add(entry);
        }
        snapshot.activeBlueprint = colony.getActiveBlueprintIndex();
        snapshot.placeRotation = colony.getPlaceRotation();
        snapshot.placeMirror = colony.isPlaceMirror();
        BuildSite site = colony.getBuildSite();
        if (site != null) {
            snapshot.hasBuildSite = true;
            snapshot.buildName = site.getBlueprint()
                .getName();
            snapshot.buildX = site.getX();
            snapshot.buildY = site.getY();
            snapshot.buildZ = site.getZ();
            snapshot.buildRemaining = site.remaining(world);
            snapshot.buildTotal = site.total();
        }

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

    public boolean hasMaterials() {
        return hasMaterials;
    }

    public int getMaterialsX() {
        return materialsX;
    }

    public int getMaterialsY() {
        return materialsY;
    }

    public int getMaterialsZ() {
        return materialsZ;
    }

    public boolean isMaterialsAt(int x, int y, int z) {
        return hasMaterials && materialsX == x && materialsY == y && materialsZ == z;
    }

    public List<BlueprintEntry> getBlueprints() {
        return blueprints;
    }

    public int getActiveBlueprint() {
        return activeBlueprint >= 0 && activeBlueprint < blueprints.size() ? activeBlueprint : -1;
    }

    public BlueprintEntry getBlueprint(int index) {
        return index >= 0 && index < blueprints.size() ? blueprints.get(index) : null;
    }

    public int getPlaceRotation() {
        return placeRotation;
    }

    public boolean isPlaceMirror() {
        return placeMirror;
    }

    public boolean hasBlueprint() {
        return getActiveBlueprint() >= 0;
    }

    public String getBuildName() {
        return buildName;
    }

    public boolean hasBuildSite() {
        return hasBuildSite;
    }

    public int getBuildX() {
        return buildX;
    }

    public int getBuildY() {
        return buildY;
    }

    public int getBuildZ() {
        return buildZ;
    }

    public int getBuildRemaining() {
        return buildRemaining;
    }

    public int getBuildTotal() {
        return buildTotal;
    }

    public boolean isBuildSiteAt(int x, int y, int z) {
        return hasBuildSite && buildX == x && buildY == y + 1 && buildZ == z;
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
        buf.writeBoolean(hasMaterials);
        buf.writeInt(materialsX);
        buf.writeInt(materialsY);
        buf.writeInt(materialsZ);
        buf.writeInt(blueprints.size());
        for (BlueprintEntry entry : blueprints) {
            ByteBufUtils.writeUTF8String(buf, entry.name);
            buf.writeShort(entry.sizeX);
            buf.writeShort(entry.sizeY);
            buf.writeShort(entry.sizeZ);
            buf.writeInt(entry.blocks);
        }
        buf.writeInt(activeBlueprint);
        buf.writeByte(placeRotation);
        buf.writeBoolean(placeMirror);
        buf.writeBoolean(hasBuildSite);
        ByteBufUtils.writeUTF8String(buf, buildName);
        buf.writeInt(buildX);
        buf.writeInt(buildY);
        buf.writeInt(buildZ);
        buf.writeInt(buildRemaining);
        buf.writeInt(buildTotal);
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
        snapshot.hasMaterials = buf.readBoolean();
        snapshot.materialsX = buf.readInt();
        snapshot.materialsY = buf.readInt();
        snapshot.materialsZ = buf.readInt();
        int blueprintCount = buf.readInt();
        for (int i = 0; i < blueprintCount; i++) {
            BlueprintEntry entry = new BlueprintEntry();
            entry.name = ByteBufUtils.readUTF8String(buf);
            entry.sizeX = buf.readShort();
            entry.sizeY = buf.readShort();
            entry.sizeZ = buf.readShort();
            entry.blocks = buf.readInt();
            snapshot.blueprints.add(entry);
        }
        snapshot.activeBlueprint = buf.readInt();
        snapshot.placeRotation = buf.readByte();
        snapshot.placeMirror = buf.readBoolean();
        snapshot.hasBuildSite = buf.readBoolean();
        snapshot.buildName = ByteBufUtils.readUTF8String(buf);
        snapshot.buildX = buf.readInt();
        snapshot.buildY = buf.readInt();
        snapshot.buildZ = buf.readInt();
        snapshot.buildRemaining = buf.readInt();
        snapshot.buildTotal = buf.readInt();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            snapshot.citizens.add(CitizenSnapshot.read(buf));
        }
        return snapshot;
    }
}
