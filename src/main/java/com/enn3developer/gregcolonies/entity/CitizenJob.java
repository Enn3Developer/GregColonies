package com.enn3developer.gregcolonies.entity;

public enum CitizenJob {

    NONE("no job"),
    BUILDER("builder");

    private final String label;

    CitizenJob(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public byte getId() {
        return (byte) ordinal();
    }

    public static CitizenJob byId(int id) {
        for (CitizenJob job : values()) {
            if (job.getId() == id) {
                return job;
            }
        }
        return NONE;
    }

    public static byte idOf(CitizenJob job) {
        return job == null ? NONE.getId() : job.getId();
    }

    public static CitizenJob byName(String name) {
        for (CitizenJob job : values()) {
            if (job.name()
                .equals(name)) {
                return job;
            }
        }
        return NONE;
    }
}
