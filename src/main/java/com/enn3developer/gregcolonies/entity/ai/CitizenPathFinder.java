package com.enn3developer.gregcolonies.entity.ai;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.enn3developer.gregcolonies.entity.ai.work.WorkBlocks;

public class CitizenPathFinder {

    private static final int MAX_NODES = 3000;

    private static final float HEURISTIC_WEIGHT = 1.25F;

    private static final int CLEAR = 1;

    private static final int OPEN_WATER = 2;

    private static final int AVOIDED_WATER = -1;

    private static final int DEADLY = -2;

    private static final int FENCE = -3;

    private static final int TRAPDOOR = -4;

    private static final int MAX_FALL = 3;

    private static final int SWIM_SCAN = 16;

    private static final int HEAP_CAPACITY = 512;

    private final Entity entity;

    private final boolean enterDoors;

    private final boolean breakDoors;

    private final boolean canSwim;

    private final boolean keepAway;

    private final PathPoint size;

    private final Map<Long, Node> nodes = new HashMap<>();

    private final Node[] options = new Node[4];

    private Node[] heap = new Node[HEAP_CAPACITY];

    private int heapSize;

    private boolean avoidsWater;

    public CitizenPathFinder(Entity entity, boolean enterDoors, boolean breakDoors, boolean avoidsWater,
        boolean canSwim, boolean keepAway) {
        this.entity = entity;
        this.enterDoors = enterDoors;
        this.breakDoors = breakDoors;
        this.avoidsWater = avoidsWater;
        this.canSwim = canSwim;
        this.keepAway = keepAway;
        this.size = new PathPoint(
            MathHelper.floor_float(entity.width + 1.0F),
            MathHelper.floor_float(entity.height + 1.0F),
            MathHelper.floor_float(entity.width + 1.0F));
    }

    public PathEntity createPath(double x, double y, double z, float range) {
        boolean avoided = avoidsWater;
        Node start = node(
            MathHelper.floor_double(entity.boundingBox.minX),
            startY(),
            MathHelper.floor_double(entity.boundingBox.minZ));
        Node target = node(
            MathHelper.floor_double(x - entity.width / 2.0F),
            MathHelper.floor_double(y),
            MathHelper.floor_double(z - entity.width / 2.0F));
        PathEntity path = search(start, target, range);
        avoidsWater = avoided;
        return path;
    }

    private int startY() {
        double bottom = entity.boundingBox.minY;
        if (!canSwim || !entity.isInWater()) {
            return MathHelper.floor_double(bottom + 0.5D);
        }

        World world = entity.worldObj;
        int x = MathHelper.floor_double(entity.posX);
        int z = MathHelper.floor_double(entity.posZ);
        int y = (int) bottom;
        for (int scan = 0; scan < SWIM_SCAN; scan++) {
            Block block = world.getBlock(x, y, z);
            if (block != Blocks.flowing_water && block != Blocks.water) {
                break;
            }
            y++;
        }
        avoidsWater = false;
        return y;
    }

    private PathEntity search(Node start, Node target, float range) {
        start.cost = 0.0F;
        start.estimate = start.distanceTo(target);
        start.total = start.estimate;
        push(start);

        Node best = start;
        float bestEstimate = start.estimate;
        int expanded = 0;
        while (heapSize > 0 && expanded < MAX_NODES) {
            Node current = pop();
            if (current == target) {
                return build(current);
            }
            expanded++;
            current.closed = true;

            float estimate = current.distanceTo(target);
            if (estimate < bestEstimate) {
                best = current;
                bestEstimate = estimate;
            }

            int found = neighbours(current, start, range);
            for (int i = 0; i < found; i++) {
                Node next = options[i];
                float cost = current.cost + current.distanceTo(next);
                if (next.queued() && cost >= next.cost) {
                    continue;
                }
                next.previous = current;
                next.cost = cost;
                next.estimate = next.distanceTo(target);
                float total = cost + next.estimate * HEURISTIC_WEIGHT;
                if (next.queued()) {
                    requeue(next, total);
                } else {
                    next.total = total;
                    push(next);
                }
            }
        }
        return best == start ? null : build(best);
    }

    private int neighbours(Node from, Node start, float range) {
        int jump = offset(from.x, from.y + 1, from.z) == CLEAR ? 1 : 0;
        int found = 0;
        found = collect(found, safePoint(from.x, from.y, from.z + 1, jump), start, range);
        found = collect(found, safePoint(from.x - 1, from.y, from.z, jump), start, range);
        found = collect(found, safePoint(from.x + 1, from.y, from.z, jump), start, range);
        found = collect(found, safePoint(from.x, from.y, from.z - 1, jump), start, range);
        return found;
    }

