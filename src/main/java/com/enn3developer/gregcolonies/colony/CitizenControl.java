package com.enn3developer.gregcolonies.colony;

public interface CitizenControl {

    int stopWork(String group);

    double distanceSq(ColonyCitizen entry, int dimension, double x, double z);

    void assign(ColonyCitizen entry, String group);
}
