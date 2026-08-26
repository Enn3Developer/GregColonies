package com.enn3developer.gregcolonies.entity.ai.auto;

import com.enn3developer.gregcolonies.colony.Colony;
import com.enn3developer.gregcolonies.entity.EntityCitizen;

public abstract class AutoTask {

    private static final double HOP_DISTANCE = 12.0D;

    public abstract String getId();

    public abstract boolean shouldStart(EntityCitizen citizen, Colony colony);

    public void start(EntityCitizen citizen, Colony colony) {}

    public abstract boolean update(EntityCitizen citizen, Colony colony);

    public void finish(EntityCitizen citizen) {}

    public String describe() {
        return getId();
    }

    protected static boolean pathTowards(EntityCitizen citizen, double x, double y, double z, double speed) {
        double dx = x - citizen.posX;
        double dy = y - citizen.posY;
        double dz = z - citizen.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= HOP_DISTANCE) {
            return citizen.getNavigator()
                .tryMoveToXYZ(x, y, z, speed);
        }

        double scale = HOP_DISTANCE / distance;
        return citizen.getNavigator()
            .tryMoveToXYZ(citizen.posX + dx * scale, citizen.posY + dy * scale, citizen.posZ + dz * scale, speed);
    }
}