    private int collect(int found, Node node, Node start, float range) {
        if (node == null || node.closed || node.distanceTo(start) >= range) {
            return found;
        }
        options[found] = node;
        return found + 1;
    }

    private Node safePoint(int x, int y, int z, int jump) {
        int here = offset(x, y, z);
        if (here == OPEN_WATER) {
            return node(x, y, z);
        }

        Node found = here == CLEAR ? node(x, y, z) : null;
        if (found == null && jump > 0 && here != FENCE && here != TRAPDOOR && offset(x, y + jump, z) == CLEAR) {
            found = node(x, y + jump, z);
            y += jump;
        }
        if (found == null) {
            return null;
        }

        int drops = 0;
        int below = 0;
        while (y > 0) {
            below = offset(x, y - 1, z);
            if (avoidsWater && below == AVOIDED_WATER) {
                return null;
            }
            if (below != CLEAR) {
                break;
            }
            if (drops++ >= Math.min(entity.getMaxSafePointTries(), MAX_FALL)) {
                return null;
            }
            y--;
            if (y > 0) {
                found = node(x, y, z);
            }
        }
        return below == DEADLY ? null : found;
    }

    private int offset(int x, int y, int z) {
        World world = entity.worldObj;
        for (int bx = x; bx < x + size.xCoord; bx++) {
            for (int by = y; by < y + size.yCoord; by++) {
                for (int bz = z; bz < z + size.zCoord; bz++) {
                    if (Hazards.isDeadly(world, bx, by, bz)) {
                        return DEADLY;
                    }
                }
            }
        }
        if (keepAway && Hazards.isBesideDeadly(world, x, y, z)) {
            return DEADLY;
        }
        return PathFinder.func_82565_a(entity, x, y, z, size, avoidsWater, breakDoors, enterDoors);
    }

    private Node node(int x, int y, int z) {
        long key = WorkBlocks.pack(x, y, z);
        Node node = nodes.get(key);
        if (node == null) {
            node = new Node(x, y, z);
            nodes.put(key, node);
        }
        return node;
    }

    private PathEntity build(Node end) {
        int length = 1;
        for (Node node = end; node.previous != null; node = node.previous) {
            length++;
        }

        PathPoint[] points = new PathPoint[length];
        Node node = end;
        for (int i = length - 1; i >= 0; i--) {
            points[i] = new PathPoint(node.x, node.y, node.z);
            node = node.previous;
        }
        return new PathEntity(points);
    }

    private void push(Node node) {
        if (heapSize == heap.length) {
            Node[] grown = new Node[heapSize << 1];
            System.arraycopy(heap, 0, grown, 0, heapSize);
            heap = grown;
        }
        heap[heapSize] = node;
        node.index = heapSize;
        sortUp(heapSize++);
    }

    private Node pop() {
        Node top = heap[0];
        heap[0] = heap[--heapSize];
        heap[heapSize] = null;
        if (heapSize > 0) {
            sortDown(0);
        }
        top.index = -1;
        return top;
    }

    private void requeue(Node node, float total) {
        float previous = node.total;
        node.total = total;
        if (total < previous) {
            sortUp(node.index);
        } else {
            sortDown(node.index);
        }
    }

    private void sortUp(int index) {
        Node node = heap[index];
        float total = node.total;
        while (index > 0) {
            int parent = index - 1 >> 1;
            Node above = heap[parent];
            if (total >= above.total) {
                break;
            }
            heap[index] = above;
            above.index = index;
            index = parent;
        }
        heap[index] = node;
        node.index = index;
    }

    private void sortDown(int index) {
        Node node = heap[index];
        float total = node.total;
        while (true) {
            int left = 1 + (index << 1);
            int right = left + 1;
            if (left >= heapSize) {
                break;
            }
            Node lower = heap[left];
            int child = left;
            if (right < heapSize && heap[right].total < lower.total) {
                lower = heap[right];
                child = right;
            }
            if (lower.total >= total) {
                break;
            }
            heap[index] = lower;
            lower.index = index;
            index = child;
        }
        heap[index] = node;
        node.index = index;
    }

    private static final class Node {

        private final int x;

        private final int y;

        private final int z;

        private int index = -1;

        private float cost;

        private float estimate;

        private float total;

        private boolean closed;

        private Node previous;

        private Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private boolean queued() {
            return index >= 0;
        }

        private float distanceTo(Node other) {
            float dx = other.x - x;
            float dy = other.y - y;
            float dz = other.z - z;
            return MathHelper.sqrt_float(dx * dx + dy * dy + dz * dz);
        }
    }
}
