package com.enn3developer.gregcolonies.colony;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.enn3developer.gregcolonies.Config;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommandRegistry;

public class Colony {

    private int id;
    private String name;
    private UUID owner;
    private String ownerName;
    private int dimension;
    private int x;
    private int y;
    private int z;
    private boolean hasDropOff;
    private int dropOffX;
    private int dropOffY;
    private int dropOffZ;
    private boolean hasPickUp;
    private int pickUpX;
    private int pickUpY;
    private int pickUpZ;
    private final Deque<CitizenCommand> orders = new ArrayDeque<>();
    private final Map<UUID, ColonyCitizen> citizens = new LinkedHashMap<>();

    private Colony() {}

    public Colony(int id, String name, UUID owner, String ownerName, int dimension, int x, int y, int z) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerName = ownerName;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
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

    public void setDropOff(int x, int y, int z) {
        hasDropOff = true;
        dropOffX = x;
        dropOffY = y;
        dropOffZ = z;
    }

    public void clearDropOff() {
        hasDropOff = false;
        dropOffX = 0;
        dropOffY = 0;
        dropOffZ = 0;
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

    public void setPickUp(int x, int y, int z) {
        hasPickUp = true;
        pickUpX = x;
        pickUpY = y;
        pickUpZ = z;
    }

    public void clearPickUp() {
        hasPickUp = false;
        pickUpX = 0;
        pickUpY = 0;
        pickUpZ = 0;
    }

    public boolean isBedFree(UUID id, int x, int y, int z) {
        for (ColonyCitizen citizen : citizens.values()) {
            if (!citizen.getId()
                .equals(id) && citizen.isBedAt(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    public boolean claimBed(UUID id, int x, int y, int z) {
        ColonyCitizen owner = citizens.get(id);
        if (owner == null || !isBedFree(id, x, y, z)) {
            return false;
        }
        owner.setBed(x, y, z);
        return true;
    }

    public void releaseBed(UUID id) {
        ColonyCitizen owner = citizens.get(id);
        if (owner != null) {
            owner.clearBed();
        }
    }

    public ColonyCitizen registerCitizen(EntityCitizen citizen) {
        ColonyCitizen entry = citizens.get(citizen.getUniqueID());
        if (entry == null) {
            entry = new ColonyCitizen(citizen);
            citizens.put(entry.getId(), entry);
        } else {
            entry.setName(citizen.getCitizenName());
            entry.updatePosition(citizen);
        }
        return entry;
    }

    public boolean hasCitizenNamed(String name) {
        for (ColonyCitizen citizen : citizens.values()) {
            if (citizen.getName()
                .equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public ColonyCitizen getCitizen(UUID id) {
        return citizens.get(id);
    }

    public boolean removeCitizen(UUID id) {
        return citizens.remove(id) != null;
    }

    public Collection<ColonyCitizen> getCitizens() {
        return citizens.values();
    }

    public int getCitizenCount() {
        return citizens.size();
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean canAccess(EntityPlayer player) {
        return isOwner(player.getUniqueID());
    }

    public void enqueueOrder(CitizenCommand command) {
        orders.addLast(command);
    }

    public CitizenCommand pollOrder() {
        return orders.pollFirst();
    }

    public CitizenCommand pollOrderFor(EntityCitizen citizen) {
        for (CitizenCommand order : orders) {
            if (order.canBeClaimedBy(citizen)) {
                orders.remove(order);
                return order;
            }
        }
        return null;
    }

    public int getOrderCount() {
        return orders.size();
    }

    public int clearOrders() {
        int cleared = orders.size();
        orders.clear();
        return cleared;
    }

    public int clearOrders(String group) {
        String target = group == null ? "" : group;
        int cleared = 0;
        Iterator<CitizenCommand> iterator = orders.iterator();
        while (iterator.hasNext()) {
            if (target.equals(
                iterator.next()
                    .getTargetGroup())) {
                iterator.remove();
                cleared++;
            }
        }
        return cleared;
    }

    public int getOrderCount(String group) {
        String target = group == null ? "" : group;
        int count = 0;
        for (CitizenCommand order : orders) {
            if (target.equals(order.getTargetGroup())) {
                count++;
            }
        }
        return count;
    }

    public boolean isCenteredAt(int dimension, int x, int y, int z) {
        return this.dimension == dimension && this.x == x && this.y == y && this.z == z;
    }

    public boolean isInside(int dimension, double x, double z) {
        if (this.dimension != dimension) {
            return false;
        }
        double dx = this.x + 0.5D - x;
        double dz = this.z + 0.5D - z;
        return dx * dx + dz * dz <= (double) Config.colonyRadius * Config.colonyRadius;
    }

    public double distanceSqTo(int dimension, int x, int z) {
        if (this.dimension != dimension) {
            return Double.MAX_VALUE;
        }
        double dx = this.x - x;
        double dz = this.z - z;
        return dx * dx + dz * dz;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("id", id);
        tag.setString("name", name);
        tag.setString("owner", owner.toString());
        tag.setString("ownerName", ownerName);
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setBoolean("hasDropOff", hasDropOff);
        if (hasDropOff) {
            tag.setInteger("dropOffX", dropOffX);
            tag.setInteger("dropOffY", dropOffY);
            tag.setInteger("dropOffZ", dropOffZ);
        }
        tag.setBoolean("hasPickUp", hasPickUp);
        if (hasPickUp) {
            tag.setInteger("pickUpX", pickUpX);
            tag.setInteger("pickUpY", pickUpY);
            tag.setInteger("pickUpZ", pickUpZ);
        }

        NBTTagList orderList = new NBTTagList();
        for (CitizenCommand order : orders) {
            orderList.appendTag(CitizenCommandRegistry.write(order));
        }
        tag.setTag("orders", orderList);

        NBTTagList citizenList = new NBTTagList();
        for (ColonyCitizen citizen : citizens.values()) {
            citizenList.appendTag(citizen.writeToNBT());
        }
        tag.setTag("citizens", citizenList);
        return tag;
    }

    public static Colony readFromNBT(NBTTagCompound tag) {
        Colony colony = new Colony();
        colony.id = tag.getInteger("id");
        colony.name = tag.getString("name");
        colony.owner = UUID.fromString(tag.getString("owner"));
        colony.ownerName = tag.getString("ownerName");
        colony.dimension = tag.getInteger("dim");
        colony.x = tag.getInteger("x");
        colony.y = tag.getInteger("y");
        colony.z = tag.getInteger("z");
        colony.hasDropOff = tag.getBoolean("hasDropOff");
        if (colony.hasDropOff) {
            colony.dropOffX = tag.getInteger("dropOffX");
            colony.dropOffY = tag.getInteger("dropOffY");
            colony.dropOffZ = tag.getInteger("dropOffZ");
        }
        colony.hasPickUp = tag.getBoolean("hasPickUp");
        if (colony.hasPickUp) {
            colony.pickUpX = tag.getInteger("pickUpX");
            colony.pickUpY = tag.getInteger("pickUpY");
            colony.pickUpZ = tag.getInteger("pickUpZ");
        }

        NBTTagList orderList = tag.getTagList("orders", 10);
        for (int i = 0; i < orderList.tagCount(); i++) {
            CitizenCommand order = CitizenCommandRegistry.read(orderList.getCompoundTagAt(i));
            if (order != null) {
                colony.orders.addLast(order);
            }
        }

        NBTTagList citizenList = tag.getTagList("citizens", 10);
        for (int i = 0; i < citizenList.tagCount(); i++) {
            ColonyCitizen citizen = ColonyCitizen.readFromNBT(citizenList.getCompoundTagAt(i));
            colony.citizens.put(citizen.getId(), citizen);
        }
        return colony;
    }

    @Override
    public String toString() {
        return "Colony#" + id
            + "["
            + name
            + ", owner="
            + ownerName
            + ", dim="
            + dimension
            + ", "
            + x
            + "/"
            + y
            + "/"
            + z
            + "]";
    }
}
