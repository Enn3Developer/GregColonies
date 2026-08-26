package com.enn3developer.gregcolonies.network;

import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;

import com.enn3developer.gregcolonies.colony.ColonyCitizen;
import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public class CitizenSnapshot {

    private UUID id;

    private int entityId = -1;
    private String group = "";
    private double x;
    private double y;
    private double z;
    private float health;
    private float maxHealth;
    private int foodLevel;
    private int pendingCount;
    private String task = "";

    private CitizenSnapshot() {}

    public static CitizenSnapshot of(ColonyCitizen entry, EntityCitizen citizen) {
        CitizenSnapshot snapshot = new CitizenSnapshot();
        snapshot.id = entry.getId();
        snapshot.group = entry.getGroup();
        if (citizen == null) {
            snapshot.x = entry.getX() + 0.5D;
            snapshot.y = entry.getY();
            snapshot.z = entry.getZ() + 0.5D;
            return snapshot;
        }

        snapshot.entityId = citizen.getEntityId();
        snapshot.group = citizen.getGroup();
        snapshot.x = citizen.posX;
        snapshot.y = citizen.posY;
        snapshot.z = citizen.posZ;
        snapshot.health = citizen.getHealth();
        snapshot.maxHealth = (float) citizen.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .getAttributeValue();
        snapshot.foodLevel = citizen.getDiet()
            .getFoodLevel();
        snapshot.pendingCount = citizen.getCommands()
            .getPendingCount();
        CitizenCommand current = citizen.getCommands()
            .getCurrent();
        String livingTask = citizen.getLivingTask();
        if (!livingTask.isEmpty()) {
            snapshot.task = livingTask;
        } else if (current != null) {
            snapshot.task = current.describe();
        } else {
            String idle = citizen.getIdleTask();
            snapshot.task = idle.isEmpty() ? "" : "idle " + idle;
        }
        return snapshot;
    }

    public UUID getId() {
        return id;
    }

    public boolean isLoaded() {
        return entityId >= 0;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getGroup() {
        return group;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public String getTask() {
        return task;
    }

    public void write(ByteBuf buf) {
        buf.writeLong(id.getMostSignificantBits());
        buf.writeLong(id.getLeastSignificantBits());
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, group);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeByte(foodLevel);
        buf.writeShort(pendingCount);
        ByteBufUtils.writeUTF8String(buf, task);
    }

    public static CitizenSnapshot read(ByteBuf buf) {
        CitizenSnapshot snapshot = new CitizenSnapshot();
        snapshot.id = new UUID(buf.readLong(), buf.readLong());
        snapshot.entityId = buf.readInt();
        snapshot.group = ByteBufUtils.readUTF8String(buf);
        snapshot.x = buf.readDouble();
        snapshot.y = buf.readDouble();
        snapshot.z = buf.readDouble();
        snapshot.health = buf.readFloat();
        snapshot.maxHealth = buf.readFloat();
        snapshot.foodLevel = buf.readByte();
        snapshot.pendingCount = buf.readShort();
        snapshot.task = ByteBufUtils.readUTF8String(buf);
        return snapshot;
    }
}
