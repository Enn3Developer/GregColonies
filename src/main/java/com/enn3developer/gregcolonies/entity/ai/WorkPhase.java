package com.enn3developer.gregcolonies.entity.ai;

public enum WorkPhase {

    TRAVEL,
    WORK,
    FINISH;

    public static WorkPhase byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : TRAVEL;
    }

    public int id() {
        return ordinal();
    }
}
