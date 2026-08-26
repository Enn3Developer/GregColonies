package com.enn3developer.gregcolonies.entity.ai.auto;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public abstract class AutoTask {

    public abstract String getId();

    public abstract boolean shouldStart(EntityCitizen citizen, Colony colony);

    public void start(EntityCitizen citizen, Colony colony) {}

    public abstract boolean update(EntityCitizen citizen, Colony colony);

    public void finish(EntityCitizen citizen) {}

    public String describe() {
        return getId();
    }

    protected static boolean pathTowards(EntityCitizen citizen, double x, double y, double z, double speed) {
        return citizen.travelTo(x, y, z, speed);
    }
}
