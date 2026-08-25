package com.enn3developer.gregcolonies.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTTagCompound;

import com.enn3developer.gregcolonies.GregColonies;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

public final class CitizenCommandRegistry {

    private static final Map<String, Supplier<CitizenCommand>> FACTORIES = new HashMap<>();

    private CitizenCommandRegistry() {}

    public static void register(String id, Supplier<CitizenCommand> factory) {
        if (FACTORIES.put(id, factory) != null) {
            throw new IllegalArgumentException("Duplicate citizen command id " + id);
        }
    }

    public static void registerDefaults() {
        register(CitizenCommandMoveTo.ID, CitizenCommandMoveTo::new);
    }

    public static CitizenCommand create(String id) {
        Supplier<CitizenCommand> factory = FACTORIES.get(id);
        return factory == null ? null : factory.get();
    }

    public static NBTTagCompound write(CitizenCommand command) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", command.getId());
        NBTTagCompound payload = new NBTTagCompound();
        command.writeToNBT(payload);
        tag.setTag("data", payload);
        return tag;
    }

    public static CitizenCommand read(NBTTagCompound tag) {
        String id = tag.getString("id");
        CitizenCommand command = create(id);
        if (command == null) {
            GregColonies.LOG.warn("Unknown citizen command id " + id + ", dropping it");
            return null;
        }
        command.readFromNBT(tag.getCompoundTag("data"));
        return command;
    }
}
