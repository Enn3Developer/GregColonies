package com.enn3developer.gregcolonies.entity;

import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;

import com.enn3developer.gregcolonies.colony.CitizenControl;
import com.enn3developer.gregcolonies.colony.ColonyCitizen;

public class LiveCitizens implements CitizenControl {

    private final Map<UUID, EntityCitizen> loaded;
    private final Entity actor;

    public LiveCitizens(Map<UUID, EntityCitizen> loaded, Entity actor) {
        this.loaded = loaded;
        this.actor = actor;
    }

    @Override
    public int stopWork(String group) {
        int stopped = 0;
        for (EntityCitizen citizen : loaded.values()) {
            if (!group.isEmpty() && !group.equals(citizen.getGroup())) {
                continue;
            }
            citizen.getCommands()
                .clear(citizen);
            stopped++;
        }
        return stopped;
    }

    @Override
    public double distanceSq(ColonyCitizen entry, int dimension, double x, double z) {
        EntityCitizen citizen = loaded.get(entry.getId());
        if (citizen == null || actor == null) {
            return entry.distanceSqTo(dimension, x, z);
        }
        return citizen.getDistanceSqToEntity(actor);
    }

    @Override
    public void assign(ColonyCitizen entry, String group) {
        EntityCitizen citizen = loaded.get(entry.getId());
        if (citizen == null) {
            entry.setGroup(group);
        } else {
            citizen.setGroup(group);
        }
    }
}
