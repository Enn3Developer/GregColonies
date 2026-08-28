package com.enn3developer.gregcolonies.client.gui;

import net.minecraft.util.MathHelper;

import com.enn3developer.gregcolonies.colony.Blueprint;

public final class BlueprintTrace {

    private static final double EPSILON = 1.0E-6D;

    private BlueprintTrace() {}

    public static Hit trace(Blueprint model, int top, int plane, double ox, double oy, double oz, double dirX,
        double dirY, double dirZ) {
        double[] origin = { ox, oy, oz };
        double[] direction = { dirX, dirY, dirZ };
        double[] high = { model.getSizeX(), top + 1.0D, model.getSizeZ() };

        double near = 0.0D;
        double far = Double.MAX_VALUE;
        int entryAxis = -1;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) < EPSILON) {
                if (origin[axis] < 0.0D || origin[axis] > high[axis]) {
                    return onPlane(model, plane, ox, oy, oz, dirX, dirY, dirZ);
                }
                continue;
            }
            double first = -origin[axis] / direction[axis];
            double second = (high[axis] - origin[axis]) / direction[axis];
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            if (first > near) {
                near = first;
                entryAxis = axis;
            }
            far = Math.min(far, second);
        }
        if (near > far || far < 0.0D) {
            return onPlane(model, plane, ox, oy, oz, dirX, dirY, dirZ);
        }

        double startX = ox + dirX * (near + EPSILON);
        double startY = oy + dirY * (near + EPSILON);
        double startZ = oz + dirZ * (near + EPSILON);
        int voxelX = clamp(MathHelper.floor_double(startX), 0, model.getSizeX() - 1);
        int voxelY = clamp(MathHelper.floor_double(startY), 0, top);
        int voxelZ = clamp(MathHelper.floor_double(startZ), 0, model.getSizeZ() - 1);

        int stepX = dirX > 0.0D ? 1 : -1;
        int stepY = dirY > 0.0D ? 1 : -1;
        int stepZ = dirZ > 0.0D ? 1 : -1;
        double deltaX = Math.abs(dirX) < EPSILON ? Double.MAX_VALUE : Math.abs(1.0D / dirX);
        double deltaY = Math.abs(dirY) < EPSILON ? Double.MAX_VALUE : Math.abs(1.0D / dirY);
        double deltaZ = Math.abs(dirZ) < EPSILON ? Double.MAX_VALUE : Math.abs(1.0D / dirZ);
        double nextX = boundary(startX, voxelX, stepX, deltaX);
        double nextY = boundary(startY, voxelY, stepY, deltaY);
        double nextZ = boundary(startZ, voxelZ, stepZ, deltaZ);

        int faceX = entryAxis == 0 ? -stepX : 0;
        int faceY = entryAxis == 1 ? -stepY : 0;
        int faceZ = entryAxis == 2 ? -stepZ : 0;
        int steps = model.getSizeX() + model.getSizeY() + model.getSizeZ() + 3;
        for (int i = 0; i < steps; i++) {
            if (voxelX < 0 || voxelY < 0
                || voxelZ < 0
                || voxelX >= model.getSizeX()
                || voxelY > top
                || voxelZ >= model.getSizeZ()) {
                break;
            }
            if (model.cellAt(voxelX, voxelY, voxelZ) != Blueprint.AIR) {
                return new Hit(true, voxelX, voxelY, voxelZ, voxelX + faceX, voxelY + faceY, voxelZ + faceZ);
            }
            if (nextX <= nextY && nextX <= nextZ) {
                voxelX += stepX;
                nextX += deltaX;
                faceX = -stepX;
                faceY = 0;
                faceZ = 0;
            } else if (nextY <= nextZ) {
                voxelY += stepY;
                nextY += deltaY;
                faceX = 0;
                faceY = -stepY;
                faceZ = 0;
            } else {
                voxelZ += stepZ;
                nextZ += deltaZ;
                faceX = 0;
                faceY = 0;
                faceZ = -stepZ;
            }
        }
        return onPlane(model, plane, ox, oy, oz, dirX, dirY, dirZ);
    }

    private static Hit onPlane(Blueprint model, int plane, double ox, double oy, double oz, double dirX, double dirY,
        double dirZ) {
        if (plane < 0 || Math.abs(dirY) < EPSILON) {
            return null;
        }
        double distance = (plane - oy) / dirY;
        if (distance < 0.0D) {
            return null;
        }
        int x = MathHelper.floor_double(ox + dirX * distance);
        int z = MathHelper.floor_double(oz + dirZ * distance);
        if (!model.contains(x, plane, z)) {
            return null;
        }
        return new Hit(false, x, plane, z, x, plane, z);
    }

    private static double boundary(double start, int voxel, int step, double delta) {
        if (delta == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }
        double fraction = step > 0 ? voxel + 1 - start : start - voxel;
        return fraction * delta;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static class Hit {

        public final boolean solid;

        public final int hitX;

        public final int hitY;

        public final int hitZ;

        public final int placeX;

        public final int placeY;

        public final int placeZ;

        Hit(boolean solid, int hitX, int hitY, int hitZ, int placeX, int placeY, int placeZ) {
            this.solid = solid;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
            this.placeX = placeX;
            this.placeY = placeY;
            this.placeZ = placeZ;
        }

        int[] hit() {
            return solid ? new int[] { hitX, hitY, hitZ } : null;
        }

        int[] place() {
            return new int[] { placeX, placeY, placeZ };
        }
    }
}
