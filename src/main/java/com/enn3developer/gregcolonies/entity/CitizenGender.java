package com.enn3developer.gregcolonies.entity;

import java.util.Random;

public enum CitizenGender {

    MALE("male"),
    FEMALE("female");

    private final String label;

    CitizenGender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public byte getId() {
        return (byte) (ordinal() + 1);
    }

    public static CitizenGender byId(int id) {
        for (CitizenGender gender : values()) {
            if (gender.getId() == id) {
                return gender;
            }
        }
        return null;
    }

    public static byte idOf(CitizenGender gender) {
        return gender == null ? 0 : gender.getId();
    }

    public static String labelOf(CitizenGender gender) {
        return gender == null ? "" : gender.getLabel();
    }

    public static CitizenGender random(Random random) {
        return random.nextBoolean() ? MALE : FEMALE;
    }
}
