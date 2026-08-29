package com.enn3developer.gregcolonies.colony;

import java.util.function.ToIntFunction;

import com.enn3developer.gregcolonies.entity.ai.CitizenCommand;

public final class ColonyActions {

    private ColonyActions() {}

    public static Outcome clearBuildSite(ColonyRegistry registry, Colony colony) {
        registry.setBuildSite(colony.getId(), null);
        return Outcome.ok("Build site cleared");
    }

    public static Outcome setBuildSite(ColonyRegistry registry, Colony colony, int dimension, int x, int y, int z,
        ToIntFunction<BuildSite> survey) {
        if (colony.getDimension() != dimension) {
            return Outcome.fail("The build site must be in the colony dimension");
        }
        Blueprint blueprint = colony.getActiveBlueprint();
        if (blueprint == null) {
            return Outcome.fail("Capture a blueprint before starting a build");
        }
        if (!colony.site(ColonySiteKind.MATERIALS)
            .isPresent()) {
            return Outcome.fail("Set a materials chest before starting a build");
        }

        int top = y + 1;
        BuildSite site = new BuildSite(x, top, z, blueprint, colony.getPlaceRotation(), colony.isPlaceMirror());
        registry.setBuildSite(colony.getId(), site);
        return Outcome
            .ok("Build site set at " + x + "/" + top + "/" + z + ", " + survey.applyAsInt(site) + " blocks to place");
    }

    public static Outcome clearSite(ColonyRegistry registry, Colony colony, ColonySiteKind kind) {
        registry.clearSite(colony.getId(), kind);
        return Outcome.ok("Colony " + kind.getLabel() + " cleared");
    }

    public static Outcome setSite(ColonyRegistry registry, Colony colony, ColonySiteKind kind, int dimension,
        boolean hasInventory, int x, int y, int z) {
        if (colony.getDimension() != dimension) {
            return Outcome.fail("The " + kind.getLabel() + " must be in the colony dimension");
        }
        if (!hasInventory) {
            return Outcome.fail("That block has no inventory");
        }
        registry.setSite(colony.getId(), kind, x, y, z);
        return Outcome.ok("Colony " + kind.getLabel() + " set to " + x + "/" + y + "/" + z);
    }

    public static Outcome clearHome(ColonyRegistry registry, Colony colony, int homeId) {
        if (!registry.removeHome(colony.getId(), homeId)) {
            return Outcome.silent();
        }
        return Outcome.ok("Home #" + homeId + " cleared");
    }

    public static Outcome setHome(ColonyRegistry registry, Colony colony, int dimension, WorkArea area, int beds) {
        if (colony.getDimension() != dimension) {
            return Outcome.fail("A home must be in the colony dimension");
        }
        if (!colony.isInside(dimension, area.getCenterX() + 0.5D, area.getCenterZ() + 0.5D)) {
            return Outcome.fail("A home must be inside the colony");
        }
        if (beds <= 0) {
            return Outcome.fail("There is no bed in that region");
        }
        if (colony.overlapsHome(area)) {
            return Outcome.fail("That region overlaps another home");
        }
        if (colony.getHomes()
            .size() >= Colony.MAX_HOMES) {
            return Outcome.fail("The colony already has " + Colony.MAX_HOMES + " homes, clear one first");
        }

        ColonyHome home = registry.addHome(colony.getId(), area, beds);
        if (home == null) {
            return Outcome.silent();
        }
        return Outcome.ok("Home #" + home.getId() + " set at " + home.describe() + ", " + beds + " bed(s)");
    }

    public static Outcome storeBlueprint(ColonyRegistry registry, Colony colony, Blueprint decoded, int index) {
        Blueprint blueprint = decoded == null ? null : decoded.trimmed();
        if (blueprint == null || !blueprint.isPlaceable()) {
            return Outcome.fail("That design holds no buildable blocks, or uses blocks this server does not have");
        }
        int slot = index;
        if (slot >= 0) {
            if (!registry.replaceBlueprint(colony.getId(), slot, blueprint)) {
                return Outcome.silent();
            }
        } else {
            if (colony.getBlueprints()
                .size() >= Colony.MAX_BLUEPRINTS) {
                return Outcome.fail("The blueprint library is full (" + Colony.MAX_BLUEPRINTS + "), delete one first");
            }
            slot = registry.addBlueprint(colony.getId(), blueprint);
            if (slot < 0) {
                return Outcome.silent();
            }
        }
        return Outcome.ok(
            "Blueprint saved: " + blueprint.getSizeX()
                + "x"
                + blueprint.getSizeY()
                + "x"
                + blueprint.getSizeZ()
                + ", "
                + blueprint.blockCount()
                + " blocks",
            slot);
    }

    public static Outcome enqueueOrder(ColonyRegistry registry, Colony colony, CitizenCommand command, String group) {
        String target = group == null ? "" : group;
        command.setTargetGroup(target);
        registry.enqueueOrder(colony.getId(), command);
        String where = target.isEmpty() ? "" : "group " + target + " of ";
        int pending = target.isEmpty() ? colony.getOrderCount() : colony.getOrderCount(target);
        return Outcome.ok(
            "Queued " + command
                .getId() + " for " + where + "colony #" + colony.getId() + " (" + pending + " order(s) pending)");
    }

    public static Outcome cancelOrders(ColonyRegistry registry, Colony colony, String group, CitizenControl control) {
        String target = group == null ? "" : group;
        int cleared = target.isEmpty() ? registry.clearOrders(colony.getId())
            : registry.clearOrders(colony.getId(), target);
        int stopped = control.stopWork(target);
        String what = target.isEmpty() ? "Dropped " + cleared + " pending order(s), "
            : "Dropped " + cleared + " order(s) for group " + target + ", ";
        return Outcome.ok(what + "stopped " + stopped + " citizen(s)");
    }

    public static Outcome assignGroup(ColonyRegistry registry, Colony colony, String group, int dimension, double x,
        double z, int radius, CitizenControl control) {
        String target = group == null ? "" : group;
        int changed = 0;
        for (ColonyCitizen entry : colony.getCitizens()) {
            if (control.distanceSq(entry, dimension, x, z) > (double) radius * radius) {
                continue;
            }
            control.assign(entry, target);
            changed++;
        }
        registry.markDirty();
        return Outcome.ok(changed + " citizen(s) " + (target.isEmpty() ? "ungrouped" : "put into group " + target));
    }
}
