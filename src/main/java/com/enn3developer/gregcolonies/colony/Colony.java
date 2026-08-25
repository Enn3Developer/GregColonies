package com.enn3developer.gregcolonies.colony;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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
    private final Deque<CitizenCommand> orders = new ArrayDeque<>();

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

    public int getOrderCount() {
        return orders.size();
    }

    public void clearOrders() {
        orders.clear();
    }

    public boolean isCenteredAt(int dimension, int x, int y, int z) {
        return this.dimension == dimension && this.x == x && this.y == y && this.z == z;
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

        NBTTagList orderList = new NBTTagList();
        for (CitizenCommand order : orders) {
            orderList.appendTag(CitizenCommandRegistry.write(order));
        }
        tag.setTag("orders", orderList);
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

        NBTTagList orderList = tag.getTagList("orders", 10);
        for (int i = 0; i < orderList.tagCount(); i++) {
            CitizenCommand order = CitizenCommandRegistry.read(orderList.getCompoundTagAt(i));
            if (order != null) {
                colony.orders.addLast(order);
            }
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
