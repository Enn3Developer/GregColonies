package com.enn3developer.gregcolonies.entity.ai;

public class ApproachTracker {

    private static final double EPSILON = 0.5D;

    private final int timeout;

    private int ticks;

    private double bestSq = Double.MAX_VALUE;

    public ApproachTracker(int timeout) {
        this.timeout = timeout;
    }

    public void reset() {
        ticks = 0;
        bestSq = Double.MAX_VALUE;
    }

    public void restart() {
        ticks = 0;
    }

    public boolean stalled(double distanceSq) {
        if (distanceSq < bestSq - EPSILON) {
            bestSq = distanceSq;
            ticks = 0;
            return false;
        }
        return ++ticks > timeout;
    }
}
