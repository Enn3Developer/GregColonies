package com.enn3developer.gregcolonies.entity.ai;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApproachTrackerTest {

    @Test
    void gettingCloserNeverStalls() {
        ApproachTracker tracker = new ApproachTracker(3);
        for (double distance = 100.0D; distance > 0.0D; distance -= 1.0D) {
            assertFalse(tracker.stalled(distance));
        }
    }

    @Test
    void standingStillStallsAfterTheTimeout() {
        ApproachTracker tracker = new ApproachTracker(3);
        assertFalse(tracker.stalled(10.0D));
        assertFalse(tracker.stalled(10.0D));
        assertFalse(tracker.stalled(10.0D));
        assertFalse(tracker.stalled(10.0D));
        assertTrue(tracker.stalled(10.0D));
    }

    @Test
    void tinyProgressDoesNotCountAsProgress() {
        ApproachTracker tracker = new ApproachTracker(2);
        assertFalse(tracker.stalled(10.0D));
        assertFalse(tracker.stalled(9.8D));
        assertFalse(tracker.stalled(9.7D));
        assertTrue(tracker.stalled(9.6D));
    }

    @Test
    void realProgressResetsTheClock() {
        ApproachTracker tracker = new ApproachTracker(2);
        tracker.stalled(10.0D);
        tracker.stalled(10.0D);
        assertFalse(tracker.stalled(5.0D));
        assertFalse(tracker.stalled(5.0D));
        assertFalse(tracker.stalled(5.0D));
        assertTrue(tracker.stalled(5.0D));
    }

    @Test
    void resetForgetsTheBestDistance() {
        ApproachTracker tracker = new ApproachTracker(1);
        tracker.stalled(1.0D);
        tracker.reset();
        assertFalse(tracker.stalled(50.0D));
        assertFalse(tracker.stalled(50.0D));
        assertTrue(tracker.stalled(50.0D));
    }

    @Test
    void restartClearsTheTicksButKeepsTheBest() {
        ApproachTracker tracker = new ApproachTracker(1);
        tracker.stalled(5.0D);
        tracker.stalled(5.0D);
        tracker.restart();
        assertFalse(tracker.stalled(5.0D));
        assertTrue(tracker.stalled(5.0D));
    }
}
