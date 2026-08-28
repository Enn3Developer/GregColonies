package com.enn3developer.gregcolonies.entity.ai;

import com.enn3developer.gregcolonies.entity.EntityCitizen;
import com.enn3developer.gregcolonies.entity.ai.command.CitizenCommandMoveTo;

public class TravelLeg {

    public enum Step {
        RUNNING,
        ARRIVED,
        FAILED
    }

    private CitizenCommandMoveTo leg;

    public boolean isActive() {
        return leg != null;
    }

    public Step walk(EntityCitizen citizen, int x, int y, int z) {
        return walk(citizen, x, y, z, true);
    }

    public Step walk(EntityCitizen citizen, int x, int y, int z, boolean mayStart) {
        if (leg == null) {
            if (!mayStart) {
                return Step.RUNNING;
            }
            leg = new CitizenCommandMoveTo(x, y, z);
            leg.start(citizen);
        }
        CitizenCommandResult result = leg.update(citizen);
        if (result == CitizenCommandResult.RUNNING) {
            return Step.RUNNING;
        }
        clear(citizen);
        return result == CitizenCommandResult.DONE ? Step.ARRIVED : Step.FAILED;
    }

    public void clear(EntityCitizen citizen) {
        if (leg != null) {
            leg.finish(citizen);
            leg = null;
        }
    }
}
