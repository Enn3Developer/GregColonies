package com.enn3developer.gregcolonies.colony;

import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;

public class ColonySite {

    private boolean present;

    private int x;

    private int y;

    private int z;

    public boolean isPresent() {
        return present;
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

    public boolean isAt(int x, int y, int z) {
        return present && this.x == x && this.y == y && this.z == z;
    }

    public void set(int x, int y, int z) {
        this.present = true;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void clear() {
        this.present = false;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public void copyFrom(ColonySite other) {
        this.present = other.present;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public String describe() {
        return x + "/" + y + "/" + z;
    }

    public void writeToNBT(NBTTagCompound tag, ColonySiteKind kind) {
        tag.setBoolean(kind.getFlagKey(), present);
        if (present) {
            tag.setInteger(kind.getAxisKey('X'), x);
            tag.setInteger(kind.getAxisKey('Y'), y);
            tag.setInteger(kind.getAxisKey('Z'), z);
        }
    }

    public void readFromNBT(NBTTagCompound tag, ColonySiteKind kind) {
        present = tag.getBoolean(kind.getFlagKey());
        if (present) {
            x = tag.getInteger(kind.getAxisKey('X'));
            y = tag.getInteger(kind.getAxisKey('Y'));
            z = tag.getInteger(kind.getAxisKey('Z'));
        } else {
            x = 0;
            y = 0;
            z = 0;
        }
    }

    public void write(ByteBuf buf) {
        buf.writeBoolean(present);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public void read(ByteBuf buf) {
        present = buf.readBoolean();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }
}
