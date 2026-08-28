package com.enn3developer.gregcolonies.colony;

import java.util.HashSet;
import java.util.Set;

public class BuildPlan {

    public enum Phase {
        CLEAR,
        BUILD,
        STRIP
    }

    public interface Probe {

        boolean needsClearing(int x, int y, int z);

        boolean canPlace(int x, int y, int z);

        boolean isFullCube(int cell);
    }

    private final Set<Long> blocked = new HashSet<>();
    private final Set<Integer> missing = new HashSet<>();
    private Phase phase = Phase.CLEAR;
    private int cursor;
    private boolean progressed;
    private boolean detailPass;
    private boolean hasTarget;
    private int targetX;
    private int targetY;
    private int targetZ;

    public Phase getPhase() {
        return phase;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public int getTargetZ() {
        return targetZ;
    }

    public void start(BuildSite site) {
        blocked.clear();
        startPhase(Phase.CLEAR, site);
    }

    private void startPhase(Phase next, BuildSite site) {
        phase = next;
        cursor = next == Phase.CLEAR && site != null ? site.getBlueprint()
            .volume() - 1 : 0;
        progressed = false;
        detailPass = false;
        hasTarget = false;
        missing.clear();
    }

    public void markProgress() {
        progressed = true;
    }

    public void materialMissing(int cell) {
        missing.add(cell);
    }

    public void clearTarget() {
        hasTarget = false;
    }

    public void skipCell() {
        hasTarget = false;
        cursor++;
    }

    public void blockTarget() {
        if (hasTarget) {
            blocked.add(BlockKey.pack(targetX, targetY, targetZ));
        }
        hasTarget = false;
        if (phase == Phase.BUILD) {
            cursor++;
        }
    }

    public boolean nextTarget(Probe probe, Colony colony, BuildSite site) {
        if (phase == Phase.CLEAR) {
            return nextClearTarget(probe, colony, site);
        }
        return phase == Phase.BUILD && nextBuildTarget(probe, site);
    }

    private boolean nextClearTarget(Probe probe, Colony colony, BuildSite site) {
        while (cursor >= 0) {
            take(site, cursor);
            if (!blocked.contains(BlockKey.pack(targetX, targetY, targetZ))
                && !isProtected(colony, targetX, targetY, targetZ)
                && probe.needsClearing(targetX, targetY, targetZ)) {
                hasTarget = true;
                return true;
            }
            cursor--;
        }
        startPhase(Phase.BUILD, site);
        return false;
    }

    private boolean nextBuildTarget(Probe probe, BuildSite site) {
        int volume = site.getBlueprint()
            .volume();
        while (cursor < volume) {
            take(site, cursor);
            int cell = site.cellFor(targetX, targetY, targetZ);
            if (cell != Blueprint.AIR && (detailPass || probe.isFullCube(cell))
                && !missing.contains(cell)
                && !blocked.contains(BlockKey.pack(targetX, targetY, targetZ))
                && probe.canPlace(targetX, targetY, targetZ)) {
                hasTarget = true;
                return true;
            }
            cursor++;
        }
        return rollPass(site);
    }

    private boolean rollPass(BuildSite site) {
        if (!detailPass) {
            detailPass = true;
            restartPass();
            return false;
        }
        if (progressed) {
            progressed = false;
            detailPass = false;
            restartPass();
            return false;
        }
        startPhase(Phase.STRIP, site);
        return false;
    }

    private void restartPass() {
        cursor = 0;
        missing.clear();
        blocked.clear();
    }

    private void take(BuildSite site, int index) {
        Blueprint blueprint = site.getBlueprint();
        int sizeX = blueprint.getSizeX();
        int sizeZ = blueprint.getSizeZ();
        targetX = site.getX() + index % sizeX;
        targetY = site.getY() + index / (sizeX * sizeZ);
        targetZ = site.getZ() + index / sizeX % sizeZ;
    }

    private static boolean isProtected(Colony colony, int x, int y, int z) {
        if (colony.getX() == x && colony.getY() == y && colony.getZ() == z) {
            return true;
        }
        for (ColonySiteKind kind : ColonySiteKind.values()) {
            if (colony.site(kind)
                .isAt(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
